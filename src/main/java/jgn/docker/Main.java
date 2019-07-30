package jgn.docker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

  public static void main(String... args) throws InterruptedException {
    boolean isOk = false;
    boolean timeoutOccurs = false;

    CommandLineParser parser = new DefaultParser();

    Options options = new Options();
    options.addOption(Option.builder().longOpt("url").hasArg().desc("jdbc url").build());
    options.addOption(Option.builder().longOpt("username").hasArg().desc("jdbc username").build());
    options.addOption(Option.builder().longOpt("password").hasArg().desc("jdbc password").build());
    options.addOption(Option.builder().longOpt("timeout").hasArg().desc("timeout in ms. 0 = infinite retry").type(Integer.class).build());
    options.addOption(Option.builder().longOpt("retryPeriod").hasArg().desc("period to retry in ms").type(Integer.class).build());

    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      String url = line.getOptionValue("url");
      String user = line.getOptionValue("username");
      String password = line.getOptionValue("password");

      long retryPeriod = Integer.parseInt(line.getOptionValue("retryPeriod", "1000"));
      long timeout = Integer.parseInt(line.getOptionValue("timeout", "0"));

      if (url == null || user == null || password == null) {
        throw new ParseException("db cnx info not filled");
      }

      //load driver
      Class.forName("oracle.jdbc.driver.OracleDriver");

      long startTime = System.currentTimeMillis();


      while (!(isOk = tryConnect(url, user, password)) && !(timeoutOccurs = hasTimeout(timeout, startTime))) {
        Thread.sleep(retryPeriod);
      }
    } catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
      new HelpFormatter().printHelp("cmd",options);
    } catch (ClassNotFoundException e) {
      System.err.println("Cannot load driver");
    }
    if(isOk){
      System.exit(0); //OK
    }
    System.exit(1); //KO
  }

  private static boolean hasTimeout(long timeout, long startTime) {
    if (timeout == 0) {
      return false;
    }
    if (System.currentTimeMillis() > startTime + timeout) {
      System.out.println("Timeout!");
      return true;
    }
    return false;
  }

  private static boolean tryConnect(String url, String user, String password) {
    try (Connection connection = DriverManager.getConnection(url, user, password);
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT 1 FROM dual")
    ) {
      if (rs.next() && rs.getInt(1) == 1) {
        System.out.println("Oracle is up & running");
        return true;
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return false;
  }
}
