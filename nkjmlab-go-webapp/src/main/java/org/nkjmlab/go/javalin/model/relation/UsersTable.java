package org.nkjmlab.go.javalin.model.relation;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.GoAccessManager.AccessRole;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.common.container.RowMap;
import org.nkjmlab.sorm4j.common.container.Tuple.Tuple2;
import org.nkjmlab.sorm4j.extension.h2.functions.table.CsvRead;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTable;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTableBase;
import org.nkjmlab.sorm4j.mapping.annotation.OrmTableName;
import org.nkjmlab.sorm4j.sql.parameterize.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.statement.SelectSql;
import org.nkjmlab.sorm4j.sql.statement.SqlKeyword;
import org.nkjmlab.sorm4j.table.definition.annotation.Index;
import org.nkjmlab.sorm4j.table.definition.annotation.PrimaryKey;
import org.nkjmlab.sorm4j.table.definition.annotation.Unique;

/***
 * Userという名前で統一したかったけど，H2のデフォルトのテーブルと衝突してしまうので，Playerに改名した．
 *
 * @author nkjm
 *
 */
public class UsersTable extends H2DefinedTableBase<User> implements SqlKeyword {
  static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private static final String EMAIL = "email";
  private static final String USER_ID = "user_id";
  private static final String ROLE = "role";

  public UsersTable(DataSource dataSource) {
    super(Sorm.create(dataSource), User.class);
  }

  public User getUser(String uid) {
    User entry = selectByPrimaryKey(uid);
    return entry;
  }

  public User getNextUser(String userId) {
    List<User> users = readAllOrderedUsers();
    for (int i = 0; i < users.size() - 1; i++) {
      if (users.get(i).userId().equals(userId)) {
        return users.get((i + 1) % users.size());
      }
    }
    return users.get(0);
  }

  private List<User> readAllOrderedUsers() {
    return readList(SelectSql.selectStarFrom(getTableName()) + " ORDER BY " + USER_ID);
  }

  public User readByEmail(String email) {
    return readOne(SelectSql.selectStarFrom(getTableName()) + WHERE + EMAIL + "=?", email);
  }

  public void readFileAndInsertIfNotExists(File usersCsvFile) {
    List<UserCsv> csvRows = readInitialUsersCsv(usersCsvFile);

    transformToUser(csvRows.stream().filter(user -> !exists(user.userId())).toList()).stream()
        .forEach(user -> insert(user));
  }

  private List<UserCsv> readInitialUsersCsv(File usersCsvFile) {
    return H2DefinedTable.of(getOrm(), UserCsv.class)
        .readList("select * from " + CsvRead.builderForCsvWithHeader(usersCsvFile).build().getSql())
        .stream()
        .toList();
  }

  public static record UserCsv(
      String userId,
      String email,
      String username,
      String role,
      int initialRank,
      int initialPoint) {}

  private List<User> transformToUser(List<UserCsv> users) {
    return users.stream()
        .map(
            row ->
                new User(
                    row.userId(),
                    row.email(),
                    row.username(),
                    row.role(),
                    "-1",
                    row.initialRank(),
                    row.initialPoint(),
                    LocalDateTime.now()))
        .collect(Collectors.toList());
  }

  public List<User> readListByUids(Collection<String> uids) {
    if (uids.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql st =
        ParameterizedSql.withOrderedParameters(
            "SELECT * from " + getTableName() + " where " + USER_ID + " IN (<?>)", uids);
    return readList(st.getSql(), st.getParameters());
  }

  public List<String> getAdminUserIds() {
    return getOrm()
        .readList(
            String.class,
            "select " + USER_ID + " from " + getTableName() + " where " + ROLE + "=?",
            AccessRole.ADMIN.name());
  }

  public List<String> getStudentUserIds() {
    return getOrm()
        .readList(
            String.class,
            "select " + USER_ID + " from " + getTableName() + " where " + ROLE + "=?",
            AccessRole.STUDENT.name());
  }

  public boolean isAdmin(String userId) {
    return selectByPrimaryKey(userId).isAdmin();
  }

  public List<User> readAll() {
    return selectAll();
  }

  public List<Tuple2<User, Login>> readAllWithLastLogin() {
    ParameterizedSql stmt =
        ParameterizedSql.of(
            "SELECT * except(r.USER_NAME) FROM PLAYERS  LEFT JOIN (SELECT * FROM LOGINS  WHERE ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)) r USING(USER_ID) ORDER BY USER_ID");
    return getOrm().readTupleList(User.class, Login.class, stmt);
  }

  @OrmTableName("PLAYERS")
  public static record User(
      @PrimaryKey String userId,
      @Unique @Index String email,
      String userName,
      @Index String role,
      String seatId,
      int rank,
      int point,
      LocalDateTime createdAt) {

    public static User createBlankUser() {
      return new User("", "", "", AccessRole.STUDENT.name(), "", -1, 0, LocalDateTime.MIN);
    }

    public boolean isAdmin() {
      return role != null && role.equalsIgnoreCase(AccessRole.ADMIN.name());
    }

    public boolean isStudent() {
      return role != null && role.equalsIgnoreCase(AccessRole.STUDENT.name());
    }

    public boolean isGuest() {
      return role != null && role.equalsIgnoreCase(AccessRole.GUEST.name());
    }

    public boolean isTa() {
      return role != null && role.equalsIgnoreCase(AccessRole.TA.name());
    }
  }

  public record UserJson(
      String userId,
      String userName,
      String seatId,
      int rank,
      int point,
      LocalDateTime createdAt,
      boolean attendance) {

    public UserJson(User user, boolean attendance) {
      this(
          user.userId(),
          user.userName(),
          user.seatId(),
          user.rank(),
          user.point(),
          user.createdAt(),
          attendance);
    }

    public static UserJson createNotFound(String userId) {
      return new UserJson(userId, "", "", -1, 0, LocalDateTime.now(), false);
    }
  }

  void updateRankAndPoint(String userId, int currentRank, int currentPoint) {
    updateByPrimaryKey(RowMap.of("rank", currentRank, "point", currentPoint), userId);
  }

  /**
   * GameRecordsTableの最新レコードから復元する
   *
   * @param usersTable
   */
  public void restoreUsersRankAndPointByLatestGameRecord(GameRecordsTable gameRecordsTable) {
    selectAll()
        .forEach(
            user -> {
              GameRecord lastRecord =
                  gameRecordsTable.readFirst(
                      "select * from "
                          + gameRecordsTable.getTableName()
                          + " where "
                          + USER_ID
                          + "=?"
                          + " order by "
                          + " created_at "
                          + " desc limit 1",
                      user.userId());
              if (lastRecord == null) {
                return;
              }
              updateByPrimaryKey(
                  RowMap.of("rank", lastRecord.rank(), "point", lastRecord.point()), user.userId());
            });
  }

  public void modifyRankAndPoint(
      GameRecordsTable gameRecordsTable, String userId, int rank, int point) {
    updateRankAndPoint(userId, rank, point);

    GameRecord rec =
        new GameRecord(
            -1, LocalDateTime.now(), userId, userId, "MODIFY", "MODIFY", rank, point, "MODIFY");
    gameRecordsTable.insert(rec);
  }
}
