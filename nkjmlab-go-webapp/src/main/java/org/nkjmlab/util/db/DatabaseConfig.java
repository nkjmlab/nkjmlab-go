package org.nkjmlab.util.db;

public class DatabaseConfig {

  private final String jdbcUrl;
  private final String username;
  private final String password;

  public DatabaseConfig(String jdbcUrl, String username, String password) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return jdbcUrl + ";USER=" + username
        + (password != null && password.length() != 0 ? ";PASSWORD=****" : "");
  }


  public static class Builder {
    private String jdbcUrl;
    private String username = "";
    private String password = "";

    public Builder() {}

    public Builder(String jdbcUrl, String dbName, String username, String password) {
      this.username = username;
      this.password = password;
    }

    public Builder setUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }


    public Builder setJdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
    }

    public DatabaseConfig build() {
      return new DatabaseConfig(jdbcUrl, username, password);
    }
  }



}
