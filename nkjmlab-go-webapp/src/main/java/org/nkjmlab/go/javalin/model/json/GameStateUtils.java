package org.nkjmlab.go.javalin.model.json;

import java.util.Map;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class GameStateUtils {
  private static final JacksonMapper mapper = GoApplication.getDefaultJacksonMapper();

  public static Map<String, Integer> symbolsStringToSymbols(String symbols) {
    return mapper.toObject(symbols, new TypeReference<Map<String, Integer>>() {});
  }

  public static Map<String, Object> optionsStringToMap(String options) {
    return mapper.toObject(options, new TypeReference<Map<String, Object>>() {});
  }
}
