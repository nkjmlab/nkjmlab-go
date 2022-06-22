package org.nkjmlab.go.javalin.model.problem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.nkjmlab.go.javalin.model.Stone;
import org.nkjmlab.go.javalin.model.Stone.Color;
import org.nkjmlab.go.javalin.model.Stone.Symbol;
import org.nkjmlab.go.javalin.model.json.HandType;
import org.nkjmlab.go.javalin.model.json.ProblemJson;
import org.nkjmlab.go.javalin.model.json.TsukadaHand;
import org.nkjmlab.util.jackson.JacksonMapper;

public class ProblemFactory {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


  private static final Queue<Long> ids = new ConcurrentLinkedQueue<>();

  public static synchronized long getNewId() {
    while (true) {
      long id = System.currentTimeMillis();
      if (!ids.contains(id)) {
        ids.offer(id);
        if (ids.size() > 1000) {
          ids.poll();
        }
        return id;
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
      }
    }
  }

  public static List<File> readProblemJsonFiles(Path pathToProblemJsonDir) {
    List<File> result = new ArrayList<>();
    getGroupDirectories(pathToProblemJsonDir).forEach(groupDir -> {
      Arrays.asList(groupDir.listFiles()).forEach(file -> {
        if (!file.getName().endsWith(".json")) {
          return;
        }
        result.add(file);
      });
    });
    return result;
  }

  public static List<ProblemJson> readProblemJsons(Path pathToProblemJsonDir) {
    List<File> files = readProblemJsonFiles(pathToProblemJsonDir);
    log.info("detect [{}] problem files in [{}]", files.size(), pathToProblemJsonDir);
    return files.stream().map(file -> {
      try {
        ProblemJson problem = JacksonMapper.getIgnoreUnknownPropertiesMapper().toObject(file, ProblemJson.class);
        return problem;
      } catch (Exception e) {
        log.error("file {}", file);
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
  }

  public static List<ProblemJson> readProblemTexts(Path pathToProblemTxtDir) {
    return new ArrayList<>(convertTxtToJson(pathToProblemTxtDir).values());
  }

  private static AtomicInteger number = new AtomicInteger(0);

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
          ProblemJson.Builder json = new ProblemJson.Builder();
          json.setProblemId(getNewId());
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
                    JacksonMapper.getDefaultMapper().toObject(line, TsukadaHand.class);
                procRemove(json, orig.getIj0(), orig.getBW());
                procPut(json, orig.getIj1(), orig.getBW(), orig.getID());
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

  private static void procPut(ProblemJson.Builder json, int[] ij1, int bw, int id) {
    if (ij1[0] == -1 || ij1[1] == -1) {
      return;
    }

    int stone = 0;
    if (id == -1) {
      int tmp = 0;
      switch (bw) {
        case 0:
          tmp = 40;
          break;
        case 1:
          tmp = 50;
          break;
        case 2:
          tmp = 60;
          break;
        case 3:
        case 4:
          tmp = 10;
          break;
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
          tmp = 20;
          break;
        default:
          throw new IllegalArgumentException(bw + "は無効です．");
      }
      stone = json.getCellColor(ij1[0] - 1, ij1[1] - 1) + tmp;
    } else {
      switch (bw) {
        case 0:
        case 1:
          stone = bw + 1;
          break;
        case 2:
        case 3:
        case 4:
          stone = 10;
          break;
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
          stone = 20;
          break;
        default:
          throw new IllegalArgumentException(bw + "は無効です．");
      }
    }
    Stone s = new Stone(stone);
    json.addHand(HandType.ON_BOARD, number.intValue(), ij1[0] - 1, ij1[1] - 1, s);

    if (s.getColor() == Color.BLANK && s.getSymbol() != Symbol.BLANK) {
      json.putSymbol(ij1[0] - 1, ij1[1] - 1, new Stone(stone).getSymbol().getId());
    }
    number.incrementAndGet();
  }

  private static void procRemove(ProblemJson.Builder json, int[] ij0, int bw) {
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

  private static List<File> getGroupDirectories(Path path) {
    File[] files = path.toFile().listFiles();
    if (files != null) {
      return Arrays.asList(files).stream().filter(f -> f.isDirectory())
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }
}
