package org.nkjmlab.go.javalin.fbauth;

import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;

public interface AuthServiceInterface {

  boolean isSigninToFirebase();

  UserJson signinWithFirebase(String idToken, String seatId);

  boolean signoutFromFirebase();

  boolean signupAsGuest(String userId, String userName, String seatId);

  UserJson signinWithoutFirebase(String userId, String passsword, String seatId);

  boolean registerAttendance(String userId, String seatId);


}
