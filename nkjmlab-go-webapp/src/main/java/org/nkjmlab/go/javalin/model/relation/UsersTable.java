package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.go.javalin.GoApplication.*;
import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.annotation.OrmTable;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.sorm4j.util.table_def.annotation.Unique;

/***
 * Userという名前で統一したかったけど，H2のデフォルトのテーブルと衝突してしまうので，Playerに改名した．
 *
 * @author nkjm
 *
 */
public class UsersTable extends BasicH2Table<User> {
  private static final org.apache.logging.log4j.Logger log =
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
    BasicH2Table<UserCsv> table = new BasicH2Table<>(getOrm(), UserCsv.class);

    transformToUser(table.readCsvWithHeader(usersCsvFile)).forEach(user -> {
      createIcon(user.userId());
      insert(user);
    });
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
        "select " + USER_ID + " from " + getTableName() + " where " + ROLE + "=?", User.ADMIN);

  }

  public List<String> getStudentUserIds() {
    return getOrm().readList(String.class,
        "select " + USER_ID + " from " + getTableName() + " where " + ROLE + "=?", User.STUDENT);
  }

  public boolean isAdmin(String userId) {
    return selectByPrimaryKey(userId).isAdmin();
  }



  public static void createIcon(String userId) {
    File uploadedIcon = new File(UPLOADED_ICON_DIR, userId + ".png");
    File initialIcon = new File(INITIAL_ICON_DIR, userId + ".png");

    File srcFile =
        uploadedIcon
            .exists()
                ? uploadedIcon
                : (initialIcon
                    .exists()
                        ? initialIcon
                        : getRandom(Stream.of(RANDOM_ICON_DIR.listFiles())
                            .filter(f -> f.getName().toLowerCase().endsWith(".png")
                                || f.getName().toLowerCase().endsWith(".jpg"))
                            .toList()).orElseThrow());
    try {
      org.apache.commons.io.FileUtils.copyFile(srcFile,
          new File(CURRENT_ICON_DIR, userId + ".png"));
    } catch (IOException e) {
      log.warn(e, e);
    }
  }


  private static <E> Optional<E> getRandom(Collection<E> e) {
    if (e.size() == 0) {
      return Optional.empty();
    }
    return e.stream().skip((ThreadLocalRandom.current().nextInt(e.size()))).findFirst();
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

    public static final String ADMIN = "ADMIN";
    public static final String STUDENT = "STUDENT";
    public static final String TA = "TA";
    public static final String GUEST = "GUEST";

    public User() {
      this("", "", "", STUDENT, "", 30, LocalDateTime.MIN);
    }

    public boolean isAdmin() {
      return role != null && role.equalsIgnoreCase(ADMIN);
    }

    public boolean isStudent() {
      return role != null && role.equalsIgnoreCase(STUDENT);
    }

    public boolean isGuest() {
      return role != null && role.equalsIgnoreCase(GUEST);
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
