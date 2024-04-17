package org.nkjmlab.go.javalin.jsonrpc;

import java.time.LocalDateTime;
import java.util.Optional;
import org.nkjmlab.go.javalin.GoAccessManager.AccessRole;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService.SigninSession;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.sorm4j.result.RowMap;
import org.nkjmlab.util.jakarta.servlet.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;

public class AuthService implements AuthServiceInterface {

  public static class Factory {
    private final GoTables goTables;
    private final GoAuthService firebaseService;

    public Factory(GoTables goTables, GoAuthService firebaseService) {
      this.goTables = goTables;
      this.firebaseService = firebaseService;
    }

    public AuthService create(HttpServletRequest request) {
      return new AuthService(goTables, firebaseService, request);
    }
  }

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final GoTables goTables;
  private final GoAuthService authService;
  private final HttpServletRequest request;

  private AuthService(
      GoTables goTables, GoAuthService firebaseService, HttpServletRequest request) {
    this.goTables = goTables;
    this.authService = firebaseService;
    this.request = request;
  }

  @Override
  public boolean isSigninToFirebase() {
    return authService.isSignin(request.getSession().getId());
  }

  @Override
  public boolean registerAttendance(String userId, String seatId) {
    User u = goTables.usersTable.selectByPrimaryKey(userId);
    goTables.usersTable.updateByPrimaryKey(RowMap.of("seat_id", seatId), u.userId());
    goTables.loginsTable.login(
        u, HttpRequestUtils.getXForwardedFor(request).orElseGet(() -> request.getRemoteAddr()));
    return true;
  }

  @Override
  public UserJson signinWithFirebase(String idToken, String seatId) {
    Optional<SigninSession> opt =
        authService.signinWithFirebase(idToken, request.getSession().getId());
    return opt.map(
            ls -> {
              User u = goTables.usersTable.selectByPrimaryKey(ls.userId());
              registerAttendance(ls.userId(), seatId);
              authService.signin(request.getSession().getId(), ls.userId());
              return new UserJson(u, true);
            })
        .orElseThrow();
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

    User u = goTables.usersTable.selectByPrimaryKey(userId);
    if (u != null && !u.isGuest()) {
      log.error("Try guest signinup but userId [{}] conflict with a regular user", userId);
      return false;
    }
    goTables.usersTable.merge(
        new User(
            userId,
            userId + "-guest@example.com",
            username,
            AccessRole.GUEST.name(),
            seatId,
            30,
            LocalDateTime.now()));

    registerAttendance(userId, seatId);
    goTables.icons.createIcon(userId);
    authService.signin(request.getSession().getId(), userId);
    return true;
  }

  @Override
  public UserJson signinWithoutFirebase(String userId, String password, String seatId) {
    if (!goTables.passwordsTable.isValid(userId, password)) {
      throw new RuntimeException("無効なユーザID/パスワードです");
    }
    User u = goTables.usersTable.selectByPrimaryKey(userId);
    registerAttendance(userId, seatId);
    goTables.icons.createIcon(userId);
    authService.signin(request.getSession().getId(), userId);
    return new UserJson(u, true);
  }
}
