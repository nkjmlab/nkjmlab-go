package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;

public class HandUpsTable extends H2BasicTable<HandUp> {

  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";

  public HandUpsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), HandUp.class);
    createTableIfNotExists().createIndexesIfNotExists();
  }

  public int readOrder(String gameId) {
    List<HandUp> list = readList(selectStarFrom(getTableName()) + orderBy(CREATED_AT));

    return IntStream.range(0, list.size())
        .map(i -> list.get(i).gameId().equals(gameId) ? i : -1)
        .max()
        .orElse(-1);
  }

  public String getNextQuestion(String currentGameId) {
    List<HandUp> qs = readList(selectStarFrom(getTableName()) + orderBy(CREATED_AT));
    for (int i = 0; i < qs.size() - 1; i++) {
      if (qs.get(i).gameId().equals(currentGameId)) {
        return qs.get((i + 1) % qs.size()).gameId();
      }
    }
    return qs.size() == 0 ? "" : qs.get(0).gameId();
  }

  public List<String> readAllGameIds() {
    return getOrm().readList(String.class, SELECT + GAME_ID + FROM + getTableName());
  }

  @OrmRecord
  public static record HandUp(@PrimaryKey String gameId, LocalDateTime createdAt, String message) {}
}
