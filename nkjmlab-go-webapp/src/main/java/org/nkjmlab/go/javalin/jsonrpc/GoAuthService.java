package org.nkjmlab.go.javalin.jsonrpc;

import java.util.Optional;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.h2.datasource.H2DataSourceFactory;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.util.firebase.auth.FirebaseAuthHandler;
import com.google.firebase.auth.FirebaseToken;

public class GoAuthService {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final FirebaseAuthHandler firebaseService;
  private final SigninSessionsTable signinSessionsTable = new SigninSessionsTable();
  private final UsersTable usersTable;

  public GoAuthService(UsersTable usersTable, FirebaseAuthHandler firebaseService) {
    this.usersTable = usersTable;
    this.firebaseService = firebaseService;
    signinSessionsTable.createTableIfNotExists().createIndexesIfNotExists();
  }

  public Optional<SigninSession> signinWithFirebase(String idToken, String sessionId) {
    Optional<FirebaseToken> opt = firebaseService.isAcceptableIdToken(idToken);
    User u = opt.map(fs -> usersTable.readByEmail(fs.getEmail())).get();
    if (u == null) {
      log.error("invalid mail {}", opt.get().getEmail());
      return Optional.empty();
    }

    SigninSession ret = opt.map(fs -> new SigninSession(sessionId, u.userId())).get();
    signin(ret);
    return Optional.of(ret);
  }

  public SigninSession signin(String sessionId, String userId) {
    SigninSession s = new SigninSession(sessionId, userId);
    signin(s);
    return s;
  }


  private SigninSession signin(SigninSession signinSession) {
    signinSessionsTable.merge(signinSession);
    return signinSession;
  }

  public void signout(String sessionId) {
    signinSessionsTable.deleteByPrimaryKey(sessionId);

  }

  public boolean isSignin(String sessionId) {
    return signinSessionsTable.exists(sessionId);
  }


  public Optional<SigninSession> toSigninSession(String sessionId) {
    return isSignin(sessionId) ? Optional.of(signinSessionsTable.selectByPrimaryKey(sessionId))
        : Optional.empty();
  }

  @OrmRecord
  public record SigninSession(@PrimaryKey String sessionId, String userId) {

  }

  private static class SigninSessionsTable extends H2BasicTable<SigninSession> {

    public SigninSessionsTable() {
      super(Sorm.create(H2DataSourceFactory.createTemporalInMemoryDataSource()),
          SigninSession.class);
    }

  }



}
