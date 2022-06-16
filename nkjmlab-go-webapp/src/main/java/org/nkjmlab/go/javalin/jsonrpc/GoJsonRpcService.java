package org.nkjmlab.go.javalin.jsonrpc;

import static org.nkjmlab.go.javalin.GoApplication.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.nkjmlab.go.javalin.model.json.GameStateJson;
import org.nkjmlab.go.javalin.model.json.ProblemJson;
import org.nkjmlab.go.javalin.model.json.UserJson;
import org.nkjmlab.go.javalin.model.json.VoteResult;
import org.nkjmlab.go.javalin.model.problem.ProblemFactory;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandsUpTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
import org.nkjmlab.go.javalin.model.row.HandUp;
import org.nkjmlab.go.javalin.model.row.MatchingRequest;
import org.nkjmlab.go.javalin.model.row.Problem;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.go.javalin.model.row.Vote;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.go.javalin.websocket.WebsoketSessionsTable;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.Base64Utils;
import org.nkjmlab.util.java.json.JsonMapper;
import org.nkjmlab.util.java.lang.ParameterizedStringUtils;
import org.nkjmlab.util.javax.imageio.ImageIoUtils;

public class GoJsonRpcService implements GoJsonRpcServiceInterface {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final GameStatesTables gameStatesTables;
  private final ProblemsTable problemsTable;
  private final UsersTable usersTable;
  private final LoginsTable loginsTable;
  private final MatchingRequestsTable matchingRequestsTable;
  private final VotesTable votesTable;
  private final WebsocketSessionsManager wsManager;
  private final WebsoketSessionsTable websoketSessionsTable;
  private final HandsUpTable handsUpTable;
  private final GameRecordsTable gameRecordsTable;

  private static final JsonMapper mapper = JacksonMapper.getIgnoreUnknownPropertiesMapper();


  public GoJsonRpcService(WebsocketSessionsManager wsManager, GameStatesTables gameStatesTables,
      ProblemsTable problemsTable, UsersTable usersTable, LoginsTable loginsTable,
      MatchingRequestsTable matchingRequestsTable, VotesTable votesTable, HandsUpTable handsUpTable,
      WebsoketSessionsTable websoketSessionsTable, GameRecordsTable gameRecordsTable) {
    this.gameStatesTables = gameStatesTables;
    this.problemsTable = problemsTable;
    this.usersTable = usersTable;
    this.loginsTable = loginsTable;
    this.matchingRequestsTable = matchingRequestsTable;
    this.wsManager = wsManager;
    this.votesTable = votesTable;
    this.handsUpTable = handsUpTable;
    this.websoketSessionsTable = websoketSessionsTable;
    this.gameRecordsTable = gameRecordsTable;

  }

  @Override
  public void sendGameState(String gameId, GameStateJson json) {
    wsManager.sendGameState(gameId, json);
  }


  @Override
  public void sendGlobalMessage(String userId, String message) {
    wsManager.sendGlobalMessage(message);
  }


  @Override
  public void syncGameState(int sessionId, String gameId, String userId) {
    wsManager.updateSession(sessionId, gameId, userId);
    wsManager.sendLatestGameStateToSessions(gameId);
  }

  @Override
  public void newGame(String gameId, GameStateJson json) {
    wsManager.newGame(gameId, json);
  }

  @Override
  public ProblemJson loadProblem(String gameId, long problemId) {
    return wsManager.loadProblem(gameId, problemId);
  }

  @Override
  public ProblemJson getProblem(long problemId) {
    Problem p = problemsTable.readByPrimaryKey(problemId);
    if (p == null) {
      return new ProblemJson();
    }
    return ProblemJson.createFrom(p);
  }

  @Override
  public void goBack(String gameId, GameStateJson json) {
    wsManager.goBack(gameId);
  }

  @Override
  public ProblemJson saveProblem(String gameId, long problemId, String groupId, String name,
      String message) {
    Problem p = problemsTable.readByPrimaryKey(problemId);
    GameStateJson currentState = gameStatesTables.readLatestGameStateJson(gameId);
    if (p != null) {
      autoBackupProblemJsonToFile(ProblemJson.createFrom(p));
      p.setAgehama(mapper.toJson(currentState.getAgehama()));
      p.setCells(mapper.toJson(currentState.getCells()));
      p.setCreatedAt(LocalDateTime.now());
      p.setHandHistory(mapper.toJson(currentState.getHandHistory()));
      p.setSymbols(mapper.toJson(currentState.getSymbols()));
      p.setName(name);
      p.setGroupId(groupId);
      p.setMessage(message);
      problemsTable.merge(p);
    } else {
      p = new Problem(problemId == -1 ? ProblemFactory.getNewId() : problemId, LocalDateTime.now(),
          groupId, name, mapper.toJson(currentState.getCells()),
          mapper.toJson(currentState.getSymbols()), message == null ? "" : message,
          mapper.toJson(currentState.getHandHistory()), mapper.toJson(currentState.getAgehama()));
      problemsTable.insert(p);
    }
    problemsTable.clearProblemsJson();
    ProblemJson problemJson = ProblemJson.createFrom(p);
    saveProblemJsonToFile(problemJson);
    return problemJson;
  }

