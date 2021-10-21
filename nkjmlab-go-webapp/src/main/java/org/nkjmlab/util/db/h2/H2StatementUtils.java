package org.nkjmlab.util.db.h2;

import static org.nkjmlab.sorm4j.sql.SelectSql.*;
import java.io.File;
import java.nio.charset.Charset;

public class H2StatementUtils {

  /**
   *
   *
   * @see <a href=
   *      "http://www.h2database.com/html/functions.html?highlight=CSVWRITE&search=csv#csvwrite">Functions</a>
   *
   * @param toFile
   * @param selectSql
   * @param charset
   * @param fieldSeparator
   * @return
   */
  public static String getCsvWriteSqlStatement(File toFile, String selectSql, String charset,
      String fieldSeparator) {
    String csvOptions = "charset=" + charset + " fieldSeparator=" + fieldSeparator;
    String csvStmt = "call csvwrite(" + literal(toFile.getAbsolutePath()) + "," + literal(selectSql)
        + "," + literal(csvOptions) + ")";
    return csvStmt;

  }

  public static String getCsvWriteSqlStatement(File toFile, String selectsql, Charset charset,
      String fieldSeparator) {
    return getCsvWriteSqlStatement(toFile, selectsql, charset.name(), fieldSeparator);
  }

}
