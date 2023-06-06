package org.nkjmlab.go.javalin;

import java.io.File;
import org.nkjmlab.util.java.web.WebApplicationConfig;

public class GoWebAppConfig {
  public static final WebApplicationConfig WEB_APP_CONFIG = WebApplicationConfig.builder()
      .addWebJar("jquery", "sweetalert2", "bootstrap", "bootstrap-treeview", "clipboard",
          "fortawesome__fontawesome-free", "stacktrace-js", "datatables", "firebase",
          "firebaseui", "ua-parser-js", "blueimp-load-image", "emojionearea")
      .build();
  public static final File PROBLEM_DIR =
      new File(GoWebAppConfig.WEB_APP_CONFIG.getAppRootDirectory(), "problem");
  public static final File CURRENT_ICON_DIR =
      new File(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory(), "img/icon");
  public static final File UPLOADED_ICON_DIR =
      new File(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory(), "img/icon-uploaded");
}