  private void autoBackupProblemJsonToFile(ProblemJson p) {
    File bkupDir = getProblemAutoBackupDir(p.getGroupId());
    File o = new File(bkupDir, new Date().getTime() + "-copy-" + p.getName() + ".json");
    mapper.toJsonAndWrite(p, o, true);
  }

  private void saveProblemJsonToFile(ProblemJson p) {
    File problemGroupDir = getProblemDir(p.getGroupId());
    File o = new File(problemGroupDir, p.getName() + ".json");
    mapper.toJsonAndWrite(p, o, true);
    log.info("Problep {} - {} is saved to {}", p.getGroupId(), p.getName(), o);

  }

  private File getProblemDir(String groupId) {
    File dir = new File(PROBLEM_DIR, groupId);
    dir.mkdirs();
    return dir;
  }

  private File getProblemAutoBackupDir(String groupId) {
    File dir = new File(PROBLEM_BACKUP_DIR, groupId);
    dir.mkdirs();
    return dir;

  }

  @Override
  public void deleteProblem(long problemId) {
    Problem p = problemsTable.readByPrimaryKey(problemId);
    if (p == null) {
      return;
    }
    autoBackupProblemJsonToFile(ProblemJson.createFrom(p));

    problemsTable.delete(p);
    problemsTable.clearProblemsJson();
  }

  @Override
  public ProblemJson readProblem(long problemId) {
    Problem p = problemsTable.readByPrimaryKey(problemId);
    if (p != null) {
      return ProblemJson.createFrom(p);
    }
    ProblemJson pj = new ProblemJson();
    pj.setProblemId(-1);
    return pj;
  }


  @Override
  public String getNextUser(String currentGameId) {
    String nextUserId = loginsTable.getNextLoginUserId(usersTable, currentGameId);
    return nextUserId;
  }

  @Override
  public String getPrevUser(String currentGameId) {
    String nextUserId = loginsTable.getPrevLoginUserId(usersTable, currentGameId);
    return nextUserId;
  }


  @Override
  public String getNextQuestion(String currentGameId) {
    String nextQuestionGameId = handsUpTable.getNextQuestion(currentGameId);
    return nextQuestionGameId;
  }


  @Override
  public UserJson getUser(String userId) {
    User u = usersTable.readByPrimaryKey(userId);
    if (u == null) {
      UserJson uj = new UserJson();
      uj.setUserId(userId);
      return uj;
    }
    UserJson uj = new UserJson(u);
    uj.attendance = loginsTable.isAttendance(userId);
    return uj;

  }

  @Override
  public String getNextGame(String currentGameId) {
    List<String> ids = websoketSessionsTable.readActiveGameIdsOrderByGameId(usersTable);
    return getNextId(ids, currentGameId);
  }


  private String getNextId(List<String> ids, String currentGameId) {
    int index = ids.indexOf(currentGameId);
    if (index == -1) {
      return currentGameId;
    } else {
      return ids.get((index + 1) % ids.size());
    }
  }


  @Override
  public String getPrevGame(String currentGameId) {
    List<String> ids = websoketSessionsTable.readActiveGameIdsOrderByGameId(usersTable);
    Collections.reverse(ids);
    return getNextId(ids, currentGameId);
  }


  @Override
  public void enterWaitingRoom(String userId) {
    try {
      User u = usersTable.readByPrimaryKey(userId);
      if (u == null) {
        log.error("userId {} is not found.", userId);
        return;
      }
      usersTable.update(u);

      MatchingRequest matchingReq = new MatchingRequest(u, LocalDateTime.now());
      if (!matchingRequestsTable.exists(matchingReq)) {
        matchingRequestsTable.insert(matchingReq);
      } else {
        MatchingRequest m = matchingRequestsTable.readByPrimaryKey(userId);
        matchingRequestsTable.update(m);
      }
      wsManager.sendUpdateWaitingRequestStatus(List.of(userId));
    } catch (Exception e) {
      log.error("maching request for {} failed", userId);
      log.error(e, e);
    }
  }


  @Override
  public void exitWaitingRoom(String userId) {
    // logger.debug("{} exited from waiting room.", userId);
    matchingRequestsTable.deleteIfExists(new MatchingRequest(userId));
    wsManager.sendUpdateWaitingRequestStatus(List.of(userId));
  }

