package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.AutoIncrement;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;

public class LoginsTable extends BasicH2Table<Login> {


  private static final String USER_ID = "user_id";


  public LoginsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), Login.class);
  }

  private List<Login> readAllLastLoginsOrderByUserId() {
    return readList(selectStarFrom(getTableName())
        + where("ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)") + orderBy(USER_ID));
  }

  public List<Login> readOrderedActiveStudentLogins(UsersTable usersTable) {
    LocalDate nowDate = LocalDate.now();
    return readAllLastLoginsOrderByUserId().stream()
        .filter(l -> Optional.ofNullable(usersTable.selectByPrimaryKey(l.userId()))
            .map(u -> u.isStudent()).orElse(false) && l.loggedInAt().toLocalDate().equals(nowDate))
        .collect(Collectors.toList());
  }

  public String getNextLoginUserId(UsersTable usersTable, String userId) {
    // List<String> users = readOrderedActiveStudentLogins(usersTable).stream().map(l ->
    // l.getUserId()) .collect(Collectors.toList());

    List<String> users = usersTable.getStudentUserIds();
    return getNext(users, userId);
  }

  public String getPrevLoginUserId(UsersTable usersTable, String userId) {
    // List<String> users = readOrderedActiveStudentLogins(usersTable).stream().map(l ->
    // l.getUserId()) .collect(Collectors.toList());
    List<String> users = usersTable.getStudentUserIds();
    Collections.reverse(users);
    return getNext(users, userId);
  }

  private static String getNext(List<String> userIds, String userId) {
    for (int i = 0; i < userIds.size() - 1; i++) {
      if (userIds.get(i).equals(userId)) {
        return userIds.get((i + 1) % userIds.size());
      }
    }
    return userIds.size() == 0 ? "" : userIds.get(0);
  }

  public void login(User u, String remoteAddr) {
    insert(new Login(-1, u.userId(), u.seatId(), u.userName(), LocalDateTime.now(), remoteAddr));
  }



  public Optional<Login> readLastLogin(String userId) {

    return Optional.ofNullable(readFirst(
        "SELECT * FROM LOGINS WHERE USER_ID=? ORDER BY LOGGED_IN_AT DESC LIMIT 1", userId));

  }

  public boolean isAttendance(String userId) {
    return readLastLogin(userId).map(l -> l.loggedInAt().toLocalDate().equals(LocalDate.now()))
        .orElse(false);
  }


  @OrmRecord
  public static record Login(@PrimaryKey @AutoIncrement long id, @Index String userId,
      String seatId, String userName, LocalDateTime loggedInAt, String remoteAddr) {

  }


}
