package org.nkjmlab.util.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.nkjmlab.util.concurrent.ForkJoinPoolUtils;

public class FileUtils {

  /**
   * Getting a temp file object of the temporal directory which is referrenced by
   * {@code System.getProperty("java.io.tmpdir")}.
   *
   * @return
   */
  public static File getTempDirectory() {
    return new File(getTempDirectoryPathString());

  }

  /**
   * Getting a path object of the temporal directory which is referrenced by
   * {@code System.getProperty("java.io.tmpdir")}.
   *
   * @return
   */
  public static Path getTempDirectoryPath() {
    return getTempDirectory().toPath();

  }

  /**
   * Getting a string object of the temporal directory which is referrenced by
   * {@code System.getProperty("java.io.tmpdir")}.
   *
   * @return
   */
  public static String getTempDirectoryPathString() {
    return System.getProperty("java.io.tmpdir");
  }

  /**
   * Getting a file object of the temporal directory which is referrenced by
   * {@code System.getProperty("user.home")}.
   *
   * @return
   */
  public static File getUserDirectory() {
    return new File(getUserHomeDirectoryPathString());

  }

  /**
   * Getting a path object of the directory which is referenced by
   * {@code System.getProperty("user.home")}.
   *
   * @return
   */
  public static Path getUserDirectoryPath() {
    return getUserDirectory().toPath();

  }

  /**
   * Getting a string object of the directory which is referenced by
   * {@code System.getProperty("user.home")}.
   *
   * @return
   */
  public static String getUserHomeDirectoryPathString() {
    return System.getProperty("user.home");
  }

  public static String getCurrentDirectoryPathString() {
    return System.getProperty("user.dir");
  }

  public static File getCurrentDirectory() {
    return new File(getCurrentDirectoryPathString());
  }

  public static File getFileInCurrentDirectory(String relativePath) {
    return new File(getCurrentDirectoryPathString(), relativePath);
  }

  /**
   * Getting a temp file object in the directory in temporal directory which is referrenced by
   * {@code System.getProperty("java.io.tmpdir")}.
   *
   * @param parent
   * @param fileName
   *
   * @return
   */
  public static File getTempFile(File parent, String fileName) {
    return new File(new File(getTempDirectory(), parent.getPath()), fileName);
  }

  /**
   * Getting a temp file object in the temporal directory which is referrenced by
   * {@code System.getProperty("java.io.tmpdir")}.
   *
   * @param fileName
   * @return
   */
  public static File getTempFile(String fileName) {
    return new File(getTempDirectory(), fileName);
  }

  /**
   * Getting a file object in the user directory which is referrenced by
   * {@code System.getProperty("user.home")}.
   *
   * @param fileName
   * @return
   */
  public static File getFileInUserDirectory(String fileName) {
    return new File(getUserDirectory(), fileName);
  }

  /**
   * Getting a file object in the user directory which is referrenced by
   * {@code System.getProperty("user.home")}.
   *
   * @param parent
   * @param fileName
   *
   * @return
   */
  public static File getFileInUserDirectory(File parent, String fileName) {
    return new File(new File(getUserDirectory(), parent.getPath()), fileName);
  }

  /**
   * Getting a file reader of {@code fileName}
   *
   * @param fileName
   * @return
   */
  public static FileReader getFileReader(String fileName) {
    return getFileReader(new File(fileName));
  }

  /**
   * Getting a file reader of {@code file}
   *
   * @param fileName
   * @return
   */
  public static FileReader getFileReader(File file) {
    try {
      return new FileReader(file);
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Getting a file writer of {@code fileName}
   *
   * @param fileName
   * @return
   */
  public static FileWriter getFileWriter(String fileName) {
    return getFileWriter(new File(fileName), false);
  }

  /**
   * Getting a file writer of {@code file}
   *
   * @param fileName
   * @return
   */
  public static FileWriter getFileWriter(File file) {
    return getFileWriter(file, false);
  }

  /**
   * Getting a file writer of {@code file} with the option of {@code append}.
   *
   * @param file
   * @param append
   * @return
   */
  public static FileWriter getFileWriter(File file, boolean append) {
    try {
      return new FileWriter(file, append);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static BufferedReader newBufferedReader(Path path) {
    return newBufferedReader(path, StandardCharsets.UTF_8);
  }

  public static BufferedReader newBufferedReader(Path path, Charset cs) {
    try {
      return Files.newBufferedReader(path, cs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options) {
    try {
      return Files.newBufferedWriter(path, cs, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) {
    return newBufferedWriter(path, StandardCharsets.UTF_8, options);
  }

  public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs,
      OpenOption... options) {
    try {
      return Files.write(path, lines, cs, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path write(Path path, Iterable<? extends CharSequence> lines,
      OpenOption... options) {
    return write(path, lines, StandardCharsets.UTF_8, options);
  }

  public static Path write(Path path, String line, Charset cs, OpenOption... options) {
    try {
      return Files.write(path, Arrays.asList(line), cs, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path write(Path path, String line, OpenOption... options) {
    return write(path, Arrays.asList(line), StandardCharsets.UTF_8, options);
  }

  public static byte[] readAllBytes(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path write(Path path, byte[] bytes, OpenOption... options) {
    try {
      return Files.write(path, bytes, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> readAllLines(Path path, Charset cs) {
    try {
      return Files.readAllLines(path, cs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<String> lines(Path path) {
    return lines(path, StandardCharsets.UTF_8);
  }

  public static Stream<String> lines(Path path, Charset cs) {
    try {
      return Files.lines(path, cs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> readAllLines(Path path) {
    return readAllLines(path, StandardCharsets.UTF_8);
  }

  public static List<File> getAllFiles(File dir) {
    List<File> result = new ArrayList<>();
    getAllFilesAux(dir, result);
    return result;
  }

  private static void getAllFilesAux(File dir, List<File> result) {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        getAllFilesAux(file, result);
      } else if (file.isFile()) {
        result.add(file);
      }
    }
  }

  public static void forEachFileInDirInParallel(File dir, Consumer<File> consumer) {
    try {
      ForkJoinPoolUtils.submitWith(() -> Arrays.stream(dir.listFiles()).parallel().forEach(file -> {
        consumer.accept(file);
      })).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static void forEachFileInDir(File dir, Consumer<File> consumer) {
    Arrays.stream(dir.listFiles()).forEach(file -> consumer.accept(file));
  }
}
