package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.sql.SqlKeyword.*;
import static org.nkjmlab.sorm4j.table.TableSchema.Keyword.*;
import java.util.List;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.json.VoteResult;
import org.nkjmlab.go.javalin.model.row.Vote;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.table.TableSchema;

public class VotesTable {

  public static final String TABLE_NAME = "VOTES";

  private static final String USER_ID = "user_id";
  private static final String PROBLEM_ID = "problem_id";
  private static final String VOTE = "vote";
  private static final String VOTE_ID = "vote_id";
  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";

  private Sorm sorm;

  private TableSchema schema;

  public VotesTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = new TableSchema.Builder(TABLE_NAME).addColumnDefinition(USER_ID, VARCHAR)
        .addColumnDefinition(PROBLEM_ID, BIGINT).addColumnDefinition(VOTE, VARCHAR)
        .addColumnDefinition(VOTE_ID, VARCHAR).addColumnDefinition(GAME_ID, VARCHAR)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).setPrimaryKey(USER_ID, PROBLEM_ID, GAME_ID)
        .build();
    schema.createTableAndIndexesIfNotExists(sorm);
  }


  public List<VoteResult> readVoteResults(long problemId, String gameId) {
    final String sql = select(VOTE_ID, as(count("*"), "NUM_OF_VOTE")) + from(TABLE_NAME);

    if (problemId != -1) {
      return sorm.readList(VoteResult.class,
          sql + WHERE + cond(PROBLEM_ID, "=", "?") + groupBy(VOTE_ID), problemId);
    } else {
      return sorm.readList(VoteResult.class,
          sql + where(cond(GAME_ID, "=", "?")) + groupBy(VOTE_ID), gameId);
    }
  }


  public void merge(Vote vote) {
    sorm.merge(vote);
  }

}
