package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils;
import org.nkjmlab.util.orangesignal_csv.OrangeSignalCsvUtils.Row;
import com.orangesignal.csv.CsvConfig;

/***
 *
 * @author nkjm
 *
 */
public class PasswordsTable {

  public static final String TABLE_NAME = "PASSWORDS";

  private static final String USER_ID = "user_id";
  private static final String PASSWORD = "password";

  private Sorm sorm;
  private TableDefinition schema;

  public PasswordsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema =
        TableDefinition.builder(TABLE_NAME).addColumnDefinition(USER_ID, VARCHAR, PRIMARY_KEY)
            .addColumnDefinition(PASSWORD, VARCHAR).build();
  }

  public void dropTableIfExists() {
    schema.dropTableIfExists(sorm);
  }


  public void createTableAndIndexesIfNotExists() {
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);
  }

  public boolean isValid(String userId, String password) {
    return Optional.ofNullable(sorm.selectByPrimaryKey(Password.class, userId))
        .map(p -> password.equals(p.password)).orElse(false);
  }



  public void readFromFileAndMerge(File usersCsvFile) {
    CsvConfig conf = OrangeSignalCsvUtils.createDefaultCsvConfig();
    conf.setSkipLines(1);
    List<Row> users = OrangeSignalCsvUtils.readAllRows(usersCsvFile, conf);
    transformToPassword(users).forEach(user -> sorm.merge(user));
  }



  private static List<Password> transformToPassword(List<Row> rows) {
    return rows.stream().map(row -> new Password(row.get(0), row.get(1)))
        .collect(Collectors.toList());
  }

  public static class Password {

    public String userId;
    public String password;

    public Password() {

    }

    public Password(String userId, String password) {
      this.userId = userId;
      this.password = password;
    }

  }

}
