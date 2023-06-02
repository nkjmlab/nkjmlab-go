package org.nkjmlab.go.javalin.jsonrpc;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.GoTables;
import org.nkjmlab.go.javalin.GoWebAppConfig;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.go.javalin.model.relation.VotesTable.Vote;
import org.nkjmlab.go.javalin.model.relation.VotesTable.VoteResult;
import org.nkjmlab.go.javalin.util.CurrentTimeMillisIdGenerator;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.result.RowMap;
import org.nkjmlab.util.java.Base64Utils;
import org.nkjmlab.util.java.json.JsonMapper;
import org.nkjmlab.util.java.lang.ParameterizedStringFormatter;
import org.nkjmlab.util.javax.imageio.ImageIoUtils;
import org.threeten.bp.Instant;

public class GoJsonRpcService implements GoJsonRpcServiceInterface {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();
  private final WebsocketSessionsManager webSocketManager;

  private final GoTables goTables;

  private static final JsonMapper mapper = GoApplication.getDefaultJacksonMapper();


  public GoJsonRpcService(WebsocketSessionsManager webSocketManager, GoTables goTables) {
    this.webSocketManager = webSocketManager;
    this.goTables = goTables;
  }

  @Override
  public void sendGameState(String gameId, GameState json) {
    webSocketManager.sendGameState(gameId, json);
  }


  @Override
  public void sendGlobalMessage(String userId, String message) {
    webSocketManager.sendGlobalMessage(message);
  }


  @Override
  public void syncGameState(int sessionId, String gameId, String userId) {
    webSocketManager.updateSession(sessionId, gameId, userId);
    webSocketManager.sendLatestGameStateToSessions(gameId);
  }

  @Override
  public void newGame(String gameId, GameState json) {
    webSocketManager.newGame(gameId, json);
  }

  @Override
  public ProblemJson loadProblem(String gameId, long problemId) {
    return webSocketManager.loadProblem(gameId, problemId);
  }

  @Override
  public ProblemJson getProblem(long problemId) {
    Problem p = goTables.problemsTable.selectByPrimaryKey(problemId);
    return p == null ? new ProblemJson(-1) : ProblemJson.createFrom(p);
  }

  @Override
  public void goBack(String gameId, GameState json) {
    webSocketManager.goBack(gameId);
  }

  @Override
  public ProblemJson saveProblem(String gameId, long problemId, String groupId, String name,
      String message) {
    Problem newP = createNewProblem(gameId, problemId, groupId, name, message);
    goTables.problemsTable.merge(newP);
    goTables.problemsTable.clearProblemsJson();
    ProblemJson problemJson = ProblemJson.createFrom(newP);
    saveProblemJsonToFile(problemJson);
    return problemJson;
  }

  private final CurrentTimeMillisIdGenerator problemIdGenerator = new CurrentTimeMillisIdGenerator();

  private Problem createNewProblem(String gameId, long problemId, String groupId, String name,
      String message) {
    Problem prevP = goTables.problemsTable.selectByPrimaryKey(problemId);
    GameState currentState = goTables.gameStatesTables.readLatestGameState(gameId);
    if (prevP != null) {
      autoBackupProblemJsonToFile(ProblemJson.createFrom(prevP));
    }
    return new Problem(
        prevP != null ? prevP.id() : (problemId == -1 ? problemIdGenerator.getNewId() : problemId),
        LocalDateTime.now(), groupId, name, mapper.toJson(currentState.cells()),
        mapper.toJson(currentState.symbols()), mapper.toJson(currentState.agehama()),
        mapper.toJson(currentState.handHistory()), message == null ? "" : message);
  }

  private void autoBackupProblemJsonToFile(ProblemJson p) {
    File bkupDir = getProblemAutoBackupDir(p.groupId());
    File o = new File(bkupDir, Instant.now().toEpochMilli() + "-copy-" + p.name() + ".json");
    mapper.toJsonAndWrite(p, o, true);
  }

  private void saveProblemJsonToFile(ProblemJson p) {
    File problemGroupDir = getProblemDir(p.groupId());
    File o = new File(problemGroupDir, p.name() + ".json");
    mapper.toJsonAndWrite(p, o, true);
    log.info("Problem {} - {} is saved to {}", p.groupId(), p.name(), o);

  }

  private File getProblemDir(String groupId) {
    File dir = new File(GoWebAppConfig.PROBLEM_DIR, groupId);
    dir.mkdirs();
    return dir;
  }

  private File getProblemAutoBackupDir(String groupId) {
    File dir =
        new File(new File(GoWebAppConfig.WEB_APP_CONFIG.getAppRootDirectory(), "problem-auto-bkup"),
            groupId);
    dir.mkdirs();
    return dir;

  }

  @Override
  public void deleteProblem(long problemId) {
    Problem p = goTables.problemsTable.selectByPrimaryKey(problemId);
    if (p == null) {
      return;
    }
    autoBackupProblemJsonToFile(ProblemJson.createFrom(p));

    goTables.problemsTable.delete(p);
    goTables.problemsTable.clearProblemsJson();
  }

  @Override
  public ProblemJson readProblem(long problemId) {
    Problem p = goTables.problemsTable.selectByPrimaryKey(problemId);
    if (p != null) {
      return ProblemJson.createFrom(p);
    }
    return new ProblemJson(-1);
  }


  @Override
  public String getNextUser(String currentGameId) {
    String nextUserId = goTables.loginsTable.getNextLoginUserId(goTables.usersTable, currentGameId);
    return nextUserId;
  }

