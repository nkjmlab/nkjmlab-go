package org.nkjmlab.go.javalin.model.problem;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.ProblemJson;

public class ProblemJsonReader {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


  private static List<File> readProblemJsonFiles(Path pathToProblemJsonDir) {
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
    log.debug("detect [{}] problem files in [{}]", files.size(), pathToProblemJsonDir);
    return files.stream().map(file -> {
      try {
        ProblemJson problem =
            GoApplication.getDefaultJacksonMapper().toObject(file, ProblemJson.class);
        return problem;
      } catch (Exception e) {
        log.error("file {}", file);
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
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
