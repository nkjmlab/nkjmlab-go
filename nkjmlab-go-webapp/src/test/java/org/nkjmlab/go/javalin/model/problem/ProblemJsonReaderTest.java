package org.nkjmlab.go.javalin.model.problem;


import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.util.java.lang.ResourceUtils;

class ProblemJsonReaderTest {

  @Test
  void test() {
    List<ProblemJson> ret =
        ProblemJsonReader.readProblemJsons(ResourceUtils.getResourceAsFile("/problem/").toPath());
    assertThat(ret.size()).isEqualTo(184);

    assertThat(ret.stream().filter(json -> json.problemId() == 1560902401944L).findAny().isPresent());

  }

}
