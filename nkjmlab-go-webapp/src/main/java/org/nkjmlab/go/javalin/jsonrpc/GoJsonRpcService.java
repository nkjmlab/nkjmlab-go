package org.nkjmlab.go.javalin.jsonrpc;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.GoApplication.GoWebAppConfig;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.problem.ProblemTextToJsonConverter;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable.Vote;
import org.nkjmlab.go.javalin.model.relation.VotesTable.VoteResult;
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

  private final GameStatesTables gameStatesTables;
  private final ProblemsTable problemsTable;
  private final UsersTable usersTable;
  private final LoginsTable loginsTable;
  private final MatchingRequestsTable matchingRequestsTable;
  private final VotesTable votesTable;
  private final WebsocketSessionsManager websocketManager;
  private final HandUpsTable handsUpTable;
  private final GameRecordsTable gameRecordsTable;

  private static final JsonMapper mapper = GoApplication.getDefaultJacksonMapper();

  public GoJsonRpcService(WebsocketSessionsManager wsManager, GameStatesTables gameStatesTables,
      ProblemsTable problemsTable, UsersTable usersTable, LoginsTable loginsTable,
      MatchingRequestsTable matchingRequestsTable, VotesTable votesTable, HandUpsTable handsUpTable,
      GameRecordsTable gameRecordsTable) {
    this.gameStatesTables = gameStatesTables;
    this.problemsTable = problemsTable;
    this.usersTable = usersTable;
    this.loginsTable = loginsTable;
    this.matchingRequestsTable = matchingRequestsTable;
    this.websocketManager = wsManager;
    this.votesTable = votesTable;
    this.handsUpTable = handsUpTable;
    this.gameRecordsTable = gameRecordsTable;

  }

  @Override
  public void sendGameState(String gameId, GameState json) {
    websocketManager.sendGameState(gameId, json);
  }


  @Override
  public void sendGlobalMessage(String userId, String message) {
    websocketManager.sendGlobalMessage(message);
  }


  @Override
  public void syncGameState(int sessionId, String gameId, String userId) {
    websocketManager.updateSession(sessionId, gameId, userId);
    websocketManager.sendLatestGameStateToSessions(gameId);
  }

  @Override
  public void newGame(String gameId, GameState json) {
    websocketManager.newGame(gameId, json);
  }

  @Override
  public ProblemJson loadProblem(String gameId, long problemId) {
    return websocketManager.loadProblem(gameId, problemId);
  }

  @Override
  public ProblemJson getProblem(long problemId) {
    Problem p = problemsTable.selectByPrimaryKey(problemId);
    return p == null ? new ProblemJson(-1) : ProblemJson.createFrom(p);
  }

  @Override
  public void goBack(String gameId, GameState json) {
    websocketManager.goBack(gameId);
  }

  @Override
  public ProblemJson saveProblem(String gameId, long problemId, String groupId, String name,
      String message) {
    Problem newP = createNewProblem(gameId, problemId, groupId, name, message);
    problemsTable.merge(newP);
    problemsTable.clearProblemsJson();
    ProblemJson problemJson = ProblemJson.createFrom(newP);
    saveProblemJsonToFile(problemJson);
    return problemJson;
  }

  private Problem createNewProblem(String gameId, long problemId, String groupId, String name,
      String message) {
    Problem prevP = problemsTable.selectByPrimaryKey(problemId);
    GameState currentState = gameStatesTables.readLatestGameState(gameId);
    if (prevP != null) {
      autoBackupProblemJsonToFile(ProblemJson.createFrom(prevP));
    }
    return new Problem(
        prevP != null ? prevP.id()
            : (problemId == -1 ? ProblemTextToJsonConverter.getNewId() : problemId),
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
    Problem p = problemsTable.selectByPrimaryKey(problemId);
    if (p == null) {
      return;
    }
    autoBackupProblemJsonToFile(ProblemJson.createFrom(p));

    problemsTable.delete(p);
    problemsTable.clearProblemsJson();
  }

  @Override
  public ProblemJson readProblem(long problemId) {
    Problem p = problemsTable.selectByPrimaryKey(problemId);
    if (p != null) {
      return ProblemJson.createFrom(p);
    }
    return new ProblemJson(-1);
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
    User u = usersTable.selectByPrimaryKey(userId);
    if (u == null) {
      UserJson uj = new UserJson(userId);
      return uj;
    }
    UserJson uj = new UserJson(u, loginsTable.isAttendance(userId));
    return uj;

  }

  @Override
  public String getNextGame(String currentGameId) {
    List<String> ids = websocketManager.readActiveGameIdsOrderByGameId();
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
    List<String> ids = websocketManager.readActiveGameIdsOrderByGameId();
    Collections.reverse(ids);
    return getNextId(ids, currentGameId);
  }


  @Override
  public void enterWaitingRoom(String userId) {
    try {
      User u = usersTable.selectByPrimaryKey(userId);
      if (u == null) {
        log.error("userId {} is not found.", userId);
        return;
      }
      usersTable.update(u);

      MatchingRequest matchingReq = MatchingRequest.createUnpaired(u);
      if (!matchingRequestsTable.exists(matchingReq)) {
        matchingRequestsTable.insert(matchingReq);
      } else {
        MatchingRequest m = matchingRequestsTable.selectByPrimaryKey(userId);
        matchingRequestsTable.update(m);
      }
      websocketManager.sendUpdateWaitingRequestStatus(Set.of(userId));
    } catch (Exception e) {
      log.error("maching request for {} failed", userId);
      log.error(e, e);
    }
  }


  @Override
  public void exitWaitingRoom(String userId) {
    matchingRequestsTable.deleteByPrimaryKey(userId);
    websocketManager.sendUpdateWaitingRequestStatus(Set.of(userId));
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
    votesTable.merge(new Vote(userId, problemId, vote, voteId, gameId, LocalDateTime.now()));
  }

  @Override
  public List<VoteResult> getVoteResult(long problemId, String gameId) {
    List<VoteResult> ret = votesTable.readVoteResults(problemId, gameId);
    return ret;
  }

  @Override
  public void handUp(String gameId, boolean handUp, String message) {
    if (handUp) {
      {
        HandUp h = handsUpTable.selectByPrimaryKey(gameId);
        if (h == null) {
          handsUpTable.insert(new HandUp(gameId, LocalDateTime.now(), message));
        } else {
          handsUpTable
              .update(new HandUp(h.gameId(), h.createdAt(), h.message() + "<br>" + message));
        }
      }
      websocketManager.sendHandUp(gameId, handUp, handsUpTable.readOrder(gameId));
    } else {
      handsUpTable.deleteByPrimaryKey(gameId);
      websocketManager.sendHandDown(gameId);

      handsUpTable.readAllGameIds().stream().forEach(handupGameId -> websocketManager
          .sendHandUpOrder(handupGameId, handsUpTable.readOrder(handupGameId)));
    }

  }

  @Override
  public int registerRecord(String userId, String opponentUserId, String jadge, String memo) {
    int rank =
        gameRecordsTable.registerRecordAndGetRank(usersTable, userId, opponentUserId, jadge, memo);

    User u = usersTable.selectByPrimaryKey(userId);
    if (u.rank() != rank) {
      usersTable.updateByPrimaryKey(RowMap.of("rank", rank), u.userId());
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
      GameState gs = gameStatesTables.readLatestGameState(gameId);
      User bp = usersTable.selectByPrimaryKey(gs.blackPlayerId());
      User wp = usersTable.selectByPrimaryKey(gs.whitePlayerId());
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
