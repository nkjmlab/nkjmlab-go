package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest.UNPAIRED;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.GoApp;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;

class MatchingRequestsTableTest {

  @Test
  void testCreatePairOfUsers() {
    MatchingRequestsTable table = new MatchingRequestsTable(GoApp.getInMemoryDataSource());
    table.createTableIfNotExists();

    table.insert(new MatchingRequest("nkjm0", "0", "nkjm", 28, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm1", "1", "nkjm", 30, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm2", "2", "nkjm", 28, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm3", "3", "nkjm", 30, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm4", "3", "nkjm", 30, UNPAIRED, LocalDateTime.now()));

    GameStatesTable gTable = new GameStatesTable(GoApp.getInMemoryDataSource());
    gTable.createTableIfNotExists();

    Set<String> ret = table.createPairOfUsers(new GameStatesTables(gTable, gTable));

    System.out.println(ret);
  }
}
