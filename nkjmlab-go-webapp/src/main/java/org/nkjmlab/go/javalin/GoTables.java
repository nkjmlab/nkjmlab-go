package org.nkjmlab.go.javalin;

import java.io.File;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.GoApplication.GoWebAppConfig;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
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

  public GoTables(GameStatesTables gameStatesTables, ProblemsTable problemsTable,
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

  public static GoTables prepareTables(DataSourceManager basicDataSource,
      DataSource fileDbDataSource, DataSource memDbDataSource) {
    final int TRIM_THRESHOLD_OF_GAME_STATE_TABLE = 30000;

    final ProblemsTable problemsTable;
    final HandUpsTable handsUpTable;
    final UsersTable usersTable;
    final PasswordsTable passwordsTable;
    final MatchingRequestsTable matchingRequestsTable;
    final GameStatesTables gameStatesTables;
    final VotesTable votesTable;
    final GameRecordsTable gameRecordsTable;
    final LoginsTable loginsTable;


    {
      problemsTable = new ProblemsTable(memDbDataSource);
      problemsTable.dropAndInsertInitialProblemsToTable(GoWebAppConfig.PROBLEM_DIR);
    }
    {
      loginsTable = new LoginsTable(fileDbDataSource);
      loginsTable.createTableIfNotExists().createIndexesIfNotExists();
      loginsTable.writeCsv(new File(new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
          "logins-" + System.currentTimeMillis() + ".csv"));
    }
    {
      handsUpTable = new HandUpsTable(memDbDataSource);
    }
    {
      usersTable = new UsersTable(fileDbDataSource);
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
    }
    {
      passwordsTable = new PasswordsTable(fileDbDataSource);
      passwordsTable.createTableIfNotExists().createIndexesIfNotExists();
      try {
        File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv");
        passwordsTable.readFromFileAndMerge(f);
      } catch (Exception e) {
        log.warn("load password.csv.default ...");
        File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv.default");
        passwordsTable.readFromFileAndMerge(f);
      }
    }
    {
      gameRecordsTable = new GameRecordsTable(fileDbDataSource);
      gameRecordsTable.createTableIfNotExists().createIndexesIfNotExists();
      gameRecordsTable
          .writeCsv(new File(new File(SystemFileUtils.getUserHomeDirectory(), "go-bkup/"),
              "game-record" + System.currentTimeMillis() + ".csv"));
      gameRecordsTable.recalculateAndUpdateRank(usersTable);
    }
    {
      matchingRequestsTable = new MatchingRequestsTable(memDbDataSource);
      matchingRequestsTable.createTableIfNotExists().createIndexesIfNotExists();
    }
    {
      GameStatesTable gameStatesTable = new GameStatesTable(fileDbDataSource);
      gameStatesTable.createTableIfNotExists().createIndexesIfNotExists();
      gameStatesTable.trimAndBackupToFile(basicDataSource.getFactory().getDatabaseDirectory(),
          TRIM_THRESHOLD_OF_GAME_STATE_TABLE);

      GameStatesTable gameStatesTableInMem = new GameStatesTable(memDbDataSource);
      gameStatesTableInMem.createTableIfNotExists().createIndexesIfNotExists();
      gameStatesTableInMem.insert(gameStatesTable.selectAll().toArray(GameState[]::new));

      gameStatesTables = new GameStatesTables(gameStatesTable, gameStatesTableInMem);
    }
    {
      votesTable = new VotesTable(memDbDataSource);
      votesTable.createTableIfNotExists().createIndexesIfNotExists();
    }

    GoTables goTables = new GoTables(gameStatesTables, problemsTable, usersTable, passwordsTable,
        loginsTable, matchingRequestsTable, votesTable, handsUpTable, gameRecordsTable);

    return goTables;
  }

}
