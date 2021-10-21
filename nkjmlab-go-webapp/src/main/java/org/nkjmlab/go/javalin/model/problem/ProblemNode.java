package org.nkjmlab.go.javalin.model.problem;

public class ProblemNode {

  private String text;
  private long problemId;

  public ProblemNode() {}

  public ProblemNode(String problemName, long problemId) {
    this.text = problemName;
    this.problemId = problemId;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getProblemId() {
    return problemId;
  }

  public void setProblemId(long problemId) {
    this.problemId = problemId;
  }

}
