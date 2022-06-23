package org.nkjmlab.go.javalin.model.relation;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable.Password;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils.Row;
import com.orangesignal.csv.CsvConfig;

/***
 *
 * @author nkjm
 *
 */
public class PasswordsTable extends BasicH2Table<Password> {



  public PasswordsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), Password.class);
  }



  public boolean isValid(String userId, String password) {
    return Optional.ofNullable(selectByPrimaryKey(userId)).map(p -> password.equals(p.password))
        .orElse(false);
  }



  public void readFromFileAndMerge(File usersCsvFile) {
    CsvConfig conf = OrangeSignalCsvUtils.createDefaultCsvConfig();
    conf.setSkipLines(1);
    List<Row> users = OrangeSignalCsvUtils.readAllRows(usersCsvFile, conf);
    transformToPassword(users).forEach(user -> merge(user));
  }



  private static List<Password> transformToPassword(List<Row> rows) {
    return rows.stream().map(row -> new Password(row.get(0), row.get(1)))
        .collect(Collectors.toList());
  }


  @OrmRecord
  public static record Password(@PrimaryKey String userId, String password) {
  }

}
