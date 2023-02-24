package org.nkjmlab.go.javalin.fbauth;

import java.util.Optional;
import org.nkjmlab.util.jakarta.servlet.UserSession;
import jakarta.servlet.http.HttpSession;

public class FirebaseUserSession extends UserSession {

  private static final String EMAIL = "EMAIL";
  private static final String ID_TOKEN = "ID_TOKEN";

  public static FirebaseUserSession wrap(HttpSession session) {
    return new FirebaseUserSession(session);
  }

  protected FirebaseUserSession(HttpSession session) {
    super(session);
  }


  public Optional<String> getEmail() {
    return getAttribute(EMAIL).map(o -> o.toString());
  }

  public Optional<String> getIdToken() {
    return getAttribute(ID_TOKEN).map(o -> o.toString());
  }


  public boolean isSetUserId() {
    return getUserId().isPresent();
  }

  public boolean isSigninFirebase() {
    return getIdToken().isPresent();
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
