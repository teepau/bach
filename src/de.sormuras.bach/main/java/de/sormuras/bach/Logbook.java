/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Markdown;
import de.sormuras.bach.internal.Paths;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** A logbook records textual log entries of all levels and also records tool call results. */
public final class Logbook {

  @Factory
  public static Logbook ofSystem() {
    var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    var logbookThreshold = System.getProperty("bach.logbook.threshold", debug ? "ALL" : "INFO");
    return new Logbook(System.out::println, Level.valueOf(logbookThreshold));
  }

  private final LocalDateTime created = LocalDateTime.now(ZoneOffset.UTC);
  private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();
  private final Queue<Result> results = new ConcurrentLinkedQueue<>();
  private final Consumer<String> printer;
  private final Level threshold;

  public Logbook(Consumer<String> printer, Level threshold) {
    this.printer = printer;
    this.threshold = threshold;
  }

  public Level threshold() {
    return threshold;
  }

  @Factory(Kind.SETTER)
  public Logbook printer(Consumer<String> printer) {
    return new Logbook(printer, threshold);
  }

  @Factory(Kind.SETTER)
  public Logbook threshold(Level threshold) {
    return new Logbook(printer, threshold);
  }

  public boolean isOn(Level level) {
    return level.getSeverity() >= threshold.getSeverity();
  }

  public boolean isOff(Level level) {
    return level.getSeverity() < threshold.getSeverity();
  }

  public String log(Level level, String format, Object... arguments) {
    return log(level, String.format(format, arguments));
  }

  public String log(Level level, String text) {
    return log(level, text, true);
  }

  private String log(Level level, String text, boolean add) {
    if (text.isEmpty()) return text;
    if (text.equals("\n")) {
      if (isOn(level)) print();
      return text;
    }
    var thread = Thread.currentThread().getId();
    var entry = new Entry(thread, level, text);
    if (add) entries.add(entry);
    if (isOff(level)) return text;
    synchronized (entries) {
      var all = threshold == Level.ALL;
      var warning = level.getSeverity() >= Level.WARNING.getSeverity();
      print(all ? entry.toString() : warning ? level.getName() + ' ' + text : text);
    }
    return text;
  }

  void print() {
    printer.accept("");
  }

  void print(String text) {
    printer.accept(text);
  }

  void print(String format, Object... args) {
    print(String.format(format, args));
  }

  Result add(Call<?> call, String out, String err, Duration duration, int code) {
    var thread = Thread.currentThread().getId();
    var tool = call.name();
    var args = call.toStringArray();
    var result = new Result(thread, tool, args, out, err, duration, code);
    results.add(result);
    log(Level.TRACE, out, false);
    log(Level.TRACE, err, false);
    return result;
  }

  public List<String> toMarkdown(Project project) {
    var md = new ArrayList<String>();
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    md.add("# Logbook of " + project.toNameAndVersion());
    md.add("");
    md.add("- Created at " + formatter.format(created));
    md.add("- Written at " + formatter.format(LocalDateTime.now(ZoneOffset.UTC)));
    md.addAll(projectModules(project.base().modules("")));
    md.addAll(projectDescription(project));
    md.addAll(toToolCallOverview());
    md.addAll(toToolCallDetails());
    md.addAll(toLogbookEntries());
    md.add("");
    md.add("## Thanks for using Bach.java " + Bach.VERSION);
    md.add("");
    md.add("Support its development at <https://github.com/sponsors/sormuras>");
    return md;
  }

