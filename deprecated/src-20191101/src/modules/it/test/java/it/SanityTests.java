/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import de.sormuras.bach.Bach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SanityTests {

  private static final String BACH = "src/bach/Bach.java";

  @Test
  @DisplayName("jshell Bach.jsh")
  void openAndRunInJShellReturnsZero() throws Exception {
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-J-ea");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var process = builder.start();
    var source =
        String.format(
            "/open %s\n"
                + "var code = 0\n"
                + "try {\n"
                + "  new Bach().main(List.of(\"version\"));\n"
                + "} catch (Throwable throwable) {\n"
                + "  throwable.printStackTrace();\n"
                + "  code = 1;\n"
                + "}\n"
                + "/exit code\n",
            BACH);
    process.getOutputStream().write(source.getBytes());
    process.getOutputStream().flush();
    process.waitFor(30, TimeUnit.SECONDS);
    assertStreams(List.of(), process);
    assertEquals(0, process.exitValue(), process.toString());
  }

  @Test
  @DisplayName("java Bach.java")
  void compileAndRunWithJavaReturnsZero() throws Exception {
    var builder = new ProcessBuilder("java");
    builder.command().add("-ea");
    builder.command().add(BACH);
    builder.command().add("version");
    var process = builder.start();
    process.waitFor(20, TimeUnit.SECONDS);
    assertStreams(List.of(), process);
    assertEquals(0, process.exitValue(), process.toString());
  }

  static void assertStreams(List<String> expectedErrorStreamLines, Process process) {
    var out = lines(process.getInputStream());
    var err = lines(process.getErrorStream());
    try {
      assertLinesMatch(List.of(Bach.VERSION), out);
      assertLinesMatch(expectedErrorStreamLines, err);
    } catch (AssertionError e) {
      var msg = String.join("\n", err) + String.join("\n", out);
      System.err.println(msg);
      throw e;
    }
  }

  static List<String> lines(InputStream stream) {
    try (var reader = new BufferedReader(new InputStreamReader(stream))) {
      return reader.lines().collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("Reading from stream failed!", e);
    }
  }
}