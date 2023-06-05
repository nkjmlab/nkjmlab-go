package org.nkjmlab.go.javalin.auth;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.sorm4j.result.RowMap;
import org.nkjmlab.util.firebase.auth.BasicFirebaseService;
import org.nkjmlab.util.jakarta.servlet.HttpRequestUtils;
import org.nkjmlab.util.jakarta.servlet.UserSession;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.http.HttpServletRequest;

public class AuthService implements AuthServiceInterface {

  public static class Factory {
    private final UsersTable usersTable;
    private final LoginsTable loginsTable;
    private final PasswordsTable passwordsTable;
    private final BasicFirebaseService firebaseService;

    public Factory(UsersTable usersTable, LoginsTable loginsTable, PasswordsTable passwordsTable,
        BasicFirebaseService firebaseService) {
      this.usersTable = usersTable;
      this.loginsTable = loginsTable;
      this.passwordsTable = passwordsTable;
      this.firebaseService = firebaseService;
    }

    public AuthService create(HttpServletRequest request) {
      return new AuthService(usersTable, loginsTable, passwordsTable, firebaseService, request);
    }

  }

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final UsersTable usersTable;
  private final LoginsTable loginsTable;
  private final PasswordsTable passwordsTable;
  private final BasicFirebaseService firebaseService;
  private final HttpServletRequest request;

  private AuthService(UsersTable usersTable, LoginsTable loginsTable, PasswordsTable passwordsTable,
      BasicFirebaseService firebaseService, HttpServletRequest request) {
    this.usersTable = usersTable;
    this.loginsTable = loginsTable;
    this.passwordsTable = passwordsTable;
    this.firebaseService = firebaseService;
    this.request = request;
  }

  @Override
  public boolean isSigninToFirebase() {
    FirebaseUserSession session = FirebaseUserSession.wrap(request.getSession());
    return session.isSigninFirebase();
  }

  @Override
  public boolean registerAttendance(String userId, String seatId) {
    UserSession session = UserSession.wrap(request.getSession());
    session.setUserId(userId);
    User u = usersTable.selectByPrimaryKey(userId);
    usersTable.updateByPrimaryKey(RowMap.of("seat_id", seatId), u.userId());
    loginsTable.insert(new Login(-1, userId, seatId, u.userName(), LocalDateTime.now(),
        HttpRequestUtils.getXForwardedFor(request).orElseGet(() -> request.getRemoteAddr())));
    return true;
  }


  @Override
  public UserJson signinWithFirebase(String idToken, String seatId) {
    return firebaseService.verifyIdToken(idToken)
        .map(token -> usersTable.readByEmail(token.getEmail())).map(u -> {
          FirebaseUserSession session = FirebaseUserSession.wrap(request.getSession());
          session.signinFirebase(idToken, u.email());
          session.setUserId(u.userId());
          registerAttendance(u.userId(), seatId);
          return new UserJson(u, true);
        }).orElseThrow();
  }


  @Override
  public boolean signoutFromFirebase() {
    request.getSession().invalidate();
    return true;

  }

  @Override
  public boolean signupAsGuest(String userId, String username, String seatId) {
    FirebaseUserSession session = FirebaseUserSession.wrap(request.getSession());
    if (session.isSigninFirebase()) {
      log.error("Already logined Firebase. userId=[{}]", userId);
      return false;
    }

    User u = usersTable.selectByPrimaryKey(userId);
    if (u != null && !u.isGuest()) {
      log.error("Try guest siginup but userId [{}] conflict with a regular user", userId);
      return false;
    }
    usersTable.merge(new User(userId, userId + "-guest@example.com", username, User.GUEST, seatId,
        30, LocalDateTime.now()));

    registerAttendance(userId, seatId);
    UsersTable.createIcon(userId);
    return true;
  }

  @Override
  public UserJson signinWithoutFirebase(String userId, String password, String seatId) {
    FirebaseUserSession session = FirebaseUserSession.wrap(request.getSession());
    if (session.isSigninFirebase()) {
      log.error("Already logined Firebase. userId=[{}]", userId);
      return null;
    }

    if (!passwordsTable.isValid(userId, password)) {
      return null;
    }

    User u = usersTable.selectByPrimaryKey(userId);
    registerAttendance(userId, seatId);
    UsersTable.createIcon(userId);
    return new UserJson(u, true);
  }


  public static void initialize(File firebaseJson) {
    try (FileInputStream serviceAccount = new FileInputStream(firebaseJson)) {
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
      FirebaseApp.initializeApp(options);
    } catch (Exception e) {
      log.error(e, e);
    }

  }

}