  private List<String> projectModules(Path directory) {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Modules");
    md.add("");
    if (!Files.isDirectory(directory)) {
      md.add(String.format("Directory `%s` doesn't exist or isn't a directory.", directory));
      return md;
    }
    var files = Paths.list(directory, Paths::isJarFile);
    md.add("- directory: " + directory.toUri());
    md.add("- files: " + files.size());
    if (files.isEmpty()) return md;
    md.add("");
    md.add("### Module API");
    md.add("");
    md.add("| Name | Version | Exports | Provides | Main Class |");
    md.add("|------|---------|---------|----------|------------|");
    for (var file : files) {
      var descriptor = ModuleFinder.of(file).findAll().iterator().next().descriptor();
      var module = descriptor.name();
      var version = descriptor.version().map(Object::toString).orElse("-");
      var exports = Markdown.join(descriptor.exports());
      var provides = Markdown.join(descriptor.provides());
      var main = descriptor.mainClass().map(Object::toString).orElse("-");
      md.add(String.format("|`%s`|%s|%s|%s|`%s`|", module, version, exports, provides, main));
    }
    md.add("");
    md.add("### Modular JAR");
    md.add("");
    md.add("| Size [Bytes] | File Name |");
    md.add("|-------------:|:----------|");
    for (var file : files) {
      var size = Paths.size(file);
      var name = file.getFileName();
      md.add(String.format("|%,d|%s", size, name));
    }
    return md;
  }

  private List<String> projectDescription(Project project) {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Project");
    md.add("");
    md.add("- name: " + project.name());
    md.add("- version: " + project.version());
    md.add("");
    md.add("### Project Descriptor");
    md.add("");
    md.add("```text");
    md.addAll(project.toStrings());
    md.add("```");
    return md;
  }

