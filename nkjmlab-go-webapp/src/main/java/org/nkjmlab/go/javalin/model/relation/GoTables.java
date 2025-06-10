package org.nkjmlab.go.javalin.model.relation;

import java.io.File;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.GoDataSourceManager;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService.Icons;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.util.java.io.SystemFileUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;

public class GoTables {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public final GameStatesTables gameStatesTables;
  public final ProblemsTable problemsTable;
  public final UsersTable usersTable;
  public final LoginsTable loginsTable;
  public final MatchingRequestsTable matchingRequestsTable;
  public final VotesTable votesTable;
  public final HandUpsTable handsUpTable;
  public final GameRecordsTable gameRecordsTable;
  public final PasswordsTable passwordsTable;
  public final Icons icons;

  private GoTables(
      File webrootDir,
      GameStatesTables gameStatesTables,
      ProblemsTable problemsTable,
      UsersTable usersTable,
      PasswordsTable passwordsTable,
      LoginsTable loginsTable,
      MatchingRequestsTable matchingRequestsTable,
      VotesTable votesTable,
      HandUpsTable handsUpTable,
      GameRecordsTable gameRecordsTable) {
    this.gameStatesTables = gameStatesTables;
    this.problemsTable = problemsTable;
    this.usersTable = usersTable;
    this.passwordsTable = passwordsTable;
    this.loginsTable = loginsTable;
    this.matchingRequestsTable = matchingRequestsTable;
    this.votesTable = votesTable;
    this.handsUpTable = handsUpTable;
    this.gameRecordsTable = gameRecordsTable;
    this.icons = new Icons(webrootDir);
  }

  public static GoTables prepareTables(
      File webrootDir, File appRootDir, GoDataSourceManager basicDataSource) {

    DataSource memDbDataSource = basicDataSource.createHikariInMemoryDataSource();
    DataSource fileDbDataSource = basicDataSource.createH2MixedModeDataSource();

    final ProblemsTable problemsTable = prepareProblemTables(appRootDir, memDbDataSource);
    final HandUpsTable handsUpTable = new HandUpsTable(memDbDataSource);

    final PasswordsTable passwordsTable = preparePasswordsTable(memDbDataSource);
    final VotesTable votesTable = prepareVotesTable(memDbDataSource);

    final GameStatesTables gameStatesTables =
        prepareGameStateTables(basicDataSource, fileDbDataSource, memDbDataSource);

    final MatchingRequestsTable matchingRequestsTable =
        prepareMatchingRequestsTable(memDbDataSource, gameStatesTables);

    final UsersTable usersTable = prepareUsersTable(fileDbDataSource);
    final GameRecordsTable gameRecordsTable = prepareGameRecordsTable(fileDbDataSource, usersTable);
    final LoginsTable loginsTable = prepareLoginsTable(fileDbDataSource);

    GoTables goTables =
        new GoTables(
            webrootDir,
            gameStatesTables,
            problemsTable,
            usersTable,
            passwordsTable,
            loginsTable,
            matchingRequestsTable,
            votesTable,
            handsUpTable,
            gameRecordsTable);

    return goTables;
  }

  private static GameRecordsTable prepareGameRecordsTable(
      DataSource fileDbDataSource, UsersTable usersTable) {
    GameRecordsTable gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    gameRecordsTable.createTableIfNotExists().createIndexesIfNotExists();
    gameRecordsTable.writeCsv(
        new File(
            new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
            "game-record" + System.currentTimeMillis() + ".csv"));
    return gameRecordsTable;
  }

  private static VotesTable prepareVotesTable(DataSource memDbDataSource) {
    VotesTable votesTable = new VotesTable(memDbDataSource);
    votesTable.createTableIfNotExists().createIndexesIfNotExists();
    return votesTable;
  }

  private static MatchingRequestsTable prepareMatchingRequestsTable(
      DataSource memDbDataSource, GameStatesTables gameStatesTables) {
    MatchingRequestsTable matchingRequestsTable =
        new MatchingRequestsTable(memDbDataSource, gameStatesTables);
    matchingRequestsTable.createTableIfNotExists().createIndexesIfNotExists();
    return matchingRequestsTable;
  }

  private static GameStatesTables prepareGameStateTables(
      GoDataSourceManager basicDataSource,
      DataSource fileDbDataSource,
      DataSource memDbDataSource) {
    final int TRIM_THRESHOLD_OF_GAME_STATE_TABLE = 30000;

    GameStatesTable gameStatesTable = new GameStatesTable(fileDbDataSource);
    gameStatesTable.createTableIfNotExists().createIndexesIfNotExists();
    gameStatesTable.trimAndBackupToFile(
        basicDataSource.getFactory().getDatabaseDirectoryPath().toFile(),
        TRIM_THRESHOLD_OF_GAME_STATE_TABLE);

    GameStatesTable gameStatesTableInMem = new GameStatesTable(memDbDataSource);
    gameStatesTableInMem.createTableIfNotExists().createIndexesIfNotExists();
    gameStatesTableInMem.insert(gameStatesTable.selectAll().toArray(GameState[]::new));

    GameStatesTables gameStatesTables = new GameStatesTables(gameStatesTable, gameStatesTableInMem);
    return gameStatesTables;
  }

  private static PasswordsTable preparePasswordsTable(DataSource dataSource) {
    PasswordsTable passwordsTable = new PasswordsTable(dataSource);
    passwordsTable.createTableIfNotExists().createIndexesIfNotExists();
    File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv");
    if (f == null) {
      log.warn("load password.csv.default ...");
      f = ResourceUtils.getResourceAsFile("/conf/passwords.csv.default");
    }
    passwordsTable.readFromFileAndMerge(f);
    return passwordsTable;
  }

  private static LoginsTable prepareLoginsTable(DataSource fileDbDataSource) {
    LoginsTable loginsTable = new LoginsTable(fileDbDataSource);
    loginsTable.createTableIfNotExists().createIndexesIfNotExists();
    loginsTable.writeCsv(
        new File(
            new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
            "logins-" + System.currentTimeMillis() + ".csv"));
    return loginsTable;
  }

  private static UsersTable prepareUsersTable(DataSource dataSource) {
    UsersTable usersTable = new UsersTable(dataSource);
    usersTable.dropTableIfExists();
    usersTable.createTableIfNotExists().createIndexesIfNotExists();
    File f = ResourceUtils.getResourceAsFile("/conf/initial-users.csv");
    if (f == null) {
      f = ResourceUtils.getResourceAsFile("/conf/initial-users.csv.default");
    }
    usersTable.readFileAndInsertIfNotExists(f);
    return usersTable;
  }

  private static ProblemsTable prepareProblemTables(File appRootDir, DataSource memDbDataSource) {
    final File PROBLEM_DIR = new File(appRootDir, "problem");
    ProblemsTable problemsTable = new ProblemsTable(memDbDataSource, PROBLEM_DIR);
    problemsTable.dropAndInsertInitialProblemsToTable();
    return problemsTable;
  }
}
