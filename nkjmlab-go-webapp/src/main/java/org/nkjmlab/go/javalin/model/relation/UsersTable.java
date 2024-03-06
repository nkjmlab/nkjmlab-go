package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.selectStarFrom;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.GoAccessManager.AccessRole;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.annotation.OrmTable;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.h2.functions.table.CsvRead;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.sorm4j.util.table_def.annotation.Unique;

/***
 * Userという名前で統一したかったけど，H2のデフォルトのテーブルと衝突してしまうので，Playerに改名した．
 *
 * @author nkjm
 *
 */
public class UsersTable extends H2BasicTable<User> {
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
    return readList(selectStarFrom(getTableName()) + " ORDER BY " + USER_ID);

  }

  public User readByEmail(String email) {
    return readOne(selectStarFrom(getTableName()) + WHERE + EMAIL + "=?", email);
  }



  public void readFromFileAndMerge(File usersCsvFile) {
    H2BasicTable<UserCsv> table = new H2BasicTable<>(getOrm(), UserCsv.class);
    List<UserCsv> csvRows = table.readList(
        "select * from " + CsvRead.builderForCsvWithHeader(usersCsvFile).build().getSql());

    transformToUser(csvRows).forEach(user -> insert(user));
  }



  @OrmRecord
  public static record UserCsv(String userId, String email, String username, String role) {

  }



  private List<User> transformToUser(List<UserCsv> users) {
    return users.stream().map(row -> new User(row.userId(), row.email(), row.username(), row.role(),
        "-1", 30, LocalDateTime.now())).collect(Collectors.toList());
  }

  public List<User> readListByUids(Collection<String> uids) {
    if (uids.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql st = OrderedParameterSqlParser
        .of("SELECT * from " + getTableName() + " where " + USER_ID + " IN (<?>)")
        .addParameter(uids).parse();
    return readList(st.getSql(), st.getParameters());
  }

  public List<String> getAdminUserIds() {
    return getOrm().readList(String.class,
        "select " + USER_ID + " from " + getTableName() + " where " + ROLE + "=?",
        AccessRole.ADMIN.name());

  }

  public List<String> getStudentUserIds() {
    return getOrm().readList(String.class,
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
    ParameterizedSql stmt = ParameterizedSqlParser.parse(
        "SELECT * except(r.USER_NAME) FROM PLAYERS  LEFT JOIN (SELECT * FROM LOGINS  WHERE ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)) r USING(USER_ID) ORDER BY USER_ID");
    return getOrm().readTupleList(User.class, Login.class, stmt);
  }


  @OrmRecord
  @OrmTable("PLAYERS")
  public static record User(@PrimaryKey String userId, @Unique @Index String email, String userName,
      @Index String role, String seatId, int rank, LocalDateTime createdAt) {


    public User() {
      this("", "", "", AccessRole.STUDENT.name(), "", 30, LocalDateTime.MIN);
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


  public record UserJson(String userId, String userName, String seatId, int rank,
      LocalDateTime createdAt, boolean attendance) {


    public UserJson(User user, boolean attendance) {
      this(user.userId(), user.userName(), user.seatId(), user.rank(), user.createdAt(),
          attendance);
    }

    public UserJson(String userId) {
      this(userId, "", "", 30, LocalDateTime.now(), false);
    }

  }


}
