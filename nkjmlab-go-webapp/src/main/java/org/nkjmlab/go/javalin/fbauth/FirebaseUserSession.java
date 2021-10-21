package org.nkjmlab.go.javalin.fbauth;

import javax.servlet.http.HttpSession;
import org.nkjmlab.util.websrv.UserSession;

public class FirebaseUserSession extends UserSession {

  private static final String EMAIL = "email";
  private static final String ID_TOKEN = "idToken";

  public static FirebaseUserSession wrap(HttpSession session) {
    return new FirebaseUserSession(session);
  }

  protected FirebaseUserSession(HttpSession session) {
    super(session);
  }


  public String getEmail() {
    return getAttribute(EMAIL) == null ? null : getAttribute(EMAIL).toString();
  }

  public String getIdToken() {
    return getAttribute(ID_TOKEN) == null ? null : getAttribute(ID_TOKEN).toString();
  }


  public boolean isSetUserId() {
    return getUserId() != null;
  }

  public boolean isSigninFirebase() {
    return getIdToken() != null;
  }

  private void setEmail(String email) {
    setAttribute(EMAIL, email);
  }

  private void setIdToken(String idToken) {
    setAttribute(ID_TOKEN, idToken);
  }

  public void signinFirebase(String idToken, String email) {
    setIdToken(idToken);
    setEmail(email);
    setMaxInactiveInterval(10 * 60 * 60);
  }



}
