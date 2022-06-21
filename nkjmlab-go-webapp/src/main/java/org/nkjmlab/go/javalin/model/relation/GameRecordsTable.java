package org.nkjmlab.go.javalin.model.relation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.result.RowMap;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.AutoIncrement;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;

public class GameRecordsTable extends BasicH2Table<GameRecord> {

  public static final String TABLE_NAME = "GAME_RECORDS";
  private static final String CREATED_AT = "created_at";
  private static final String USER_ID = "user_id";
  private static final String RANK = "rank";


  public GameRecordsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), GameRecord.class);
  }

  public void recalculateAndUpdateRank(UsersTable usersTable) {
    getOrm().readList(RowMap.class,
        "SELECT USER_ID, MIN(RANK) AS RANK FROM GAME_RECORDS GROUP BY USER_ID").forEach(m -> {
          String userId = m.get(USER_ID.toLowerCase()).toString();
          Integer rank = Integer.valueOf(m.get(RANK.toLowerCase()).toString());
          User u = usersTable.readByPrimaryKey(userId);
          if (u == null) {
            return;
          }
          u.setRank(rank);
          usersTable.update(u);
        });

  }

  public int registerRecordAndGetRank(UsersTable usersTable, String userId, String opponentUserId,
      String jadge, String memo) {
    GameRecord lastRecords = readFirst("select * from " + TABLE_NAME + " where " + USER_ID + "=?"
        + " order by " + CREATED_AT + " desc limit 1", userId);


    int rank = lastRecords == null
        ? Optional.ofNullable(usersTable.readByPrimaryKey(userId)).map(u -> u.getRank()).orElse(30)
        : lastRecords.rank();
    int point = lastRecords == null ? 0 : lastRecords.point();
    String message = "";

    point += toScore(jadge);

    if (point >= getThreshold(rank)) {
      rank--;
      point = 0;
      message = rank + "級に昇級 <i class='fas fa-trophy'></i>";
    }

    insert(new GameRecord(-1, LocalDateTime.now(), userId, opponentUserId, jadge, memo, rank, point,
        message));
    return rank;
  }

  private static int toScore(String jadge) {
    switch (jadge) {
      case "WIN":
        return 10;
      case "DRAW":
        return 5;
      case "LOSE":
      case "OTHER":
        return 0;
      default:
        throw new RuntimeException(jadge);
    }
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
        "select * from " + TABLE_NAME + " where " + USER_ID + "=? order by " + CREATED_AT + " DESC",
        userId);
  }


  public static record GameRecord(@PrimaryKey @AutoIncrement int id, LocalDateTime createdAt,
      String userId, String opponentUserId, String jadge, String memo, int rank, int point,
      String message) {


  }

}
