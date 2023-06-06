package org.nkjmlab.go.javalin;

import java.io.File;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
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

  private GoTables(GameStatesTables gameStatesTables, ProblemsTable problemsTable,
      UsersTable usersTable, PasswordsTable passwordsTable, LoginsTable loginsTable,
      MatchingRequestsTable matchingRequestsTable, VotesTable votesTable, HandUpsTable handsUpTable,
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
  }

  public static GoTables prepareTables(DataSourceManager basicDataSource) {

    DataSource memDbDataSource = basicDataSource.createHikariInMemoryDataSource();
    DataSource fileDbDataSource = basicDataSource.createHikariServerModeDataSource();


    final ProblemsTable problemsTable = prepareProblemTables(memDbDataSource);
    final HandUpsTable handsUpTable = new HandUpsTable(memDbDataSource);
    final MatchingRequestsTable matchingRequestsTable =
        prepareMatchingRequestsTable(memDbDataSource);
    final PasswordsTable passwordsTable = preparePasswordsTable(memDbDataSource);
    final VotesTable votesTable = prepareVotesTable(memDbDataSource);

    final GameStatesTables gameStatesTables =
        prepareGameStateTables(basicDataSource, fileDbDataSource, memDbDataSource);

    final UsersTable usersTable = prepareUsersTable(fileDbDataSource);
    final GameRecordsTable gameRecordsTable = prepareGameRecordsTable(fileDbDataSource, usersTable);
    final LoginsTable loginsTable = prepareLoginsTable(fileDbDataSource);


    GoTables goTables = new GoTables(gameStatesTables, problemsTable, usersTable, passwordsTable,
        loginsTable, matchingRequestsTable, votesTable, handsUpTable, gameRecordsTable);

    return goTables;
  }

  private static GameRecordsTable prepareGameRecordsTable(DataSource fileDbDataSource,
      UsersTable usersTable) {
    GameRecordsTable gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    gameRecordsTable.createTableIfNotExists().createIndexesIfNotExists();
    gameRecordsTable.writeCsv(new File(new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
        "game-record" + System.currentTimeMillis() + ".csv"));
    gameRecordsTable.recalculateAndUpdateRank(usersTable);
    return gameRecordsTable;
  }

  private static VotesTable prepareVotesTable(DataSource memDbDataSource) {
    VotesTable votesTable = new VotesTable(memDbDataSource);
    votesTable.createTableIfNotExists().createIndexesIfNotExists();
    return votesTable;
  }

  private static MatchingRequestsTable prepareMatchingRequestsTable(DataSource memDbDataSource) {
    MatchingRequestsTable matchingRequestsTable = new MatchingRequestsTable(memDbDataSource);
    matchingRequestsTable.createTableIfNotExists().createIndexesIfNotExists();
    return matchingRequestsTable;
  }

  private static GameStatesTables prepareGameStateTables(DataSourceManager basicDataSource,
      DataSource fileDbDataSource, DataSource memDbDataSource) {
    final int TRIM_THRESHOLD_OF_GAME_STATE_TABLE = 30000;

    GameStatesTable gameStatesTable = new GameStatesTable(fileDbDataSource);
    gameStatesTable.createTableIfNotExists().createIndexesIfNotExists();
    gameStatesTable.trimAndBackupToFile(basicDataSource.getFactory().getDatabaseDirectory(),
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
    try {
      File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv");
      passwordsTable.readFromFileAndMerge(f);
    } catch (Exception e) {
      log.warn("load password.csv.default ...");
      File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv.default");
      passwordsTable.readFromFileAndMerge(f);
    }
    return passwordsTable;
  }

  private static LoginsTable prepareLoginsTable(DataSource fileDbDataSource) {
    LoginsTable loginsTable = new LoginsTable(fileDbDataSource);
    loginsTable.createTableIfNotExists().createIndexesIfNotExists();
    loginsTable.writeCsv(new File(new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
        "logins-" + System.currentTimeMillis() + ".csv"));
    return loginsTable;
  }

  private static UsersTable prepareUsersTable(DataSource dataSource) {
    UsersTable usersTable = new UsersTable(dataSource);
    usersTable.dropTableIfExists();
    usersTable.createTableIfNotExists().createIndexesIfNotExists();
    try {
      File f = ResourceUtils.getResourceAsFile("/conf/users.csv");
      usersTable.readFromFileAndMerge(f);
    } catch (Exception e) {
      log.error(e, e);
      log.warn("load users.csv.default ...");
      File f = ResourceUtils.getResourceAsFile("/conf/users.csv.default");
      usersTable.readFromFileAndMerge(f);
    }
    return usersTable;
  }

  private static ProblemsTable prepareProblemTables(DataSource memDbDataSource) {
    ProblemsTable problemsTable = new ProblemsTable(memDbDataSource);
    problemsTable.dropAndInsertInitialProblemsToTable(GoWebAppConfig.PROBLEM_DIR);
    return problemsTable;
  }

}
