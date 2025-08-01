package org.nkjmlab.go.javalin.model.relation;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.common.container.RowMap;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTableBase;
import org.nkjmlab.sorm4j.sql.statement.SqlKeyword;
import org.nkjmlab.sorm4j.sql.statement.SqlTrait;
import org.nkjmlab.sorm4j.table.definition.annotation.Index;
import org.nkjmlab.sorm4j.table.definition.annotation.PrimaryKey;

public class MatchingRequestsTable extends H2DefinedTableBase<MatchingRequest>
    implements SqlTrait, SqlKeyword {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private static final String USER_ID = "user_id";
  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";

  private final GameStatesTables gameStatesTables;

  public MatchingRequestsTable(DataSource dataSource, GameStatesTables gameStatesTables) {
    super(Sorm.create(dataSource), MatchingRequest.class);
    this.gameStatesTables = gameStatesTables;
  }

  public List<MatchingRequest> readRequests() {
    List<MatchingRequest> result = selectAll().stream().collect(Collectors.toList());
    return result;
  }

  private List<String> readUserIdsOfUnpairedRequestOrdereByCreatedAt() {
    return getOrm()
        .readList(
            String.class,
            select(USER_ID)
                + from(getTableName())
                + where(cond(GAME_ID, "=", literal(MatchingRequest.UNPAIRED)))
                + orderByAsc(CREATED_AT));
  }

  private List<MatchingRequest> readUnpairedRequestsOrderByCreatedAt() {
    return readList(
        selectStarFrom(getTableName())
            + where(cond(GAME_ID, "=", literal(MatchingRequest.UNPAIRED)))
            + orderByAsc(CREATED_AT));
  }

  /**
   * マッチングをする u1:30級，u2:27級,u3:26級の順でwaitingRoomに入って来たとする．u2とu3がペアになってu1が待って欲しいが，そうならない．
   * なぜならば，最初にu1がtargetとなって検索が始まる.u1と級位差が小さいu2がペアになってu3が待たされるということ．
   *
   * @return マッチしたユーザ名
   */
  public Set<String> createPairOfUsers() {

    List<String> reqs = readUserIdsOfUnpairedRequestOrdereByCreatedAt();

    log.trace(
        "[{}] unpaired matching requests in [{}] matching requests",
        reqs.size(),
        getOrm().readFirst(Integer.class, SELECT + COUNT + "(*)" + FROM + getTableName()));

    Set<String> matchedUsers = new HashSet<>();
    for (String uid : reqs) {
      MatchingRequest target = selectByPrimaryKey(uid);

      if (!target.isUnpaired()) {
        continue;
      }

      Set<String> pastOpponents = gameStatesTables.readPastOpponents(target.userId());
      log.trace("[{}] has been matched with {} on today", target.userId(), pastOpponents);

      MatchingRequest nextOpponent = selectNextOponent(target, pastOpponents);
      if (nextOpponent == null) {
        continue;
      }

      String black = target.rank() >= nextOpponent.rank() ? target.userId() : nextOpponent.userId();
      String white = target.rank() >= nextOpponent.rank() ? nextOpponent.userId() : target.userId();
      String gameId = black + GameStatesTables.VS_SEPARATOR + white;

      log.trace("[{}] is created", gameId);

      updateByPrimaryKey(RowMap.of("game_id", gameId), target.userId);
      updateByPrimaryKey(RowMap.of("game_id", gameId), nextOpponent.userId);

      matchedUsers.add(target.userId());
      matchedUsers.add(nextOpponent.userId());
    }
    return matchedUsers;
  }

  private MatchingRequest selectNextOponent(MatchingRequest target, Set<String> pastOpponents) {

    List<MatchingRequest> unpairedRequestWithoutPastOpponennts =
        readUnpairedRequestsOrderByCreatedAt().stream()
            .filter(r -> !target.userId().equals(r.userId()) && !pastOpponents.contains(r.userId()))
            .collect(Collectors.toList());

    if (unpairedRequestWithoutPastOpponennts.size() == 0) {
      return null;
    }

    MatchingRequest nextOpponent = unpairedRequestWithoutPastOpponennts.get(0);

    for (MatchingRequest r : unpairedRequestWithoutPastOpponennts) {
      if (Integer.compare(
              Math.abs(r.rank() - target.rank()), Math.abs(nextOpponent.rank() - target.rank()))
          < 0) {
        nextOpponent = r;
      }
    }
    return nextOpponent;
  }

  public static record MatchingRequest(
      @PrimaryKey String userId,
      String seatId,
      String userName,
      int rank,
      @Index String gameId,
      LocalDateTime createdAt) {

    static final String UNPAIRED = "UNPAIRED";

    public MatchingRequest() {
      this("", "", "", 30, UNPAIRED, LocalDateTime.now());
    }

    public static MatchingRequest createUnpaired(User u) {
      return new MatchingRequest(
          u.userId(), u.seatId(), u.userName(), u.rank(), UNPAIRED, LocalDateTime.now());
    }

    // Thymeleaf templateからよばれるのでpublicのままにする必要がある．
    public boolean isUnpaired() {
      return UNPAIRED.equals(gameId);
    }
  }
}
