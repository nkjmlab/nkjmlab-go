package org.nkjmlab.go.javalin;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.util.java.net.UrlUtils;
import org.nkjmlab.util.java.web.ViewModel;
import org.nkjmlab.util.java.web.WebApplicationConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.servlet.http.HttpServletRequest;

class GoGetHandler implements Handler {

  private final GoViewHandler handler;
  private final GoTables goTables;
  private final GoAuthService authService;
  private final WebApplicationConfig webAppConfig;

  public GoGetHandler(WebApplicationConfig webAppConfig, GoTables goTables,
      GoAuthService authService, GoViewHandler handler) {
    this.handler = handler;
    this.goTables = goTables;
    this.authService = authService;
    this.webAppConfig = webAppConfig;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    String filePath = UrlUtils.of(ctx.url()).getPath().replaceFirst("^/app/", "");
    ViewModel.Builder model = createDefaultViewModelBuilder(ctx.req());
    handler.apply(ctx).apply(filePath).accept(model);
  }

  private ViewModel.Builder createDefaultViewModelBuilder(HttpServletRequest request) {
    User u = authService.toSigninSession(request.getSession().getId())
        .map(uid -> goTables.usersTable.selectByPrimaryKey(uid.userId())).orElse(new User());

    Map<String, Object> map =
        ViewModel.builder().setFileModifiedDate(webAppConfig.getWebRootDirectory(), 10, "js", "css")
            .put("webjars", webAppConfig.getWebJars()).put("currentUser", u).build();
    return ViewModel.builder(map);
  }

  static interface GoViewHandler
      extends Function<Context, Function<String, Consumer<ViewModel.Builder>>> {
  }


}
