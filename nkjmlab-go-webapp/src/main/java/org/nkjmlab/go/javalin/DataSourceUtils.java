package org.nkjmlab.go.javalin;

import org.h2.jdbcx.JdbcConnectionPool;
import org.nkjmlab.util.java.concurrent.ForkJoinPoolUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceUtils {

  private static final int DEFAULT_MAX_CONNECTIONS =
      Math.min(ForkJoinPoolUtils.availableProcessors() * 2 * 2, 10);

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  public static HikariDataSource createHikariDataSource(String url, String user, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    config.setMaximumPoolSize(DEFAULT_MAX_CONNECTIONS);
    config.setConnectionTimeout(DEFAULT_TIMEOUT_SECONDS * 1000);
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("minimumIdle", "2048");
    return new HikariDataSource(config);
  }


  public static JdbcConnectionPool createH2DataSource(String url, String user, String password) {
    JdbcConnectionPool ds = JdbcConnectionPool.create(url, user, password);
    ds.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
    ds.setLoginTimeout(DEFAULT_TIMEOUT_SECONDS);
    return ds;
  }
}
