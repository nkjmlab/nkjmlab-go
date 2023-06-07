package org.nkjmlab.go.javalin.jsonrpc;

import java.time.LocalDateTime;
import java.util.Optional;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService.SigninSession;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.sorm4j.result.RowMap;
import org.nkjmlab.util.jakarta.servlet.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;

public class AuthService implements AuthServiceInterface {

  public static class Factory {
    private final UsersTable usersTable;
    private final LoginsTable loginsTable;
    private final PasswordsTable passwordsTable;
    private final GoAuthService firebaseService;

    public Factory(UsersTable usersTable, LoginsTable loginsTable, PasswordsTable passwordsTable,
        GoAuthService firebaseService) {
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
  private final GoAuthService authService;
  private final HttpServletRequest request;

  private AuthService(UsersTable usersTable, LoginsTable loginsTable, PasswordsTable passwordsTable,
      GoAuthService firebaseService, HttpServletRequest request) {
    this.usersTable = usersTable;
    this.loginsTable = loginsTable;
    this.passwordsTable = passwordsTable;
    this.authService = firebaseService;
    this.request = request;
  }

  @Override
  public boolean isSigninToFirebase() {
    return authService.isSignin(request.getSession().getId());
  }

  @Override
  public boolean registerAttendance(String userId, String seatId) {
    User u = usersTable.selectByPrimaryKey(userId);
    usersTable.updateByPrimaryKey(RowMap.of("seat_id", seatId), u.userId());
    loginsTable.insert(new Login(-1, userId, seatId, u.userName(), LocalDateTime.now(),
        HttpRequestUtils.getXForwardedFor(request).orElseGet(() -> request.getRemoteAddr())));
    return true;
  }


  @Override
  public UserJson signinWithFirebase(String idToken, String seatId) {
    Optional<SigninSession> opt =
        authService.signinWithFirebase(idToken, request.getSession().getId());
    return opt.map(ls -> {
      User u = usersTable.selectByPrimaryKey(ls.userId());
      registerAttendance(ls.userId(), seatId);
      authService.signin(request.getSession().getId(), ls.userId());
      return new UserJson(u, true);
    }).orElseThrow();
  }


  @Override
  public boolean signoutFromFirebase() {
    authService.signout(request.getSession().getId());
    return true;

  }

  @Override
  public boolean signupAsGuest(String userId, String username, String seatId) {
    if (authService.isSignin(request.getSession().getId())) {
      log.error("Already logined Firebase. userId=[{}]", userId);
      return false;
    }

    User u = usersTable.selectByPrimaryKey(userId);
    if (u != null && !u.isGuest()) {
      log.error("Try guest signinup but userId [{}] conflict with a regular user", userId);
      return false;
    }
    usersTable.merge(new User(userId, userId + "-guest@example.com", username, User.GUEST, seatId,
        30, LocalDateTime.now()));

    registerAttendance(userId, seatId);
    UsersTable.createIcon(userId);
    authService.signin(request.getSession().getId(), userId);
    return true;
  }

  @Override
  public UserJson signinWithoutFirebase(String userId, String password, String seatId) {
    if (authService.isSignin(request.getSession().getId())) {
      log.error("Already logined Firebase. userId=[{}]", userId);
      return null;
    }

    if (!passwordsTable.isValid(userId, password)) {
      return null;
    }

    User u = usersTable.selectByPrimaryKey(userId);
    registerAttendance(userId, seatId);
    UsersTable.createIcon(userId);
    authService.signin(request.getSession().getId(), userId);
    return new UserJson(u, true);
  }


}
