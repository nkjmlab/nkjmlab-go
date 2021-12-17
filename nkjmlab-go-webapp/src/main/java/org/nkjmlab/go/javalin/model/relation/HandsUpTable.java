package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.sql.SqlKeyword.*;
import static org.nkjmlab.sorm4j.sql.schema.TableSchema.Keyword.*;
import java.util.List;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.row.HandUp;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.sql.schema.TableSchema;

public class HandsUpTable {

  private final TableSchema schema;
  private final Sorm sorm;

  public static final String TABLE_NAME = "HANDS_UP";

  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";
  private static final String MESSAGE = "message";

  public HandsUpTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableSchema.builder().setTableName(TABLE_NAME)
        .addColumnDefinition(GAME_ID, VARCHAR, PRIMARY_KEY)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).addColumnDefinition(MESSAGE, VARCHAR).build();
    schema.createTableAndIndexesIfNotExists(sorm);
  }

  public void dropTableIfExists() {
    sorm.executeUpdate(schema.getDropTableIfExistsStatement());
  }

  public void createTableAndIndexesIfNotExists() {
    sorm.executeUpdate(schema.getCreateTableIfNotExistsStatement());
    schema.getCreateIndexIfNotExistsStatements()
        .forEach(createIndexStatement -> sorm.executeUpdate(createIndexStatement));
  }

  public int count() {
    return sorm.readOne(Integer.class, select(func(COUNT, STAR)) + from(TABLE_NAME));
  }

  public int readOrder(String gameId) {
    List<HandUp> list = sorm.readList(HandUp.class, selectFrom(TABLE_NAME) + orderBy(CREATED_AT));

    return IntStream.range(0, list.size()).map(i -> list.get(i).getGameId().equals(gameId) ? i : -1)
        .max().orElse(-1);

  }

  public String getNextQuestion(String currentGameId) {
    List<HandUp> qs = sorm.readList(HandUp.class, selectFrom(TABLE_NAME) + orderBy(CREATED_AT));
    for (int i = 0; i < qs.size() - 1; i++) {
      if (qs.get(i).getGameId().equals(currentGameId)) {
        return qs.get((i + 1) % qs.size()).getGameId();
      }
    }
    return qs.size() == 0 ? "" : qs.get(0).getGameId();
  }

  public List<String> readAllGameIds() {
    return sorm.readList(String.class, SELECT + GAME_ID + FROM + TABLE_NAME);
  }

  public HandUp readByPrimaryKey(String gameId) {
    return sorm.readByPrimaryKey(HandUp.class, gameId);
  }

  public void insert(HandUp handUp) {
    sorm.insert(handUp);
  }

  public void update(HandUp handUp) {
    sorm.insert(handUp);
  }

  public void delete(HandUp handUp) {
    sorm.delete(handUp);

  }



}
