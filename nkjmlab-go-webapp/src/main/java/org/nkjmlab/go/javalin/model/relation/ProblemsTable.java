package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.problem.ProblemJsonReader;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import com.google.firebase.database.annotations.NotNull;

public class ProblemsTable extends BasicH2Table<Problem> {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

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
    this.problemGroupNodeFactory = new ProblemGroupNodeFactory(this);
    createTableIfNotExists().createIndexesIfNotExists();
  }



  private List<String> getGroupsOrderByAsc() {
    return getOrm().readList(String.class,
        selectDistinct(GROUP_ID) + from(getTableName()) + orderByAsc(GROUP_ID));
  }

  private List<Problem> readProblemsByGroupId(String groupId) {
    return readList(selectStarFrom(getTableName()) + where(GROUP_ID + "=?") + orderByAsc(NAME),
        groupId);

  }

  public void dropAndInsertInitialProblemsToTable(File problemDir) {
    log.info("{} is problem dir", problemDir);
    deleteAll();
    List<ProblemJson> probs = ProblemJsonReader.readProblemJsons(problemDir.toPath());
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

  @OrmRecord
  public static record Problem(@PrimaryKey long id, LocalDateTime createdAt,
      @Index @NotNull String groupId, @NotNull String name, @NotNull String cells,
      @NotNull String symbols, @NotNull String agehama, @NotNull String handHistory,
      @NotNull String message) {
  }


  public String getProblemGroupsNode() {
    return problemGroupNodeFactory.getProblemGroupsNode();
  }

  public void clearProblemsJson() {
    problemGroupNodeFactory.clearProblemsJson();
  }

  public final ProblemGroupNodeFactory problemGroupNodeFactory;

  public static class ProblemGroupNodeFactory {

    private final ProblemsTable problemsTable;
    private String problemGroupsNodeJson = "";

    public ProblemGroupNodeFactory(ProblemsTable problemsTable) {
      this.problemsTable = problemsTable;
    }

    private void clearProblemsJson() {
      synchronized (problemGroupsNodeJson) {
        problemGroupsNodeJson = "";
      }
    }

    private String getProblemGroupsNode() {
      synchronized (problemGroupsNodeJson) {
        if (problemGroupsNodeJson.length() != 0) {
          return problemGroupsNodeJson;
        }
        ProblemGroupsNode groupsNode = new ProblemGroupsNode();

        List<String> tmp = problemsTable.getGroupsOrderByAsc();

        for (int i = 0; i < tmp.size(); i++) {
          if (!groupNames.contains(tmp.get(i))) {
            log.error("{} は，登録されていない問題グループです．", tmp.get(i));
            log.error("グループの順序を付けるため，" + getClass().getSimpleName() + "にグループ名を登録して下さい．");
          }
        }


        groupNames.forEach(groupId -> {
          groupsNode.addProblemGroup(groupId);
          problemsTable.readProblemsByGroupId(groupId).forEach(problem -> {
            groupsNode.addProblem(groupId, problem.name(), problem.id());
          });
        });
        this.problemGroupsNodeJson =
            GoApplication.getDefaultJacksonMapper().toJson(groupsNode.getNodes());
        return problemGroupsNodeJson;
      }
    }



    public static class ProblemGroupsNode {
      private List<ProblemGroupNode> nodes = new ArrayList<>();

      public List<ProblemGroupNode> getNodes() {
        return nodes;
      }

      public void addProblemGroup(String groupName) {
        nodes.add(new ProblemGroupNode(groupName));
      }

      public void addProblem(String groupName, String problemName, long problemId) {
        getProblemGroup(groupName).add(problemName, problemId);
        getProblemGroup(groupName).refreshTags();
      }

      private ProblemGroupNode getProblemGroup(String groupName) {
        for (ProblemGroupNode node : nodes) {
          if (node.getText().equals(groupName)) {
            return node;
          }
        }
        return null;
      }

      public static class ProblemGroupNode {
        private String text;
        private boolean selectable = false;

        private List<String> tags = new ArrayList<>();

        private List<ProblemNode> nodes = new ArrayList<>();

        public ProblemGroupNode(String groupName) {
          this.text = groupName;
        }

        public String getText() {
          return text;
        }

        public List<ProblemNode> getNodes() {
          return nodes;
        }

        public void add(String problemName, long problemId) {
          nodes.add(new ProblemNode(problemName, problemId));
        }

        public boolean isSelectable() {
          return selectable;
        }

        public List<String> getTags() {
          return tags;
        }

        public void refreshTags() {
          this.tags.clear();
          this.tags.add(String.valueOf(nodes.size()));
        }

      }
      public static record ProblemNode(String text, long problemId) {

      }

    }
  }


}
