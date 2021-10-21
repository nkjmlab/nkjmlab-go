package org.nkjmlab.util.db.h2;

import java.io.File;
import java.util.List;
import org.nkjmlab.util.collections.ArrayUtils;
import org.nkjmlab.util.db.DatabaseConfig;

/**
 * <a href="http://www.h2database.com/html/cheatSheet.html">H2 Database Engine</a>
 * <a href="http://h2database.com/html/features.html#database_url">Database URL Overview</a>
 *
 * @author nkjm
 *
 */
public class H2ConfigFactory {

  public static enum ConnectionMode {
    SERVER_MODE, EMBEDDED_MODE

  }

  public static final String JDBC_H2_TCP = "jdbc:h2:tcp://";
  public static final String JDBC_H2_TCP_LOCALHOST = JDBC_H2_TCP + "localhost/";
  public static final String JDBC_H2_EMBEDDED_DB = "jdbc:h2:file:";
  public static final String JDBC_H2_EMBEDDED_MEMORY_MAPPED_DB =
      JDBC_H2_EMBEDDED_DB + "split:nioMapped:";
  public static final String JDBC_H2_MEM = "jdbc:h2:mem:";
  public static final String SA = "sa";
  public static final String BLANK = "";
  public static final String DB_CLOSE_DELAY = "DB_CLOSE_DELAY";

  public static DatabaseConfig create(String jdbcUrl) {
    return create(jdbcUrl, SA, BLANK);
  }

  public static DatabaseConfig create(String jdbcUrl, String username, String password) {
    return new DatabaseConfig(jdbcUrl, username, password);
  }

  public static DatabaseConfig create(File file) {
    return create(file, SA, BLANK);
  }

  public static DatabaseConfig create(File file, String username, String password) {
    return new DatabaseConfig(JDBC_H2_TCP_LOCALHOST + file.toString(), username, password);
  }

  public static DatabaseConfig createInMemoryDbConfig(String dbName, String username,
      String password) {
    return new DatabaseConfig(JDBC_H2_MEM + dbName + ";" + DB_CLOSE_DELAY + "=-1", username,
        password);
  }

  public static DatabaseConfig createInMemoryDbConfig(String dbName) {
    return createInMemoryDbConfig(dbName, SA, BLANK);
  }

  public static List<String> DEFAULT_H2_SERVER_OPTIONS =
      List.of("-web", "-webAllowOthers", "-tcp", "-ifNotExists");

  /**
   *
   * @see http://www.h2database.com/html/performance.html?highlight=performance&search=performance#fast_import
   *      Fast Database Import To speed up large imports, consider using the following options
   *      temporarily: in-memory mode では効果がなさそう． "LOCK_MODE=0" を付けると使いにくそう
   *      http://www.h2database.com/html/features.html#cache_settings
   */
  public static final String[] WITHOUT_LOG_OPTIONS = {"LOG=0", "UNDO_LOG=0"};

  private static final int CACHE_SIZE = 16384;
  private static final String TWICE_CACHE_SIZE = "CACHE_SIZE=" + CACHE_SIZE * 2;
  private static final String QUAD_CACHE_SIZE = "CACHE_SIZE=" + CACHE_SIZE * 4;

  public static final String[] FAST_LARGE_IMPORT_OPTIONS =
      ArrayUtils.add(WITHOUT_LOG_OPTIONS, QUAD_CACHE_SIZE);

  public static final String[] FAST_READONLY_OPTIONS =
      ArrayUtils.addAll(WITHOUT_LOG_OPTIONS, new String[] {QUAD_CACHE_SIZE, "ACCESS_MODE_DATA=r"});
  public static final String AUTO_SERVER_OPTION = "AUTO_SERVER=TRUE";

  public static final String[] WITHOUT_LOG_AUTO_SERVER_OPTIONS =
      ArrayUtils.add(WITHOUT_LOG_OPTIONS, AUTO_SERVER_OPTION);


}
