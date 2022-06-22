package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;

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
