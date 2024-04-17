package org.nkjmlab.go.javalin.model.relation;

import static org.assertj.core.api.Assertions.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.GoApplicationTestUtils;
import org.nkjmlab.go.javalin.model.relation.VotesTable.Vote;
import org.nkjmlab.go.javalin.model.relation.VotesTable.VoteResult;

class VotesTableTest {

  @Test
  void testReadVoteResults() {
    VotesTable table = new VotesTable(GoApplicationTestUtils.getInMemoryDataSource());
    table.createTableIfNotExists();
    long problemId = 1594221942280L;
    table.insert(new Vote("nkjm", problemId, "A", "40", "5588999", LocalDateTime.now()));
    table.insert(new Vote("nkjm2", problemId, "A", "40", "5588999", LocalDateTime.now()));
    table.insert(new Vote("nkjm3", problemId, "B", "50", "5588999", LocalDateTime.now()));

    assertThat(table.readVoteResults(problemId, null))
        .contains(new VoteResult("40", 2), new VoteResult("50", 1));
  }
}
