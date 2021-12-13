package org.nkjmlab.go.javalin.model.json;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nkjmlab.go.javalin.model.Stone;
import org.nkjmlab.go.javalin.model.row.Problem;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemJson {

  private long problemId;
  private String groupId;
  private String name;
  private int[][] cells;
  private Map<String, Integer> symbols = new HashMap<>();
  private String message = "";
  private int ro;
  private List<HandJson> handHistory = new ArrayList<>();
  private AgehamaJson agehama = new AgehamaJson();

  private static final JacksonMapper mapper = JacksonMapper.getDefaultMapper();

  public ProblemJson() {}

  private ProblemJson(long id, String groupId, String name, int[][] cells,
      Map<String, Integer> symbols, String message, int ro, List<HandJson> handHistory,
      AgehamaJson agehama) {
    this.problemId = id;
    this.groupId = groupId;
    this.name = name;
    this.cells = cells;
    this.symbols = symbols;
    this.message = message;
    this.ro = ro;
    this.handHistory = handHistory;
    this.agehama = agehama;
  }

  public Problem toProblem() {
    return new Problem(problemId, Timestamp.from(Instant.now()), groupId, name, mapper.toJson(cells),
        mapper.toJson(symbols), message, mapper.toJson(handHistory), mapper.toJson(agehama));
  }

  public static ProblemJson createFrom(Problem problem) {
    int[][] cells = mapper.toObject(problem.getCells(), int[][].class);
    return new ProblemJson(problem.getId(), problem.getGroupId(), problem.getName(), cells,
        mapper.toObject(problem.getSymbols(), new TypeReference<Map<String, Integer>>() {}),
        problem.getMessage(), cells.length,
        mapper.toObject(problem.getHandHistory(), new TypeReference<List<HandJson>>() {}),
        mapper.toObject(problem.getAgehama(), AgehamaJson.class));
  }

  public void initCells() {
    if (cells == null) {
      cells = new int[ro][ro];
      for (int i = 0; i < cells.length; i++) {
        Arrays.fill(cells[i], 0);
      }
    }
  }

  public void addHand(HandType type, int number, int x, int y, Stone stone) {
    if (type == HandType.AGEHAMA) {
      agehama.increment(stone);
    }
    handHistory.add(new HandJson(type, number, x, y, stone.getId()));
    cells[x][y] = stone.getId();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getRo() {
    return ro;
  }

  public void setRo(int ro) {
    this.ro = ro;
  }

  public int[][] getCells() {
    return cells;
  }

  public void setCells(int[][] cells) {
    this.cells = cells;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void appendMessage(String msg) {
    this.message += msg;
  }

  public int getCellColor(int x, int y) {
    return cells[x][y];
  }

  public List<HandJson> getHandHistory() {
    return handHistory;
  }

  public void setHandHistory(List<HandJson> handHistory) {
    this.handHistory = handHistory;
  }

  public AgehamaJson getAgehama() {
    return agehama;
  }

  public void setAgehama(AgehamaJson agehama) {
    this.agehama = agehama;
  }

  public void putSymbol(int x, int y, int symbol) {
    symbols.put(String.valueOf(x) + "-" + String.valueOf(y), symbol);
  }

  public void setProblemId(long id) {
    this.problemId = id;
  }

  public long getProblemId() {
    return problemId;
  }

  public Map<String, Integer> getSymbols() {
    return symbols;
  }

  public void setSymbols(Map<String, Integer> symbols) {
    this.symbols = symbols;
  }

}
