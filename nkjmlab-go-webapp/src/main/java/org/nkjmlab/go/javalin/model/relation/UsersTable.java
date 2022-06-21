package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.go.javalin.GoApplication.*;
import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
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
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils.Row;
import com.orangesignal.csv.CsvConfig;

/***
 * Userという名前で統一したかったけど，H2のデフォルトのテーブルと衝突してしまうので，Playerに改名した．
 *
 * @author nkjm
 *
 */
public class UsersTable {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static final String TABLE_NAME = "PLAYERS";

  private static final String EMAIL = "email";
  private static final String USER_ID = "user_id";
  private static final String USER_NAME = "user_name";
  private static final String ROLE = "role";
  private static final String SEAT_ID = "seat_id";
  private static final String RANK = "rank";
  private static final String CREATED_AT = "created_at";

  private Sorm sorm;
  private TableDefinition schema;

  public UsersTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema =
        TableDefinition.builder(TABLE_NAME).addColumnDefinition(USER_ID, VARCHAR, PRIMARY_KEY)
            .addColumnDefinition(EMAIL, VARCHAR, UNIQUE).addColumnDefinition(USER_NAME, VARCHAR)
            .addColumnDefinition(ROLE, VARCHAR).addColumnDefinition(SEAT_ID, VARCHAR)
            .addColumnDefinition(RANK, INT).addColumnDefinition(CREATED_AT, TIMESTAMP)
            .addIndexDefinition(EMAIL).addIndexDefinition(ROLE).build();
  }

  public void dropTableIfExists() {
    schema.dropTableIfExists(sorm);
  }


  public void createTableAndIndexesIfNotExists() {
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);
  }


  public User getUser(String uid) {
    User entry = sorm.selectByPrimaryKey(User.class, uid);
    return entry;
  }

  public User getNextUser(String userId) {
    List<User> users = readAllOrderedUsers();
    for (int i = 0; i < users.size() - 1; i++) {
      if (users.get(i).getUserId().equals(userId)) {
        return users.get((i + 1) % users.size());
      }
    }
    return users.get(0);
  }

  private List<User> readAllOrderedUsers() {
    return sorm.readList(User.class, selectStarFrom(TABLE_NAME) + " ORDER BY " + USER_ID);

  }

  public User readByEmail(String email) {
    return sorm.readOne(User.class, selectStarFrom(TABLE_NAME) + WHERE + EMAIL + "=?", email);
  }



  public void readFromFileAndMerge(File usersCsvFile) {
    CsvConfig conf = OrangeSignalCsvUtils.createDefaultCsvConfig();
    conf.setSkipLines(1);
    List<Row> users = OrangeSignalCsvUtils.readAllRows(usersCsvFile, conf);
    transformToUser(users).forEach(user -> {
      createIcon(user.getUserId());
      sorm.insert(user);
    });
  }



  private static List<User> transformToUser(List<Row> rows) {
    return rows.stream().map(row -> {
      User user =
          new User(row.get(0), row.get(1), row.get(2), row.get(3), "-1", 30, LocalDateTime.now());
      return user;
    }).collect(Collectors.toList());
  }

  public List<User> readListByUids(Collection<String> uids) {
    if (uids.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql st = OrderedParameterSqlParser
        .of("SELECT * from " + TABLE_NAME + " where " + USER_ID + " IN (<?>)").addParameter(uids)
        .parse();
    return sorm.readList(User.class, st.getSql(), st.getParameters());
  }

  public List<String> getAdminUserIds() {
    return sorm.readList(String.class,
        "select " + USER_ID + " from " + TABLE_NAME + " where " + ROLE + "=?", User.ADMIN);

  }

  public List<String> getStudentUserIds() {
    return sorm.readList(String.class,
        "select " + USER_ID + " from " + TABLE_NAME + " where " + ROLE + "=?", User.STUDENT);
  }

  public boolean isAdmin(String userId) {
    return sorm.selectByPrimaryKey(User.class, userId).isAdmin();
  }


  public User selectByPrimaryKey(String userId) {
    return sorm.selectByPrimaryKey(User.class, userId);
  }


  public void merge(User u) {
    sorm.merge(u);
  }


  public void update(User u) {
    sorm.update(u);
  }


  public void insert(User user) {
    sorm.insert(user);
  }


  public boolean exists(User user) {
    return sorm.exists(user);
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
    return sorm.selectAll(User.class);
  }

  public List<Tuple2<User, Login>> readAllWithLastLogin() {
    ParameterizedSql stmt = ParameterizedSqlParser.parse(
        "SELECT * except(r.USER_NAME) FROM PLAYERS  LEFT JOIN (SELECT * FROM LOGINS  WHERE ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)) r USING(USER_ID) ORDER BY USER_ID");
    return sorm.readTupleList(User.class, Login.class, stmt);
  }



}
