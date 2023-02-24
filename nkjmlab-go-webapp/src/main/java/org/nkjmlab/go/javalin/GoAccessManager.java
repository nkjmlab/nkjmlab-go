package org.nkjmlab.go.javalin;

import java.util.Set;
import org.nkjmlab.go.javalin.fbauth.FirebaseUserSession;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.util.jakarta.servlet.UserSession;
import org.nkjmlab.util.java.lang.ParameterizedStringFormat;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.security.AccessManager;
import io.javalin.security.RouteRole;

public class GoAccessManager implements AccessManager {

  public enum UserRole implements RouteRole {

    BEFORE_LOGIN, GUEST, STUDENT, ADMIN;

    static final UserRole[] LOGIN_ROLES = new UserRole[] {GUEST, STUDENT, ADMIN};

    static UserRole of(UsersTable usersTable, UserSession session) {
      if (!session.isLogined()) {
        return BEFORE_LOGIN;
      }
      FirebaseUserSession fSession = FirebaseUserSession.wrap(session.getSession());
      User u = null;
      if (fSession.isSigninFirebase()) {
        String email = fSession.getEmail().orElseThrow(() -> new RuntimeException(
            ParameterizedStringFormat.DEFAULT.format("Email is not set in the session")));
        u = usersTable.readByEmail(email);
      } else if (fSession.isLogined()) {
        u = fSession.getUserId().map(userId -> usersTable.selectByPrimaryKey(userId)).orElse(null);
      }
      if (u == null) {
        return BEFORE_LOGIN;
      }
      if (u.isAdmin()) {
        return ADMIN;
      }
      if (u.isStudent()) {
        return STUDENT;
      }
      if (u.isGuest()) {
        return GUEST;
      }
      return BEFORE_LOGIN;
    }
  }

  private UsersTable usersTable;

  public GoAccessManager(UsersTable usersTable) {
    this.usersTable = usersTable;
  }

  @Override
  public void manage(Handler handler, Context ctx, Set<? extends RouteRole> routeRoles)
      throws Exception {
    if (routeRoles.size() == 0) {
      handler.handle(ctx);
    } else if (routeRoles
        .contains(UserRole.of(usersTable, UserSession.wrap(ctx.req().getSession())))) {
      handler.handle(ctx);
    } else {
      ctx.redirect("/app/index.html");
    }


  }


}