  private List<String> toToolCallOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Overview");
    md.add("");
    md.add("|    |Thread| Duration |Tool|Arguments");
    md.add("|----|-----:|---------:|----|---------");
    for (var call : results) {
      var kind = ' ';
      var thread = call.thread;
      var millis = toString(call.duration);
      var tool = "[" + call.tool + "](#" + call.toDetailedCaption() + ")";
      var arguments = "`" + String.join(" ", call.args) + "`";
      var row = String.format("|%4c|%6X|%10s|%s|%s", kind, thread, millis, tool, arguments);
      md.add(row);
    }
    return md;
  }

  private List<String> toToolCallDetails() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Details");
    md.add("");
    md.add(String.format("Recorded %d tool call results.", results.size()));
    for (var call : results) {
      md.add("");
      md.add("### " + call.toDetailedCaption());
      md.add("");
      md.add("- tool = `" + call.tool + '`');
      md.add("- args = `" + String.join(" ", call.args) + '`');
      if (!call.out.isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(call.out);
        md.add("```");
      }
      if (!call.err.isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(call.err);
        md.add("```");
      }
    }
    return md;
  }

  private List<String> toLogbookEntries() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## All Entries");
    md.add("");
    md.add("```text");
    for (var entry : entries) md.add(entry.toString().replace('\t', ' '));
    md.add("```");
    return md;
  }

  Path write(Bach bach) {
    var base = bach.project().base();
    var markdownFile = base.workspace("logbook.md");
    var markdownLines = toMarkdown(bach.project());
    try {
      Paths.createDirectories(base.workspace());
      Files.write(markdownFile, markdownLines);

      var formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
      var timestamp = formatter.format(created);
      var logbooks = Paths.createDirectories(base.workspace("logbooks"));
      Files.write(logbooks.resolve("logbook-" + timestamp + ".md"), markdownLines);
    } catch (Exception exception) {
      var message = log(Level.ERROR, "Write logbook failed: %s", exception);
      if (bach.is(Flag.FAIL_FAST)) throw new AssertionError(message, exception);
    }
    return markdownFile;
  }

  void printSummaryAndCheckErrors(Bach bach, Consumer<String> errorPrinter) {
    if (bach.is(Flag.SUMMARY_WITH_TOOL_CALL_OVERVIEW)) {
      var lineLength = bach.is(Flag.SUMMARY_LINES_UNCUT) || isOn(Level.ALL) ? 0xFFFF : 120;
      printSummaryOfToolCallResults(lineLength);
    }
    if (bach.is(Flag.SUMMARY_WITH_MAIN_MODULE_OVERVIEW)) {
      var modules = bach.project().base().modules("");
      printSummaryOfModules(modules, isOn(Level.INFO), isOn(Level.DEBUG));
    }

    var errors = results.stream().filter(Result::isError).collect(Collectors.toList());
    if (errors.isEmpty()) return;

    errors.forEach(error -> error.toStrings().forEach(errorPrinter));
    var message = "Detected " + errors.size() + " error" + (errors.size() != 1 ? "s" : "");
    if (bach.is(Flag.FAIL_ON_ERROR)) throw new AssertionError(message);
  }

  void printSummaryOfToolCallResults(int maxLineLength) {
    var format = "%10s %10s %s";
    print();
    print(String.format("Tool Call Overview contains %s distinct calls", results.size()));
    print(String.format(format, "Duration", "Name", "Arguments"));
    var total = Duration.ZERO;
    for (var call : results) {
      var millis = toString(call.duration);
      var tool = call.tool;
      var args = String.join(" ", call.args);
      var line = String.format(format, millis, tool, args);
      print(line.length() <= maxLineLength ? line : line.substring(0, maxLineLength - 3) + "...");
      total = total.plus(call.duration);
    }
    print(String.format("%10s %10s", "  --------", "----------"));
    print(String.format("%10s %10s", toString(total), results.size() + " calls"));
  }

  public void printSummaryOfModules(Path directory) {
    printSummaryOfModules(directory, isOn(Level.DEBUG), isOn(Level.TRACE));
  }

  void printSummaryOfModules(Path directory, boolean listFiles, boolean describeModule) {
    if (!Files.isDirectory(directory)) {
      log(Level.WARNING, "Can not print modules summary -- not a directory: %s", directory);
      return;
    }
    var name = Optional.ofNullable(directory.getFileName()).map(Path::toString).orElse("");
    var uri = directory.toUri().toString();
    var files = Paths.list(directory, Files::isRegularFile);
    var s = files.size() == 1 ? "" : "s";
    print();
    print(String.format("Directory %s (%s) contains %d file%s", name, uri, files.size(), s));
    if (!listFiles) return;
    try {
      for (var file : files) {
        print(String.format("- %s with %,d bytes", file.getFileName(), Files.size(file)));
        if (!describeModule) continue;
        if (!Paths.isJarFile(file)) continue;
        var string = new StringWriter();
        var writer = new PrintWriter(string);
        var jar = ToolProvider.findFirst("jar").orElseThrow();
        jar.run(writer, writer, "--describe-module", "--file", file.toString());
        var trim = string.toString().trim().replace(uri, "${DIRECTORY}");
        print(trim.replaceAll("(?m)^", "\t"));
      }
    } catch (Exception e) {
      throw new AssertionError("Analyzing JAR files failed", e);
    }
  }

  public static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  /** A textual log entry. */
  static final class Entry {
    private final long thread;
    private final Level level;
    private final String text;

    Entry(long thread, Level level, String text) {
      this.thread = thread;
      this.level = level;
      this.text = text;
    }

    @Override
    public String toString() {
      return String.format("%-7s %6X| %s", level, thread, text);
    }
  }

  /** A tool call result. */
  static final class Result {
    private final long thread;
    private final String tool;
    private final String[] args;
    private final String out;
    private final String err;
    private final Duration duration;
    private final int code;

    Result(
        long thread,
        String tool,
        String[] args,
        String out,
        String err,
        Duration duration,
        int code) {
      this.thread = thread;
      this.tool = tool;
      this.args = args;
      this.out = out;
      this.err = err;
      this.duration = duration;
      this.code = code;
    }

    public boolean isError() {
      return code != 0;
    }

    public String toDetailedCaption() {
      return tool + '-' + Integer.toHexString(System.identityHashCode(this));
    }

    public List<String> toStrings() {
      var message = new ArrayList<String>();
      message.add("");
      message.add('\t' + tool + ' ' + String.join(" ", args));
      if (!out.isEmpty()) {
        message.add("");
        out.lines().forEach(line -> message.add("\t\t" + line));
      }
      if (!err.isEmpty()) {
        message.add("");
        err.lines().forEach(line -> message.add("\t\t" + line));
      }
      message.add("");
      return message;
    }
  }
}
