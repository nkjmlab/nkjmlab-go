package org.nkjmlab.go.javalin.model.problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemGroupsNode {
  private List<ProblemGroupNode> nodes = new ArrayList<>();

  public List<ProblemGroupNode> getNodes() {
    return nodes;
  }

  public void setNodes(List<ProblemGroupNode> nodes) {
    this.nodes = nodes;
  }

  public void addProblemGroup(String groupName) {
    nodes.add(new ProblemGroupNode(groupName));
  }

  public void addProblem(String groupName, String problemName, long problemId) {
    getProblemGroup(groupName).add(problemName, problemId);
    getProblemGroup(groupName).refreshTags();
  }

  private ProblemGroupNode getProblemGroup(String groupName) {
    for (ProblemGroupNode node : nodes) {
      if (node.getText().equals(groupName)) {
        return node;
      }
    }
    return null;
  }

}
