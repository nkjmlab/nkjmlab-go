package org.nkjmlab.go.javalin.fbauth;

import static org.nkjmlab.go.javalin.model.row.User.*;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.nkjmlab.go.javalin.model.json.UserJson;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.util.javax.servlet.HttpRequestUtils;
import org.nkjmlab.util.javax.servlet.UserSession;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

public class AuthService implements AuthServiceInterface {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final UsersTable usersTable;
  private final LoginsTable loginsTable;
  private final PasswordsTable passwordsTable;
  private final HttpServletRequest request;


  public AuthService(UsersTable usersTable, LoginsTable loginsTable, PasswordsTable passwordsTable,
      HttpServletRequest request) {
    this.usersTable = usersTable;
    this.loginsTable = loginsTable;
    this.passwordsTable = passwordsTable;
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
    User u = usersTable.readByPrimaryKey(userId);
    u.setSeatId(seatId);
    usersTable.merge(u);
    loginsTable.insert(new Login(userId, seatId, u.getUserName(), LocalDateTime.now(),
        HttpRequestUtils.getXForwardedFor(request).orElseGet(() -> request.getRemoteAddr())));
    return true;
  }


  @Override
  public UserJson signinWithFirebase(String idToken, String seatId) {
    return verifyIdToken(idToken).map(token -> usersTable.readByEmail(token.getEmail())).map(u -> {
      FirebaseUserSession session = FirebaseUserSession.wrap(request.getSession());
      session.signinFirebase(idToken, u.getEmail());
      session.setUserId(u.getUserId());
      registerAttendance(u.getUserId(), seatId);
      return new UserJson(u);
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

    User u = usersTable.readByPrimaryKey(userId);
    if (u != null && !u.isGuest()) {
      log.error("Try guest siginup but userId [{}] conflict with a regular user", userId);
      return false;
    }
    usersTable.merge(new User(userId, userId + "-guest@example.com", username, GUEST, seatId, 30,
        LocalDateTime.now()));

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

    User u = usersTable.readByPrimaryKey(userId);
    registerAttendance(userId, seatId);
    UsersTable.createIcon(userId);
    return new UserJson(u);
  }


  public static void initialize(String url, File firebaseJson) {
    try (FileInputStream serviceAccount = new FileInputStream(firebaseJson)) {
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount)).setDatabaseUrl(url).build();
      FirebaseApp.initializeApp(options);
    } catch (Exception e) {
      log.error(e, e);
    }

  }

  public static Optional<FirebaseToken> verifyIdToken(String idToken) {
    try {
      if (idToken == null || idToken.length() == 0) {
        return Optional.empty();
      }
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      return decodedToken.isEmailVerified() ? Optional.of(decodedToken) : Optional.empty();
    } catch (FirebaseAuthException e) {
      log.error(e, e);
      return Optional.empty();
    }
  }

}
