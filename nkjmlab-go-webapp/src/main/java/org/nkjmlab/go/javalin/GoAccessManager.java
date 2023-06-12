package org.nkjmlab.go.javalin;

import java.util.Set;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.security.AccessManager;
import io.javalin.security.RouteRole;

public class GoAccessManager implements AccessManager {

  public enum AccessRole implements RouteRole {

    BEFORE_LOGIN, GUEST, STUDENT, ADMIN;

    static final AccessRole[] LOGIN_ROLES = new AccessRole[] {GUEST, STUDENT, ADMIN};

  }

  private final UsersTable usersTable;
  private final GoAuthService authService;

  public GoAccessManager(UsersTable usersTable, GoAuthService authService) {
    this.usersTable = usersTable;
    this.authService = authService;
  }

  AccessRole toUserRole(UsersTable usersTable, String sessionId) {
    if (!authService.isSignin(sessionId)) {
      return AccessRole.BEFORE_LOGIN;
    }

    User u = authService.toSigninSession(sessionId)
        .map(login -> usersTable.selectByPrimaryKey(login.userId())).orElse(null);
    if (u == null) {
      return AccessRole.BEFORE_LOGIN;
    }
    if (u.isAdmin()) {
      return AccessRole.ADMIN;
    }
    if (u.isStudent()) {
      return AccessRole.STUDENT;
    }
    if (u.isGuest()) {
      return AccessRole.GUEST;
    }
    return AccessRole.BEFORE_LOGIN;
  }


  @Override
  public void manage(Handler handler, Context ctx, Set<? extends RouteRole> routeRoles)
      throws Exception {
    if (routeRoles.size() == 0) {
      handler.handle(ctx);
    } else if (routeRoles.contains(toUserRole(usersTable, ctx.req().getSession().getId()))) {
      handler.handle(ctx);
    } else {
      ctx.redirect("/app/index.html");
    }


  }


}
