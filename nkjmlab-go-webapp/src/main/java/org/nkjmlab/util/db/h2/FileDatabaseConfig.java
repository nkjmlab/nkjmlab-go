package org.nkjmlab.util.db.h2;

import java.io.File;
import org.nkjmlab.util.db.DatabaseConfig;

public class FileDatabaseConfig extends DatabaseConfig {

  private final File dbDir;
  private final String dbName;


  public FileDatabaseConfig(File dbDir, String dbName, String username, String password) {
    super("jdbc:h2:tcp://localhost/" + new File(dbDir, dbName), username, password);
    this.dbDir = dbDir;
    this.dbName = dbName;
  }


  public File getDatabaseDirectory() {
    return dbDir;
  }

  public String getDatabaseName() {
    return dbName;
  }

  public static class Builder {
    private String dbDir;
    private String dbName;
    private String username = "";
    private String password = "";

    public Builder() {
      setDbDir("~/db/");
    }

    /**
     * Initializes a newly created {@code FileDatabaseConfig.Builder} object; you can get
     * {{@code FileDatabaseConfig} object via {@link #build()} method.
     *
     * @param dbDir the directory including the database file.
     * @param dbName the name of database.
     * @param username
     * @param password
     */
    public Builder(String dbDir, String dbName, String username, String password) {
      this.dbName = dbName;
      this.username = username;
      this.password = password;
      setDbDir(dbDir);

    }

    public Builder setUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder setDbDir(String dbDir) {
      if (dbDir.startsWith("~/")) {
        dbDir = dbDir.replace("~/",
            new File(System.getProperty("user.home")).getPath() + File.separator);
      }
      this.dbDir = dbDir;
      return this;
    }

    public Builder setDbName(String dbName) {
      this.dbName = dbName;
      return this;
    }

    public FileDatabaseConfig build() {
      return new FileDatabaseConfig(new File(dbDir), dbName, username, password);
    }
  }

  @Override
  public String toString() {
    return "FileDatabaseConfig [dbDir=" + dbDir + ", dbName=" + dbName + ", toString()="
        + super.toString() + "]";
  }



}
