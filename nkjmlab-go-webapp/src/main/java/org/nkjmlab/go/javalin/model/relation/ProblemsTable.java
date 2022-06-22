package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.json.ProblemJson;
import org.nkjmlab.go.javalin.model.problem.ProblemFactory;
import org.nkjmlab.go.javalin.model.problem.ProblemGroupsNode;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.google.firebase.database.annotations.NotNull;

public class ProblemsTable extends BasicH2Table<Problem> {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


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
    super(Sorm.create(dataSource), Problem.class);
    createTableIfNotExists().createIndexesIfNotExists();
  }



  public List<String> getGroupsOrderByAsc() {
    return getOrm().readList(String.class,
        selectDistinct(GROUP_ID) + from(TABLE_NAME) + orderByAsc(GROUP_ID));
  }

  public List<Problem> readProblemsByGroupId(String groupId) {
    return readList(selectStarFrom(TABLE_NAME) + where(GROUP_ID + "=?") + orderByAsc(NAME),
        groupId);

  }

  public void dropAndInsertInitialProblemsToTable(File problemDir) {
    log.info("{} is problem dir", problemDir);
    deleteAll();
    List<ProblemJson> probs = ProblemFactory.readProblemJsons(problemDir.toPath());
    log.info("[{}] problems are loaded", probs.size());
    probs.forEach(j -> {
      try {
        insert(j.toProblem());
      } catch (Exception e) {
        log.error("[{} - {}] in [{}] has error = {}", j.groupId(), j.name(), problemDir,
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
          groupsNode.addProblem(groupId, problem.name(), problem.id());
        });
      });
      this.problemGroupsNodeJson = JacksonMapper.getDefaultMapper().toJson(groupsNode.getNodes());
      return problemGroupsNodeJson;
    }
  }


  @OrmRecord
  public static record Problem(@PrimaryKey long id, LocalDateTime createdAt,
      @Index @NotNull String groupId, @NotNull String name, @NotNull String cells,
      @NotNull String symbols, @NotNull String agehama, @NotNull String handHistory,
      @NotNull String message) {
  }

}