  @Override
  public File uploadImage(String userId, String base64EncodedImage) {
    try {
      {
        File outputFile = new File(UPLOADED_ICON_DIR, userId + ".png");
        outputFile.mkdirs();
        ImageIoUtils.write(Base64Utils.decodeToImage(base64EncodedImage, "png"), "png", outputFile);
      }
      File outputFile = new File(CURRENT_ICON_DIR, userId + ".png");
      outputFile.mkdirs();
      ImageIoUtils.write(Base64Utils.decodeToImage(base64EncodedImage, "png"), "png", outputFile);
      log.info("Icon is uploaded={}", outputFile);
      return outputFile;
    } catch (Exception e) {
      log.error(e, e);
      return null;
    }
  }

  @Override
  public boolean sendLog(String logLevel, String location, String msg, String options) {
    log.error("{},{},{},{}", logLevel, location, msg, options);
    return true;
  }

  @Override
  public void vote(String gameId, String userId, long problemId, String vote, String voteId) {
    votesTable.merge(new Vote(userId, problemId, vote, voteId, gameId));
  }

  @Override
  public List<VoteResult> getVoteResult(long problemId, String gameId) {
    return votesTable.readVoteResults(problemId, gameId);
  }

  @Override
  public void handUp(String gameId, boolean handUp, String message) {
    if (handUp) {
      HandUp h = handsUpTable.readByPrimaryKey(gameId);
      if (h == null) {
        handsUpTable.insert(new HandUp(gameId, LocalDateTime.now(), message));
      } else {
        h.setMessage(h.getMessage() + "<br>" + message);
        handsUpTable.update(h);
      }
      wsManager.sendHandUp(gameId, handUp, handsUpTable.readOrder(gameId));
    } else {
      handsUpTable.delete(HandUp.createAsPrimaryKey(gameId));
      wsManager.sendHandDown(gameId);

      handsUpTable.readAllGameIds().stream().forEach(handupGameId -> wsManager
          .sendHandUpOrder(handupGameId, handsUpTable.readOrder(handupGameId)));
    }

  }

  @Override
  public int registerRecord(String userId, String opponentUserId, String jadge, String memo) {
    // logger.debug("{},{},{},{}", userId, opponentUserId, jadge, memo);
    int rank =
        gameRecordsTable.registerRecordAndGetRank(usersTable, userId, opponentUserId, jadge, memo);

    User u = usersTable.readByPrimaryKey(userId);
    if (u.getRank() != rank) {
      u.setRank(rank);
      usersTable.update(u);
      return rank;
    }
    return -1;
  }

  private static final Map<String, List<String>> start = Map.of("sm",
      List.of("互先 黒6目コミ出し", "互先 黒3目コミ出し", "先", "先 白3目コミ出し", "2子", "2子  白3目コミ出し または 3子  黒3目コミ出し"),
      "lg",
      List.of("互先 黒6目コミ出し", "互先 黒3目コミ出し", "先", "先 白3目コミ出し", "2子", "2子  白3目コミ出し または 3子  黒3目コミ出し"));

  private static final Map<String, List<String>> midFlow =
      Map.of("sm", List.of("互先 黒6目コミ出し", "互先 黒3目コミ出し", "先", "先 白3目コミ出し", "先 白6目コミ出し", "先 白9目コミ出し"),
          "lg", List.of("互先 黒6目半コミ出し", "先 コミなし", "先 白6目半コミ出し", "先 白13目コミ出し", "先 白19目半コミ出し",
              "先 白26目コミ出し 以下，1級差増えるごとに白のコミ出しを6目半増やす"));

  @Override
  public String getKomi(String gameId) {
    String msg = "";
    try {
      GameStateJson gs = gameStatesTables.readLatestGameStateJson(gameId);
      User bp = usersTable.readByPrimaryKey(gs.getBlackPlayerId());
      User wp = usersTable.readByPrimaryKey(gs.getWhitePlayerId());
      int diff = Math.abs(wp.getRank() - bp.getRank());
      int ro = gs.getCells()[0].length;
      String roCol = ro == 19 ? "lg" : "sm";

      String s1 = start.get(roCol).get(Math.min(diff, 5));
      String s2 = midFlow.get(roCol).get(Math.min(diff, 5));

      msg = ParameterizedStringUtils.newString(
          "{} ({}，{}級) vs {} ({}，{}級): {}級差，{}路 <br><span class='badge badge-info'>はじめから</span> {}, <span class='badge badge-info'>棋譜並べから</span> {} <br>",
          bp.getUserId(), bp.getUserName(), bp.getRank(), wp.getUserId(), wp.getUserName(),
          wp.getRank(), diff, ro, s1, s2);
    } catch (Exception e) {
      msg = "";
      log.error(e);
    }
    return msg;
  }

}
