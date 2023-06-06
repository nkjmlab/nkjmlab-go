package org.nkjmlab.go.javalin;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.nkjmlab.sorm4j.util.h2.datasource.H2LocalDataSourceFactory;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.concurrent.ForkJoinPoolUtils;
import org.nkjmlab.util.java.json.FileDatabaseConfigJson;
import org.nkjmlab.util.java.lang.ResourceUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceManager {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private static final int DEFAULT_MAX_CONNECTIONS =
      Math.min(ForkJoinPoolUtils.availableProcessors() * 2 * 2, 10);

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private H2LocalDataSourceFactory factory;

  public DataSourceManager() {
    FileDatabaseConfigJson fileDbConf = getFileDbConfig();
    H2LocalDataSourceFactory factory =
        H2LocalDataSourceFactory.builder(fileDbConf.databaseDirectory, fileDbConf.databaseName,
            fileDbConf.username, fileDbConf.password).build();
    this.factory = factory;
    factory.makeFileDatabaseIfNotExists();
    log.info("server jdbcUrl={}", factory.getServerModeJdbcUrl());
  }

  public DataSource createHikariInMemoryDataSource() {
    return createHikariDataSource(factory.getInMemoryModeJdbcUrl(), factory.getUsername(),
        factory.getPassword());
  }

  public DataSource createHikariServerModeDataSource() {
    return createHikariDataSource(factory.getServerModeJdbcUrl(), factory.getUsername(),
        factory.getPassword());
  }

  public JdbcConnectionPool createH2InMemoryDataSource() {
    return createH2DataSource(factory.getInMemoryModeJdbcUrl(), factory.getUsername(),
        factory.getPassword());
  }

  public JdbcConnectionPool createH2ServerModeDataSource() {
    return createH2DataSource(factory.getServerModeJdbcUrl(), factory.getUsername(),
        factory.getPassword());
  }


  private static HikariDataSource createHikariDataSource(String url, String user, String password) {
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


  private static JdbcConnectionPool createH2DataSource(String url, String user, String password) {
    JdbcConnectionPool ds = JdbcConnectionPool.create(url, user, password);
    ds.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
    ds.setLoginTimeout(DEFAULT_TIMEOUT_SECONDS);
    return ds;
  }

  private static FileDatabaseConfigJson getFileDbConfig() {
    try {
      return JacksonMapper.getDefaultMapper()
          .toObject(ResourceUtils.getResourceAsFile("/conf/h2.json"),
              FileDatabaseConfigJson.Builder.class)
          .build();
    } catch (Exception e) {
      log.warn("Try to load h2.json.default");
      return JacksonMapper.getDefaultMapper()
          .toObject(ResourceUtils.getResourceAsFile("/conf/h2.json.default"),
              FileDatabaseConfigJson.Builder.class)
          .build();
    }
  }

  public H2LocalDataSourceFactory getFactory() {
    return factory;
  }



}
