package org.nkjmlab.go.javalin;

import org.nkjmlab.sorm4j.common.DriverManagerDataSource;

public class GoApp {
  private final DataSourceManager dataSourceManager;
  private final GoApplication app;

  public static void main(String[] args) {
    DataSourceManager manager = new DataSourceManager();
    GoApp app = new GoApp(manager);
  }

  private GoApp(DataSourceManager dataSourceManager) {
    this.dataSourceManager = dataSourceManager;
    this.app = new GoApplication(dataSourceManager);
  }

  public static DriverManagerDataSource getInMemoryDataSource() {
    final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    final String USER = "sa";
    final String PASSWORD = "";
    return DriverManagerDataSource.create(JDBC_URL, USER, PASSWORD);
  }
}
