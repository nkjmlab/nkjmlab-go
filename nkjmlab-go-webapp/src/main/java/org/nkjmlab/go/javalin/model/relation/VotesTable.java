package org.nkjmlab.go.javalin.model.relation;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.model.relation.VotesTable.Vote;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTableBase;
import org.nkjmlab.sorm4j.sql.statement.SelectSql;
import org.nkjmlab.sorm4j.sql.statement.SqlKeyword;
import org.nkjmlab.sorm4j.sql.statement.SqlTrait;
import org.nkjmlab.sorm4j.table.definition.annotation.PrimaryKeyConstraint;

public class VotesTable extends H2DefinedTableBase<Vote> implements SqlTrait, SqlKeyword {

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
      return getOrm()
          .readList(
              VoteResult.class,
              statement(sql, WHERE, cond(PROBLEM_ID, "=", "?"), groupBy(VOTE_ID)),
              problemId);
    } else {
      return getOrm()
          .readList(
              VoteResult.class, sql + where(cond(GAME_ID, "=", "?")) + groupBy(VOTE_ID), gameId);
    }
  }

  public static record VoteResult(String voteId, int numOfVote) {}

  @PrimaryKeyConstraint("USER_ID, PROBLEM_ID, GAME_ID")
  public static record Vote(
      String userId,
      long problemId,
      String vote,
      String voteId,
      String gameId,
      LocalDateTime createdAt) {}
}
