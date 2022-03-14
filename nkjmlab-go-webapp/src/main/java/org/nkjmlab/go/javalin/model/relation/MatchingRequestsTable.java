package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.json.MatchingRequestJson;
import org.nkjmlab.go.javalin.model.row.MatchingRequest;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;

public class MatchingRequestsTable {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static final String TABLE_NAME = "MATCHING_REQUESTS";

  private static final String USER_ID = "user_id";
  private static final String SEAT_ID = "seat_id";
  private static final String USER_NAME = "user_name";
  private static final String RANK = "rank";
  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";

  private Sorm sorm;

  private TableDefinition schema;

  public MatchingRequestsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableDefinition.builder(TABLE_NAME).addColumnDefinition(USER_ID, VARCHAR, PRIMARY_KEY)
        .addColumnDefinition(SEAT_ID, VARCHAR).addColumnDefinition(USER_NAME, VARCHAR)
        .addColumnDefinition(RANK, INT).addColumnDefinition(GAME_ID, VARCHAR)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).addIndexDefinition(GAME_ID).build();
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);
  }


  public List<MatchingRequestJson> readRequests() {
    List<MatchingRequestJson> result = sorm.selectAll(MatchingRequest.class).stream()
        .map(wr -> new MatchingRequestJson(wr)).collect(Collectors.toList());
    return result;
  }

  public boolean empty() {
    return sorm.selectAll(MatchingRequest.class).size() == 0;
  }

  public List<String> readAllUserIds() {
    return sorm.readList(String.class, select(USER_ID) + from(TABLE_NAME));
  }

  public List<String> readUserIdsOfUnpairedRequestOrdereByCreatedAt() {
    return sorm.readList(String.class,
        select(USER_ID) + from(TABLE_NAME)
            + where(cond(GAME_ID, "=", literal(MatchingRequest.UNPAIRED)))
            + orderByAsc(CREATED_AT));
  }

  public List<MatchingRequest> readUnpairedRequestsOrderByCreatedAt() {
    return sorm.readList(MatchingRequest.class,
        selectStarFrom(TABLE_NAME)
            + where(cond(GAME_ID, "=", literal(MatchingRequest.UNPAIRED)))
            + orderByAsc(CREATED_AT));
  }


  /**
   * マッチングをする u1:30級，u2:27級,u3:26級の順でwaitingRoomに入って来たとする．u2とu3がペアになってu1が待って欲しいが，そうならない．
   * なぜならば，最初にu1がtargetとなって検索が始まる.u1と級位差が小さいu2がペアになってu3が待たされるということ．
   */
  public List<String> createPairOfUsers(GameStatesTables gameStatesTables) {


    List<String> reqs = readUserIdsOfUnpairedRequestOrdereByCreatedAt();

    log.debug("[{}] unpaired matching requests in [{}] matching requests", reqs.size(),
        sorm.readFirst(Integer.class, SELECT + COUNT + "(*)" + FROM + TABLE_NAME));

    List<String> ret = new ArrayList<>();
    for (String uid : reqs) {
      MatchingRequest target = sorm.selectByPrimaryKey(MatchingRequest.class, uid);

      if (!target.isUnpaired()) {
        continue;
      }

      Set<String> pastOpponents = gameStatesTables.readPastOpponents(target.getUserId());
      log.debug("[{}] has been matched with {} on today", target.getUserId(), pastOpponents);

      MatchingRequest nextOpponent = selectNextOponent(target, pastOpponents);
      if (nextOpponent == null) {
        continue;
      }

      String black = target.getRank() >= nextOpponent.getRank() ? target.getUserId()
          : nextOpponent.getUserId();
      String white = target.getRank() >= nextOpponent.getRank() ? nextOpponent.getUserId()
          : target.getUserId();
      String gameId = black + GameStatesTables.VS_SEPARATOR + white;
      target.setGameId(gameId);
      nextOpponent.setGameId(gameId);
      log.debug("[{}] is created", gameId);

      sorm.merge(target);
      sorm.merge(nextOpponent);

      ret.add(target.getUserId());
      ret.add(nextOpponent.getUserId());
    }
    return ret;
  }

  private MatchingRequest selectNextOponent(MatchingRequest target, Set<String> pastOpponents) {

    List<MatchingRequest> unpairedRequestWithoutPastOpponennts =
        readUnpairedRequestsOrderByCreatedAt().stream()
            .filter(r -> !target.getUserId().equals(r.getUserId())
                && !pastOpponents.contains(r.getUserId()))
            .collect(Collectors.toList());

    if (unpairedRequestWithoutPastOpponennts.size() == 0) {
      return null;
    }

    MatchingRequest nextOpponent = unpairedRequestWithoutPastOpponennts.get(0);

    for (MatchingRequest r : unpairedRequestWithoutPastOpponennts) {
      if (Integer.compare(Math.abs(r.getRank() - target.getRank()),
          Math.abs(nextOpponent.getRank() - target.getRank())) < 0) {
        nextOpponent = r;
      }
    }
    return nextOpponent;
  }


  public MatchingRequest readByPrimaryKey(String userId) {
    return sorm.selectByPrimaryKey(MatchingRequest.class, userId);
  }


  public boolean exists(MatchingRequest matchingReq) {
    return sorm.exists(matchingReq);
  }


  public void insert(MatchingRequest matchingReq) {
    sorm.insert(matchingReq);
  }


  public void update(MatchingRequest m) {
    sorm.update(m);
  }


  public void deleteIfExists(MatchingRequest matchingRequest) {
    sorm.delete(matchingRequest);
  }

}
