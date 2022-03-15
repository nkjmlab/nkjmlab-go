package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
import java.io.File;
import java.util.List;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.json.ProblemJson;
import org.nkjmlab.go.javalin.model.problem.ProblemFactory;
import org.nkjmlab.go.javalin.model.problem.ProblemGroupsNode;
import org.nkjmlab.go.javalin.model.row.Problem;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;
import org.nkjmlab.util.jackson.JacksonMapper;

public class ProblemsTable {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private Sorm sorm;
  private TableDefinition schema;

  public static final String TABLE_NAME = "PROBLEMS";

  public static final String ID = "id";
  public static final String CREATED_AT = "created_at";
  public static final String GROUP_ID = "group_id";
  public static final String NAME = "name";
  public static final String CELLS = "cells";
  public static final String SYMBOLS = "symbols";
  public static final String AGEHAMA = "agehama";
  public static final String HAND_HISTORY = "hand_history";
  public static final String MESSAGE = "message";

  private static List<String> groupNames = List.of("投票", "第1回", "第2回", "第3回", "第4回", "問題集 Part 1",
      "問題集 Part 2", "問題集 Part 3", "問題集 Part 4", "問題集 Part 5", "セキ", "模範碁");

  public ProblemsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableDefinition.builder(TABLE_NAME).addColumnDefinition(ID, BIGINT, PRIMARY_KEY)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).addColumnDefinition(GROUP_ID, VARCHAR, NOT_NULL)
        .addColumnDefinition(NAME, VARCHAR, NOT_NULL).addColumnDefinition(CELLS, VARCHAR, NOT_NULL)
        .addColumnDefinition(SYMBOLS, VARCHAR, NOT_NULL)
        .addColumnDefinition(AGEHAMA, VARCHAR, NOT_NULL)
        .addColumnDefinition(HAND_HISTORY, VARCHAR, NOT_NULL)
        .addColumnDefinition(MESSAGE, VARCHAR, NOT_NULL).addIndexDefinition(GROUP_ID).build();
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);
  }



  public List<String> getGroupsOrderByAsc() {
    return sorm.readList(String.class,
        selectDistinct(GROUP_ID) + from(TABLE_NAME) + orderByAsc(GROUP_ID));
  }

  public List<Problem> readProblemsByGroupId(String groupId) {
    return sorm.readList(Problem.class,
        selectStarFrom(TABLE_NAME) + where(GROUP_ID + "=?") + orderByAsc(NAME), groupId);

  }

  public void dropAndInsertInitialProblemsToTable(File problemDir) {
    log.info("{} is problem dir", problemDir);
    sorm.deleteAll(Problem.class);
    List<ProblemJson> probs = ProblemFactory.readProblemJsons(problemDir.toPath());
    log.info("[{}] problems are loaded", probs.size());
    probs.forEach(j -> {
      try {
        sorm.insert(j.toProblem());
      } catch (Exception e) {
        log.error("[{} - {}] in [{}] has error = {}", j.getGroupId(), j.getName(), problemDir,
            e.getMessage());
        log.error(e, e);
      }
    });
  }

  private String problemGroupsNodeJson = "";

  public void clearProblemsJson() {
    synchronized (problemGroupsNodeJson) {
      problemGroupsNodeJson = "";
    }
  }

  public String getproblemGroupsNode() {
    synchronized (problemGroupsNodeJson) {
      if (problemGroupsNodeJson.length() != 0) {
        return problemGroupsNodeJson;
      }
      ProblemGroupsNode groupsNode = new ProblemGroupsNode();

      List<String> tmp = getGroupsOrderByAsc();

      for (int i = 0; i < tmp.size(); i++) {
        if (!groupNames.contains(tmp.get(i))) {
          log.error("{} は，登録されていない問題グループです．", tmp.get(i));
          log.error("グループの順序を付けるため，" + getClass().getSimpleName() + "にグループ名を登録して下さい．");
        }
      }


      groupNames.forEach(groupId -> {
        groupsNode.addProblemGroup(groupId);
        readProblemsByGroupId(groupId).forEach(problem -> {
          groupsNode.addProblem(groupId, problem.getName(), problem.getId());
        });
      });
      this.problemGroupsNodeJson = JacksonMapper.getDefaultMapper().toJson(groupsNode.getNodes());
      return problemGroupsNodeJson;
    }
  }

  public Problem readByPrimaryKey(long pid) {
    return sorm.selectByPrimaryKey(Problem.class, pid);
  }

  public void merge(Problem p) {
    sorm.merge(p);
  }

  public void insert(Problem p) {
    sorm.insert(p);
  }

  public void delete(Problem p) {
    sorm.delete(p);
  }

}
