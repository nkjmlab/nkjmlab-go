package org.nkjmlab.go.javalin.obsolete;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.Agehama;
import org.nkjmlab.go.javalin.model.common.Hand;
import org.nkjmlab.go.javalin.model.common.Hand.HandType;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.common.Stone;
import org.nkjmlab.go.javalin.model.common.Stone.Color;
import org.nkjmlab.go.javalin.model.common.Stone.Symbol;
import org.nkjmlab.go.javalin.util.CurrentTimeMillisIdGenerator;

public class ProblemTextToJsonConverter {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();



  public static List<ProblemJson> readProblemTexts(Path pathToProblemTxtDir) {
    return new ArrayList<>(convertTxtToJson(pathToProblemTxtDir).values());
  }

  private static AtomicInteger number = new AtomicInteger(0);

  private static List<File> getGroupDirectories(Path path) {
    File[] files = path.toFile().listFiles();
    if (files != null) {
      return Arrays.asList(files).stream().filter(f -> f.isDirectory())
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }


  private static final CurrentTimeMillisIdGenerator problemIdGenerator = new CurrentTimeMillisIdGenerator();

  private static Map<File, ProblemJson> convertTxtToJson(Path pathToProblemTxtDir) {
    Map<File, ProblemJson> result = new LinkedHashMap<>();
    number.set(0);
    getGroupDirectories(pathToProblemTxtDir).forEach(groupDir -> {
      Arrays.asList(groupDir.listFiles()).forEach(file -> {
        if (!file.getName().endsWith(".txt")) {
          return;
        }
        try {
          List<String> lines = Files.readAllLines(file.toPath());
          Builder json = new Builder();
          json.setProblemId(problemIdGenerator.getNewId());
          json.setGroupId(groupDir.getName());
          String name = file.getName().replace(".txt", "");
          json.setName(name);
          try {
            lines.forEach(line -> {
              if (line.length() == 0) {
                return;
              } else if (line.matches("^[0-9]+")) {
                json.setRo(Integer.parseInt(line));
                json.initCells();
              } else if (line.startsWith("> ")) {
                json.appendMessage("<p>" + " " + line.replaceAll("^> ", "") + "</p>");
              } else if (line.startsWith("{")) {
                TsukadaHand orig =
                    GoApplication.getDefaultJacksonMapper().toObject(line, TsukadaHand.class);
                procRemove(json, orig.ij0(), orig.BW());
                procPut(json, orig.ij1(), orig.BW(), orig.ID());
              } else {
                json.appendMessage("<p>" + " " + line + "</p>");
              }
            });
          } catch (Exception e) {
            log.error(groupDir.getName() + "/" + file.getName());
            log.error(json);
            log.error(e, e);
          }
          result.put(new File(groupDir, json.getName() + ".json"), json.build());
        } catch (IOException e) {
          log.error(e, e);
        }
      });
    });
    return result;
  }

  private static void procPut(Builder json, int[] ij1, int bw, int id) {
    if (ij1[0] == -1 || ij1[1] == -1) {
      return;
    }


    int stone;
    if (id == -1) {
      int tmp = switch (bw) {
        case 0 -> 40;
        case 1 -> 50;
        case 2 -> 60;
        case 3, 4 -> 10;
        case 5, 6, 7, 8, 9, 10, 11, 12, 13 -> 20;
        default -> throw new IllegalArgumentException(bw + "は無効です．");
      };
      stone = json.getCellColor(ij1[0] - 1, ij1[1] - 1) + tmp;
    } else {
      stone = switch (bw) {
        case 0, 1 -> bw + 1;
        case 2, 3, 4 -> 10;
        case 5, 6, 7, 8, 9, 10, 11, 12, 13 -> 20;
        default -> throw new IllegalArgumentException(bw + "は無効です．");
      };
    }
    Stone s = new Stone(stone);
    json.addHand(HandType.ON_BOARD, number.intValue(), ij1[0] - 1, ij1[1] - 1, s);

    if (s.color() == Color.BLANK && s.symbol() != Symbol.BLANK) {
      json.putSymbol(ij1[0] - 1, ij1[1] - 1, new Stone(stone).symbol().getId());
    }
    number.incrementAndGet();
  }

  private static void procRemove(Builder json, int[] ij0, int bw) {
    if (ij0.length == 0) {
      return;
    }
    if (ij0[0] == -1 || ij0[1] == -1) {
      return;
    }
    int stone = bw + 1;

    json.addHand(HandType.REMOVE_FROM_BOARD, number.get(), ij0[0] - 1, ij0[1] - 1,
        new Stone(stone));
    number.incrementAndGet();
  }


  public record TsukadaHand(int[] ij0, int[] ij1, int BW, int ID) {

  }

  private static class Builder {
    private long problemId;
    private String groupId;
    private String name;
    private int[][] cells;
    private Map<String, Integer> symbols = new HashMap<>();
    private String message = "";
    private int ro;
    private List<Hand> handHistory = new ArrayList<>();
    private Agehama agehama = new Agehama(0, 0);


    public Builder() {}

    public ProblemJson build() {
      return new ProblemJson(problemId, groupId, name, cells, symbols, message, ro,
          handHistory.toArray(Hand[]::new), agehama);
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
        agehama = agehama.increment(stone);
      }
      handHistory.add(new Hand(type.getTypeName(), number, x, y, stone.id(), ""));
      cells[x][y] = stone.id();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }


    public void setRo(int ro) {
      this.ro = ro;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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

    public void putSymbol(int x, int y, int symbol) {
      symbols.put(String.valueOf(x) + "-" + String.valueOf(y), symbol);
    }

    public void setProblemId(long id) {
      this.problemId = id;
    }
  }

}
