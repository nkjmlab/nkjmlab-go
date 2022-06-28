package org.nkjmlab.go.javalin;

import org.nkjmlab.sorm4j.common.DriverManagerDataSource;

public class GoApplicationTestUtils {


  public static DriverManagerDataSource getInMemoryDataSource() {
    final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    final String USER = "sa";
    final String PASSWORD = "";
    return DriverManagerDataSource.create(JDBC_URL, USER, PASSWORD);
  }


}
