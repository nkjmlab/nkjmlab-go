package org.nkjmlab.go.javalin.model.json;

import java.time.LocalDateTime;
import java.util.Map;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public record ProblemJson(long problemId, String groupId, String name, int[][] cells,
    Map<String, Integer> symbols, String message, int ro, HandJson[] handHistory,
    AgehamaJson agehama) {

  private static final JacksonMapper mapper = GoApplication.getDefaultJacksonMapper();

  public ProblemJson(int id) {
    this(id, null, null, null, null, null, -1, null, null);
  }

  public Problem toProblem() {
    return new Problem(problemId, LocalDateTime.now(), groupId, name, mapper.toJson(cells),
        mapper.toJson(symbols), mapper.toJson(agehama), mapper.toJson(handHistory), message);
  }

  public static ProblemJson createFrom(Problem problem) {
    int[][] cells = mapper.toObject(problem.cells(), int[][].class);
    return new ProblemJson(problem.id(), problem.groupId(), problem.name(), cells,
        mapper.toObject(problem.symbols(), new TypeReference<Map<String, Integer>>() {}),
        problem.message(), cells.length, mapper.toObject(problem.handHistory(), HandJson[].class),
        mapper.toObject(problem.agehama(), AgehamaJson.class));
  }


}
