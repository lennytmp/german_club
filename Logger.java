package FightLang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.Runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Date;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

class Logger {
  private static String logsFile;
  private static String clientsPath;
  private static String varsPath;
  private static String exceptionsLog;
  private static final String EXT = ".db";
  private static final String BACKUP_FILE = ".backup";
  private static final String CONFIG_FILE = "config.json";
  private static PrintWriter logsWriter;

  public static void initialize() {
    if (hasClientsBackup()) {
      restoreClientsFromBackup();
      removeClientsBackup();
    }
  }

  static void logException(Exception e) {
    try (StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          PrintWriter out = new PrintWriter(exceptionsLog)) {
      e.printStackTrace(pw);
      String stackTrace = sw.toString();
      out.println(stackTrace);
      if (!Main.isProd) {
        System.out.println(stackTrace);
      }
      log(stackTrace);
      String[] cmd = {
        "/bin/sh",
        "-c",
        "cat " + exceptionsLog + " | sendmail " +
        "lennytmp@gmail.com borodin.vadim@gmail.com"
      };
      Runtime.getRuntime().exec(cmd);
    } catch (IOException e2) {
      log(e2.toString());
      System.exit(1);
    }
  }

  static String getConfigText() {
    String result = readOneLineFile(CONFIG_FILE);
    if (result == null || result.isEmpty()) {
      Logger.log("Could not read config file with API keys");
      System.exit(4);
    }
    return result;
  }

  static void setDbPath(String path) {
    clientsPath = path + "/clients/";
    exceptionsLog = path + "/exceptions_log";
    logsFile = path + "/network";
    varsPath = path + "/vars/";
  }

  static List<String> getAllClientNames() {
    List<String> result = new LinkedList<>();
    File folder = new File(clientsPath);
    File[] listOfFiles = folder.listFiles();
    for (File f : listOfFiles) {
      if (f.isFile()) {
        String fileName = f.getName();
        String name = fileName.substring(0, fileName.length() - EXT.length());
        result.add(name);
      }
    }
    return result;
  }

  static void saveClient(String name, String value) {
    makeClientBackup(name);
    writeClient(name, value); 
    removeClientsBackup();
  }

  static void saveClients(String[] names, String[] values) {
    assert names.length == values.length;
    makeClientsBackup(names);
    for (int i = 0; i < names.length; i++) {
      writeClient(names[i], values[i]); 
    }
    removeClientsBackup();
  }

  static void writeClient(String name, String value) {
    try (FileWriter fw = new FileWriter(clientsPath + name + EXT, false)) {
      fw.write(value);
    } catch (IOException e) {
      Logger.logException(e);
    }
  }

  static String getClient(String name) {
    return readOneLineFile(clientsPath + name + EXT, true);
  }

  static void saveIntVar(String name, int value) {
      // TODO: for some variables (maxUpdateId) it would be better to open file once
      // and use flush to save data.
    try (FileWriter fw = new FileWriter(varsPath + name + EXT, false)) {
      fw.write(Integer.toString(value));
    } catch (IOException e) {
      Logger.logException(e);
    }
  }

  static Integer getIntVar(String name) {
    String value = readOneLineFile(varsPath + name + EXT, true);
    if (value == null || value.isEmpty()) {
      return null;
    }
    return Integer.parseInt(value);
  }

  static void log(String entry) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    entry = dateFormat.format(date) + " " + entry;
    Logger.getLogsWriter().println(entry);
  }

  private static boolean hasClientsBackup() {
    File f = new File(clientsPath + BACKUP_FILE);
    return f.exists() && !f.isDirectory();
  }

  private static void restoreClientsFromBackup() {
    try {
      BufferedReader br = new BufferedReader(
          new FileReader(clientsPath + BACKUP_FILE));
      String backupLine = br.readLine();
      Pattern nameExtractor = Pattern.compile("^([-0-9]*)");
      Pattern valueExtractor = Pattern.compile("^.*?;(.*)$");
      while (backupLine != null) {
        String name = Utils.getMatch(backupLine, nameExtractor);
        String value = Utils.getMatch(backupLine, valueExtractor);
        writeClient(name, value);
        backupLine = br.readLine();
      }
      br.close();
    } catch (Exception e) {
      logException(e);
    }
  }

  private static void removeClientsBackup() {
    Path p = Paths.get(clientsPath + BACKUP_FILE);
    try {
      Files.delete(p);
    } catch (Exception e) {
      Logger.logException(e);
    }
  }

  private static void makeClientBackup(String filename) {
    makeClientsBackup(new String[] {filename});
  }

  private static void makeClientsBackup(String[] filenames) {
    try (BufferedWriter bw = new BufferedWriter(
            new FileWriter(clientsPath + BACKUP_FILE));
         PrintWriter backupWriter = new PrintWriter(bw)) {
      for (int i = 0; i < filenames.length; i++) {
        String value = getClient(filenames[i]);
        backupWriter.println(filenames[i] + ";" + value);
      }
    } catch (Exception e) {
      Logger.logException(e);
    }
  }

  private static String readOneLineFile(String filename) {
    return readOneLineFile(filename, false);
  }

  private static String readOneLineFile(String filename, boolean ignoreErr) {
    String value = null;
    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      value = br.readLine();
    } catch (IOException e) {
      if (!ignoreErr) {
        Logger.logException(e);
      }
    } 
    return value;
  }

  static String readAllFile(String filename) {
    String str = "";
    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      String line = "";
      while ((line = br.readLine()) != null) {
        str += line;
      }
    } catch (Exception e) {
      Logger.logException(e);
    }
    return str;
  }

  private static PrintWriter getLogsWriter() {
    if (logsWriter == null) {
      try {
        logsWriter = new PrintWriter(new BufferedWriter(
            new FileWriter(logsFile + EXT, true)
        ), true);
      } catch (Exception e) {
        Logger.logException(e);
        System.exit(3);
      }
    }
    return logsWriter;
  }
}
