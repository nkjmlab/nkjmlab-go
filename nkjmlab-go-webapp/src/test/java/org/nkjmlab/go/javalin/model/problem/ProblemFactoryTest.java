package org.nkjmlab.go.javalin.model.problem;


import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.lang.ResourceUtils;

class ProblemFactoryTest {

  @Test
  void test() {
    List<ProblemJson> ret =
        ProblemFactory.readProblemJsonFiles(ResourceUtils.getResourceAsFile("/problem/").toPath())
            .stream().map(file -> {
              ProblemJson problem =
                  JacksonMapper.getDefaultMapper().toObject(file, ProblemJson.class);
              return problem;
            }).collect(Collectors.toList());
    assertThat(ret.size()).isEqualTo(184);
  }

}
