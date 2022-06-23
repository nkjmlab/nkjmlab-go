package org.nkjmlab.go.javalin.jsonrpc;

import java.io.File;
import java.util.List;
import org.nkjmlab.go.javalin.model.json.ProblemJson;
import org.nkjmlab.go.javalin.model.json.UserJson;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameStateJson;
import org.nkjmlab.go.javalin.model.relation.VotesTable.VoteResult;

public interface GoJsonRpcServiceInterface {

  void vote(String gameId, String userId, long problemId, String vote, String voteId);

  List<VoteResult> getVoteResult(long problemId, String gameId);

  void syncGameState(int sessionId, String gameId, String userId);

  UserJson getUser(String userId);

  String getNextUser(String currentGameId);

  String getPrevUser(String currentGameId);

  String getNextGame(String currentGameId);

  String getPrevGame(String currentGameId);

  String getNextQuestion(String currentGameId);


  boolean sendLog(String logLevel, String location, String msg, String options);

  void newGame(String gameId, GameStateJson json);

  ProblemJson loadProblem(String gameId, long problemId);

  ProblemJson getProblem(long problemId);

  void goBack(String gameId, GameStateJson json);

  void sendGameState(String gameId, GameStateJson json);

  ProblemJson saveProblem(String gameId, long problemId, String groupId, String name,
      String message);

  void deleteProblem(long problemId);

  ProblemJson readProblem(long problemId);

  void enterWaitingRoom(String userId);

  void exitWaitingRoom(String userId);

  File uploadImage(String userId, String base64EncodedImage);

  void handUp(String gameId, boolean handUp, String msg);

  void sendGlobalMessage(String userId, String message);

  int registerRecord(String userId, String opponentUserId, String jadge, String memo);

  String getKomi(String gameId);


}
