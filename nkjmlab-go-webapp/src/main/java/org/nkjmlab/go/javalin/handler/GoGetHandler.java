package org.nkjmlab.go.javalin.handler;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.util.java.net.UrlUtils;
import org.nkjmlab.util.java.web.ViewModel;
import org.nkjmlab.util.java.web.WebJarsUtils;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.servlet.http.HttpServletRequest;

class GoGetHandler implements Handler {

  private final GoViewHandler handler;
  private final GoTables goTables;
  private final GoAuthService authService;
  private final ViewModelBuilderTemplate template;

  public GoGetHandler(
      Path webRootDirectory, GoTables goTables, GoAuthService authService, GoViewHandler handler) {
    this.handler = handler;
    this.goTables = goTables;
    this.authService = authService;
    this.template = new ViewModelBuilderTemplate(webRootDirectory);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    String filePath = UrlUtils.of(ctx.url()).getPath().replaceFirst("^/app/", "");
    ViewModel.Builder model = createDefaultViewModelBuilder(ctx.req());
    handler.apply(ctx).apply(filePath).accept(model);
  }

  static class ViewModelBuilderTemplate {

    private final Map<String, Object> viewModelTemplate;

    public ViewModelBuilderTemplate(Path webRootDir) {
      Map<String, String> webjars =
          WebJarsUtils.findWebJarsVersionsFromClasspath(
              "jquery",
              "sweetalert2",
              "bootstrap",
              "bootstrap-treeview",
              "clipboard",
              "fortawesome__fontawesome-free",
              "stacktrace-js",
              "datatables.net",
              "datatables.net-bs5",
              "firebase",
              "firebaseui",
              "ua-parser-js",
              "blueimp-load-image",
              "emojionearea");
      this.viewModelTemplate =
          ViewModel.builder()
              .setFileModifiedDate(webRootDir, 10, "js", "css")
              .put("webjars", webjars)
              .build();
    }

    final ViewModel.Builder builder() {
      return ViewModel.builder(viewModelTemplate);
    }
  }

  private ViewModel.Builder createDefaultViewModelBuilder(HttpServletRequest request) {
    User u =
        authService
            .toSigninSession(request.getSession().getId())
            .map(uid -> goTables.usersTable.selectByPrimaryKey(uid.userId()))
            .orElse(new User());

    Map<String, Object> map = template.builder().put("currentUser", u).build();
    return ViewModel.builder(map);
  }

  static interface GoViewHandler
      extends Function<Context, Function<String, Consumer<ViewModel.Builder>>> {}
}
