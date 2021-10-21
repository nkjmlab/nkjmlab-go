package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;

public class LoginJson {

  private Login login;
  private User user;

  public LoginJson() {}

  public LoginJson(Login entry, User user) {
    this.login = entry;
    this.user = user;
  }

  public Login getLogin() {
    return login;
  }


  public User getUser() {
    return user;
  }


}
