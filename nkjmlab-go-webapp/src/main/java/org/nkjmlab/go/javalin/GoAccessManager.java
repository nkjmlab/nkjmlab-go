package org.nkjmlab.go.javalin;

import java.util.Set;

import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;

import io.javalin.http.Context;
import io.javalin.security.RouteRole;

public class GoAccessManager {

  public enum AccessRole implements RouteRole {
    BEFORE_LOGIN,
    GUEST,
    STUDENT,
    TA,
    ADMIN;

    public static final GoAccessManager.AccessRole[] LOGIN_ROLES =
        new GoAccessManager.AccessRole[] {GUEST, STUDENT, TA, ADMIN};
  }

  private final UsersTable usersTable;
  private final GoAuthService authService;

  public GoAccessManager(UsersTable usersTable, GoAuthService authService) {
    this.usersTable = usersTable;
    this.authService = authService;
  }

  GoAccessManager.AccessRole toUserRole(UsersTable usersTable, String sessionId) {
    if (!authService.isSignin(sessionId)) {
      return AccessRole.BEFORE_LOGIN;
    }

    User u =
        authService
            .toSigninSession(sessionId)
            .map(login -> usersTable.selectByPrimaryKey(login.userId()))
            .orElse(null);
    if (u == null) {
      return AccessRole.BEFORE_LOGIN;
    } else if (u.isAdmin()) {
      return AccessRole.ADMIN;
    } else if (u.isStudent()) {
      return AccessRole.STUDENT;
    } else if (u.isGuest()) {
      return AccessRole.GUEST;
    } else if (u.isTa()) {
      return AccessRole.TA;
    } else {
      return AccessRole.BEFORE_LOGIN;
    }
  }

  public void manage(Context ctx) throws Exception {
    Set<RouteRole> routeRoles = ctx.routeRoles();
    if (routeRoles.size() == 0) {
      return;
    } else if (routeRoles.contains(toUserRole(usersTable, ctx.req().getSession().getId()))) {
      return;
    } else {
      ctx.redirect("/app/index.html");
    }
  }
}
