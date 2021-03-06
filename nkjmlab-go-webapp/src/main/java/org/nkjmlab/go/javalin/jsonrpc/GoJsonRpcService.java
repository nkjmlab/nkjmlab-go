package org.nkjmlab.go.javalin.jsonrpc;

import static org.nkjmlab.go.javalin.GoApplication.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.problem.ProblemTextToJsonConverter;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameStateJson;
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
import org.nkjmlab.go.javalin.websocket.WebsoketSessionsTable;
import org.nkjmlab.sorm4j.result.RowMap;
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
  private final HandUpsTable handsUpTable;
  private final GameRecordsTable gameRecordsTable;

  private static final JsonMapper mapper = GoApplication.getDefaultJacksonMapper();


  public GoJsonRpcService(WebsocketSessionsManager wsManager, GameStatesTables gameStatesTables,
      ProblemsTable problemsTable, UsersTable usersTable, LoginsTable loginsTable,
      MatchingRequestsTable matchingRequestsTable, VotesTable votesTable, HandUpsTable handsUpTable,
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
    Problem p = problemsTable.selectByPrimaryKey(problemId);
    return p == null ? new ProblemJson(-1) : ProblemJson.createFrom(p);
  }

  @Override
  public void goBack(String gameId, GameStateJson json) {
    wsManager.goBack(gameId);
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
    GameStateJson currentState = gameStatesTables.readLatestGameStateJson(gameId);
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
    File o = new File(bkupDir, new Date().getTime() + "-copy-" + p.name() + ".json");
    mapper.toJsonAndWrite(p, o, true);
  }

  private void saveProblemJsonToFile(ProblemJson p) {
    File problemGroupDir = getProblemDir(p.groupId());
    File o = new File(problemGroupDir, p.name() + ".json");
    mapper.toJsonAndWrite(p, o, true);
    log.info("Problep {} - {} is saved to {}", p.groupId(), p.name(), o);

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
      wsManager.sendUpdateWaitingRequestStatus(Set.of(userId));
    } catch (Exception e) {
      log.error("maching request for {} failed", userId);
      log.error(e, e);
    }
  }


  @Override
  public void exitWaitingRoom(String userId) {
    // logger.debug("{} exited from waiting room.", userId);
    matchingRequestsTable.deleteByPrimaryKey(userId);
    wsManager.sendUpdateWaitingRequestStatus(Set.of(userId));
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
      wsManager.sendHandUp(gameId, handUp, handsUpTable.readOrder(gameId));
    } else {
      handsUpTable.deleteByPrimaryKey(gameId);
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

    User u = usersTable.selectByPrimaryKey(userId);
    if (u.rank() != rank) {
      usersTable.updateByPrimaryKey(RowMap.of("rank", rank), u.userId());
      return rank;
    }
    return -1;
  }

  private static final Map<String, List<String>> start = Map.of("sm",
      List.of("?????? ???6???????????????", "?????? ???3???????????????", "???", "??? ???3???????????????", "2???", "2???  ???3??????????????? ????????? 3???  ???3???????????????"),
      "lg",
      List.of("?????? ???6???????????????", "?????? ???3???????????????", "???", "??? ???3???????????????", "2???", "2???  ???3??????????????? ????????? 3???  ???3???????????????"));

  private static final Map<String, List<String>> midFlow =
      Map.of("sm", List.of("?????? ???6???????????????", "?????? ???3???????????????", "???", "??? ???3???????????????", "??? ???6???????????????", "??? ???9???????????????"),
          "lg", List.of("?????? ???6??????????????????", "??? ????????????", "??? ???6??????????????????", "??? ???13???????????????", "??? ???19??????????????????",
              "??? ???26??????????????? ?????????1?????????????????????????????????????????????6???????????????"));

  @Override
  public String getKomi(String gameId) {
    String msg = "";
    try {
      GameStateJson gs = gameStatesTables.readLatestGameStateJson(gameId);
      User bp = usersTable.selectByPrimaryKey(gs.blackPlayerId());
      User wp = usersTable.selectByPrimaryKey(gs.whitePlayerId());
      int diff = Math.abs(wp.rank() - bp.rank());
      int ro = gs.cells()[0].length;
      String roCol = ro == 19 ? "lg" : "sm";

      String s1 = start.get(roCol).get(Math.min(diff, 5));
      String s2 = midFlow.get(roCol).get(Math.min(diff, 5));

      msg = ParameterizedStringUtils.newString(
          "{} ({}???{}???) vs {} ({}???{}???): {}?????????{}??? <br><span class='badge badge-info'>???????????????</span> {}, <span class='badge badge-info'>??????????????????</span> {} <br>",
          bp.userId(), bp.userName(), bp.rank(), wp.userId(), wp.userName(), wp.rank(), diff, ro,
          s1, s2);
    } catch (Exception e) {
      msg = "";
      log.error(e);
    }
    return msg;
  }

}
