package org.nkjmlab.go.javalin.model.relation;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTableBase;
import org.nkjmlab.sorm4j.sql.statement.SqlKeyword;
import org.nkjmlab.sorm4j.sql.statement.SqlTrait;
import org.nkjmlab.sorm4j.table.definition.annotation.AutoIncrement;
import org.nkjmlab.sorm4j.table.definition.annotation.PrimaryKey;

public class GameRecordsTable extends H2DefinedTableBase<GameRecord>
    implements SqlTrait, SqlKeyword {
  private static final String CREATED_AT = "created_at";
  private static final String USER_ID = "user_id";

  public GameRecordsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), GameRecord.class);
  }

  public void registerGameResultAndUpdateUserRankAndPoint(
      UsersTable usersTable, User user, String opponentUserId, String jadge, String memo) {
    RankAndPoint rankAndPoint = calcRankAndPoint(user, jadge, memo);
    usersTable.updateRankAndPoint(user.userId(), rankAndPoint.rank(), rankAndPoint.point());
    GameRecord ret =
        new GameRecord(
            -1,
            LocalDateTime.now(),
            user.userId(),
            opponentUserId,
            jadge,
            memo,
            rankAndPoint.rank(),
            rankAndPoint.point(),
            rankAndPoint.message());
    insert(ret);
  }

  private RankAndPoint calcRankAndPoint(User user, String jadge, String memo) {
    int currentRank = user.rank();
    int currentPoint = user.point();

    String message = "";
    currentPoint += toScore(jadge);

    if (currentPoint >= getThreshold(currentRank)) {
      currentRank--;
      currentPoint = 0;
      message = currentRank + "級に昇級 <i class='fas fa-trophy'></i>";
    }

    return new RankAndPoint(currentRank, currentPoint, message);
  }

  private static record RankAndPoint(int rank, int point, String message) {}

  private static int toScore(String jadge) {
    return switch (jadge) {
      case "WIN" -> 10;
      case "DRAW" -> 5;
      case "LOSE", "OTHER" -> 0;
      default -> throw new IllegalArgumentException(jadge + " is invalid");
    };
  }

  private int getThreshold(int currentRank) {
    if (currentRank >= 26) {
      return 20;
    } else if (currentRank >= 21) {
      return 30;
    } else if (currentRank >= 16) {
      return 40;
    } else if (currentRank >= 11) {
      return 50;
    } else {
      return 60;
    }
  }

  public List<GameRecord> readByUserId(String userId) {
    return readList(
        "select * from "
            + getTableName()
            + " where "
            + USER_ID
            + "=? order by "
            + CREATED_AT
            + " DESC",
        userId);
  }

  public static record GameRecord(
      @PrimaryKey @AutoIncrement int id,
      LocalDateTime createdAt,
      String userId,
      String opponentUserId,
      String jadge,
      String memo,
      int rank,
      int point,
      String message) {}
}
