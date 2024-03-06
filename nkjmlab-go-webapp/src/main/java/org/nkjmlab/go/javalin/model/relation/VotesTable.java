package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.VotesTable.Vote;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.sql.SelectSql;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKeyColumns;

public class VotesTable extends H2BasicTable<Vote> {


  private static final String PROBLEM_ID = "problem_id";
  private static final String VOTE_ID = "vote_id";
  private static final String GAME_ID = "game_id";


  public VotesTable(DataSource dataSource) {
    super(Sorm.create(dataSource), Vote.class);
  }


  public List<VoteResult> readVoteResults(long problemId, String gameId) {
    final String sql =
        select(VOTE_ID, as(SelectSql.count("*"), "NUM_OF_VOTE")) + from(getTableName());

    if (problemId != -1) {
      return getOrm().readList(VoteResult.class,
          sql + WHERE + cond(PROBLEM_ID, "=", "?") + groupBy(VOTE_ID), problemId);
    } else {
      return getOrm().readList(VoteResult.class,
          sql + where(cond(GAME_ID, "=", "?")) + groupBy(VOTE_ID), gameId);
    }
  }

  @OrmRecord
  public static record VoteResult(String voteId, int numOfVote) {
  }


  @OrmRecord
  @PrimaryKeyColumns({"USER_ID", "PROBLEM_ID", "GAME_ID"})
  public static record Vote(String userId, long problemId, String vote, String voteId,
      String gameId, LocalDateTime createdAt) {

  }

}
