package org.nkjmlab.go.javalin.model.json;


public class VoteResult {
  private String voteId;
  private int numOfVote;

  public VoteResult() {}

  public String getVoteId() {
    return voteId;
  }

  public void setVoteId(String voteId) {
    this.voteId = voteId;
  }

  public int getNumOfVote() {
    return numOfVote;
  }

  public void setNumOfVote(int numOfVote) {
    this.numOfVote = numOfVote;
  }

}
