package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.sql.schema.TableSchema.Keyword.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.row.GameRecord;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.sql.SelectSql;
import org.nkjmlab.sorm4j.sql.schema.TableSchema;
import org.nkjmlab.util.h2.H2StatementUtils;

public class GameRecordsTable {

  public static final String TABLE_NAME = "GAME_RECORDS";
  private static final String ID = "id";
  private static final String CREATED_AT = "created_at";
  private static final String USER_ID = "user_id";
  private static final String OPPONENT_USER_ID = "opponent_user_id";
  private static final String JADGE = "jadge";
  private static final String MEMO = "memo";
  private static final String MESSAGE = "message";
  private static final String RANK = "rank";
  private static final String POINT = "point";
  private Sorm sorm;
  private TableSchema schema;


  public GameRecordsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = new TableSchema.Builder(TABLE_NAME)
        .addColumnDefinition(ID, INT, AUTO_INCREMENT, PRIMARY_KEY)
        .addColumnDefinition(CREATED_AT, "TIMESTAMP AS CURRENT_TIMESTAMP")
        .addColumnDefinition(USER_ID, VARCHAR).addColumnDefinition(OPPONENT_USER_ID, VARCHAR)
        .addColumnDefinition(JADGE, VARCHAR).addColumnDefinition(MEMO, VARCHAR)
        .addColumnDefinition(RANK, INT).addColumnDefinition(POINT, INT)
        .addColumnDefinition(MESSAGE, VARCHAR).build();
    schema.createTableAndIndexesIfNotExists(sorm);
  }

  public void recalculateAndUpdateRank(UsersTable usersTable) {
    sorm.readMapList("SELECT USER_ID, MIN(RANK) AS RANK FROM GAME_RECORDS GROUP BY USER_ID")
        .forEach(m -> {
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
    GameRecord lastRecords = sorm.readFirst(GameRecord.class, "select * from " + TABLE_NAME
        + " where " + USER_ID + "=?" + " order by " + CREATED_AT + " desc limit 1", userId);


    int rank = lastRecords == null
        ? Optional.ofNullable(usersTable.readByPrimaryKey(userId)).map(u -> u.getRank()).orElse(30)
        : lastRecords.getRank();
    int point = lastRecords == null ? 0 : lastRecords.getPoint();
    String message = "";

    point += toScore(jadge);

    if (point >= getThreshold(rank)) {
      rank--;
      point = 0;
      message = rank + "級に昇級 <i class='fas fa-trophy'></i>";
    }

    sorm.insert(new GameRecord(userId, opponentUserId, jadge, memo, rank, point, message));
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
    return sorm.readList(GameRecord.class,
        "select * from " + TABLE_NAME + " where " + USER_ID + "=? order by " + CREATED_AT + " DESC",
        userId);
  }

  public void backupToCsv(File backupDir) {
    String stmt = H2StatementUtils.getCsvWriteSql(
        new File(backupDir, System.currentTimeMillis() + ".csv"), SelectSql.selectFrom(TABLE_NAME),
        StandardCharsets.UTF_8, ",");
    sorm.executeUpdate(stmt);
  }



}
