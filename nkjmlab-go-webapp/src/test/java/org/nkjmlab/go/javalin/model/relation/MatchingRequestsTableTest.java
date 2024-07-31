package org.nkjmlab.go.javalin.model.relation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest.UNPAIRED;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.WebApp;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;

class MatchingRequestsTableTest {

  @Test
  void testCreatePairOfUsers() {
    GameStatesTable gTable = new GameStatesTable(WebApp.getInMemoryDataSource());
    gTable.createTableIfNotExists();

    MatchingRequestsTable table =
        new MatchingRequestsTable(
            WebApp.getInMemoryDataSource(), new GameStatesTables(gTable, gTable));
    table.createTableIfNotExists();

    table.insert(new MatchingRequest("nkjm0", "0", "nkjm", 28, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm1", "1", "nkjm", 30, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm2", "2", "nkjm", 28, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm3", "3", "nkjm", 30, UNPAIRED, LocalDateTime.now()));
    table.insert(new MatchingRequest("nkjm4", "3", "nkjm", 30, UNPAIRED, LocalDateTime.now()));

    assertThat(table.createPairOfUsers())
        .containsExactlyInAnyOrder("nkjm0", "nkjm1", "nkjm2", "nkjm3");
  }
}
