package org.nkjmlab.go.javalin.model.relation;

import java.io.File;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable.Password;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.h2.functions.table.CsvRead;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;

/***
 *
 * @author nkjm
 *
 */
public class PasswordsTable extends H2BasicTable<Password> {

  public PasswordsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), Password.class);
  }

  public boolean isValid(String userId, String password) {
    return Optional.ofNullable(selectByPrimaryKey(userId)).map(p -> password.equals(p.password))
        .orElse(false);
  }

  public void readFromFileAndMerge(File csvFile) {
    List<Password> password =
        readList("select * from " + CsvRead.builderForCsvWithHeader(csvFile).build().getSql());
    merge(password);
  }

  @OrmRecord
  public static record Password(@PrimaryKey String userId, String password) {
  }

}
