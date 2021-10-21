package org.nkjmlab.util.db.h2;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.h2.jdbcx.JdbcConnectionPool;
import org.nkjmlab.util.db.DatabaseConfig;

public final class H2ConnectionPool implements DataSource {
  private final org.apache.logging.log4j.Logger log = LogManager.getLogger();
  private final JdbcConnectionPool connectionPool;
  private final DatabaseConfig conf;

  public static final int DEFAULT_MAX_CONNECTIONS = 256;
  public static final int DEFAULT_TIMEOUT = 30;

  public H2ConnectionPool(DatabaseConfig conf) {
    this.conf = conf;
    this.connectionPool =
        JdbcConnectionPool.create(conf.getJdbcUrl(), conf.getUsername(), conf.getPassword());
    log.info("setMaxConnections={}, setLoginTimeout={}sec, dbConfig={}", DEFAULT_MAX_CONNECTIONS,
        DEFAULT_TIMEOUT, conf);
  }

  @Override
  public Connection getConnection() throws SQLException {
    // logger.trace("nActive connection={}", getActiveConnections());
    return connectionPool.getConnection();
  }

  public void dispose() {
    connectionPool.dispose();
  }

  /**
   * Dispose connectionPool. This method is just for fail safe. User of this object should be call
   * dispose method explicitly.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      super.finalize();
    } finally {
      connectionPool.dispose();
    }
  }

  public int getActiveConnections() {
    return connectionPool.getActiveConnections();
  }

  @Override
  public void setLoginTimeout(int seconds) {
    connectionPool.setLoginTimeout(seconds);
    // logger.debug("setLoginTimeOut={}", seconds);
  }

  public void setMaxConnections(int maxConnections) {
    connectionPool.setMaxConnections(maxConnections);
    // logger.debug("setMaxConnections={}", maxConnections);
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return connectionPool.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return connectionPool.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return connectionPool.isWrapperFor(iface);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return connectionPool.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return connectionPool.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    connectionPool.setLogWriter(out);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return connectionPool.getLoginTimeout();
  }

  public DatabaseConfig getConf() {
    return conf;
  }

}
