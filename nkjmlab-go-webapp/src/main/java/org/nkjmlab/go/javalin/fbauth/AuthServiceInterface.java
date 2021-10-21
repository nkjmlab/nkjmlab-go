package org.nkjmlab.go.javalin.fbauth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.nkjmlab.go.javalin.model.json.UserJson;

public interface AuthServiceInterface {

  boolean isSigninToFirebase();

  UserJson signinWithFirebase(String idToken, String seatId);

  boolean signoutFromFirebase();

  boolean signupAsGuest(String userId, String userName, String seatId);

  boolean registerAttendance(String userId, String seatId);

  static Set<String> getDeclaredMethodNames() {
    return Arrays.stream(AuthServiceInterface.class.getDeclaredMethods()).map(m -> m.getName())
        .collect(Collectors.toSet());
  }


}