  @Override
  public String getPrevUser(String currentGameId) {
    String nextUserId = goTables.loginsTable.getPrevLoginUserId(goTables.usersTable, currentGameId);
    return nextUserId;
  }


  @Override
  public String getNextQuestion(String currentGameId) {
    String nextQuestionGameId = goTables.handsUpTable.getNextQuestion(currentGameId);
    return nextQuestionGameId;
  }


  @Override
  public UserJson getUser(String userId) {
    User u = goTables.usersTable.selectByPrimaryKey(userId);
    if (u == null) {
      UserJson uj = new UserJson(userId);
      return uj;
    }
    UserJson uj = new UserJson(u, goTables.loginsTable.isAttendance(userId));
    return uj;

  }

  @Override
  public String getNextGame(String currentGameId) {
    List<String> ids = webSocketManager.readActiveGameIdsOrderByGameId();
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
    List<String> ids = webSocketManager.readActiveGameIdsOrderByGameId();
    Collections.reverse(ids);
    return getNextId(ids, currentGameId);
  }


  @Override
  public void enterWaitingRoom(String userId) {
    try {
      User u = goTables.usersTable.selectByPrimaryKey(userId);
      if (u == null) {
        log.error("userId {} is not found.", userId);
        return;
      }
      goTables.usersTable.update(u);

      MatchingRequest matchingReq = MatchingRequest.createUnpaired(u);
      if (!goTables.matchingRequestsTable.exists(matchingReq)) {
        goTables.matchingRequestsTable.insert(matchingReq);
      } else {
        MatchingRequest m = goTables.matchingRequestsTable.selectByPrimaryKey(userId);
        goTables.matchingRequestsTable.update(m);
      }
      webSocketManager.sendUpdateWaitingRequestStatus(Set.of(userId));
    } catch (Exception e) {
      log.error("maching request for {} failed", userId);
      log.error(e, e);
    }
  }


  @Override
  public void exitWaitingRoom(String userId) {
    goTables.matchingRequestsTable.deleteByPrimaryKey(userId);
    webSocketManager.sendUpdateWaitingRequestStatus(Set.of(userId));
  }

  @Override
  public File uploadImage(String userId, String base64EncodedImage) {
    try {
      {
        File outputFile = new File(GoWebAppConfig.UPLOADED_ICON_DIR, userId + ".png");
        outputFile.mkdirs();
        ImageIoUtils.write(Base64Utils.decodeToImage(base64EncodedImage, "png"), "png", outputFile);
      }
      File outputFile = new File(GoWebAppConfig.CURRENT_ICON_DIR, userId + ".png");
      outputFile.mkdirs();
      ImageIoUtils.write(Base64Utils.decodeToImage(base64EncodedImage, "png"), "png", outputFile);
      log.debug("Icon is uploaded={}", outputFile);
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
    goTables.votesTable
        .merge(new Vote(userId, problemId, vote, voteId, gameId, LocalDateTime.now()));
  }

  @Override
  public List<VoteResult> getVoteResult(long problemId, String gameId) {
    List<VoteResult> ret = goTables.votesTable.readVoteResults(problemId, gameId);
    return ret;
  }

  @Override
  public void handUp(String gameId, boolean handUp, String message) {
    if (handUp) {
      {
        HandUp h = goTables.handsUpTable.selectByPrimaryKey(gameId);
        if (h == null) {
          goTables.handsUpTable.insert(new HandUp(gameId, LocalDateTime.now(), message));
        } else {
          goTables.handsUpTable
              .update(new HandUp(h.gameId(), h.createdAt(), h.message() + "<br>" + message));
        }
      }
      webSocketManager.sendHandUp(gameId, handUp, goTables.handsUpTable.readOrder(gameId));
    } else {
      goTables.handsUpTable.deleteByPrimaryKey(gameId);
      webSocketManager.sendHandDown(gameId);

      goTables.handsUpTable.readAllGameIds().stream().forEach(handupGameId -> webSocketManager
          .sendHandUpOrder(handupGameId, goTables.handsUpTable.readOrder(handupGameId)));
    }

  }

  @Override
  public int registerRecord(String userId, String opponentUserId, String jadge, String memo) {
    int rank = goTables.gameRecordsTable.registerRecordAndGetRank(goTables.usersTable, userId,
        opponentUserId, jadge, memo);

    User u = goTables.usersTable.selectByPrimaryKey(userId);
    if (u.rank() != rank) {
      goTables.usersTable.updateByPrimaryKey(RowMap.of("rank", rank), u.userId());
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
    try {
      GameState gs = goTables.gameStatesTables.readLatestGameState(gameId);
      User bp = goTables.usersTable.selectByPrimaryKey(gs.blackPlayerId());
      User wp = goTables.usersTable.selectByPrimaryKey(gs.whitePlayerId());
      int diff = Math.abs(wp.rank() - bp.rank());
      int ro = gs.cells()[0].length;
      String roCol = ro == 19 ? "lg" : "sm";

      String s1 = start.get(roCol).get(Math.min(diff, 5));
      String s2 = midFlow.get(roCol).get(Math.min(diff, 5));
      Object[] params = {bp.userId(), bp.userName(), bp.rank(), wp.userId(), wp.userName(),
          wp.rank(), diff, ro, s1, s2};

      String msg = ParameterizedStringFormatter.DEFAULT.format(
          (String) "{} ({}，{}級) vs {} ({}，{}級): {}級差，{}路 <br><span class='badge badge-info'>はじめから</span> {}, <span class='badge badge-info'>棋譜並べから</span> {} <br>",
          params);
      return msg;
    } catch (Exception e) {
      log.error(e);
      return "";
    }
  }

}
