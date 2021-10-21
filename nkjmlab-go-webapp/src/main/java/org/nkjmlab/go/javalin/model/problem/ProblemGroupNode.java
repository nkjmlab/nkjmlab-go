package org.nkjmlab.go.javalin.model.problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemGroupNode {
  private String text;
  private boolean selectable = false;

  private List<String> tags = new ArrayList<>();

  private List<ProblemNode> nodes = new ArrayList<>();

  public ProblemGroupNode() {}

  public ProblemGroupNode(String groupName) {
    this.text = groupName;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<ProblemNode> getNodes() {
    return nodes;
  }

  public void setNodes(List<ProblemNode> nodes) {
    this.nodes = nodes;
  }

  public void add(String problemName, long problemId) {
    nodes.add(new ProblemNode(problemName, problemId));
  }

  public boolean isSelectable() {
    return selectable;
  }

  public void setSelectable(boolean selectable) {
    this.selectable = selectable;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public void refreshTags() {
    this.tags.clear();
    this.tags.add(String.valueOf(nodes.size()));
  }

}
