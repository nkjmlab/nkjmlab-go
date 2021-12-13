package org.nkjmlab.go.javalin.model.json;

import java.util.Map;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class GameStateUtils {
  private static final JacksonMapper mapper = JacksonMapper.getDefaultMapper();

  public static int[][] cellsStringToCellsArray(String cells) {
    return mapper.toObject(cells, int[][].class);
  }

  public static Map<String, Integer> symbolsStringToSymbols(String symbols) {
    return mapper.toObject(symbols, new TypeReference<Map<String, Integer>>() {});
  }

  public static Map<String, Object> optionsStringToMap(String options) {
    return mapper.toObject(options, new TypeReference<Map<String, Object>>() {});
  }
}
