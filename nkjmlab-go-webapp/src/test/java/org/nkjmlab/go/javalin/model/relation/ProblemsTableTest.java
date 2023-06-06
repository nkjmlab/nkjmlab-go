package org.nkjmlab.go.javalin.model.relation;


import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.util.java.lang.ResourceUtils;

class ProblemsTableTest {

  @Test
  void test() {
    List<ProblemJson> ret =
        ProblemsTable.readProblemJsons(ResourceUtils.getResourceAsFile("/problem/").toPath());
    assertThat(ret.size()).isEqualTo(184);

    assertThat(
        ret.stream().filter(json -> json.problemId() == 1560902401944L).findAny().isPresent());

  }

}
