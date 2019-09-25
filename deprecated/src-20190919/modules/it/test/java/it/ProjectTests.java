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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectTests {

  private static final Path PROJECTS = Path.of("src/test-project");

  private static Stream<Path> projects() throws Exception {
    try (var entries = Files.list(PROJECTS)) {
      return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
    }
  }

  @ParameterizedTest
  @MethodSource("projects")
  void clean(Path base) {
    var bach = new Probe(base);
    assertDoesNotThrow(bach::clean, "bach::clean failed: " + base);
    assertLinesMatch(List.of(), bach.lines());
  }

  @ParameterizedTest
  @MethodSource("projects")
  void help(Path base) {
    var bach = new Probe(base);
    assertDoesNotThrow(bach::help, "bach::help failed: " + base);
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            ">> METHODS AND DECLARING CLASSES >>",
            "Provided tools",
            ">> NAMES OF TOOLS >>"),
        bach.lines());
  }

  @ParameterizedTest
  @MethodSource("projects")
  void info(Path base) {
    var bach = new Probe(base);
    assertDoesNotThrow(bach::info, "bach::info failed: " + base);
    assertLinesMatch(
        List.of(
            "Bach \\(" + Bach.VERSION + ".*\\)",
            "project name = " + base.getFileName().toString(),
            "project version = 0",
            "base directory = '" + base + "' -> " + base.toUri(),
            "source directories = [" + base.resolve("src") + "]",
            "target directory = '" + base.resolve("bin") + "'",
            "library paths = [" + base.resolve("lib") + "]"
            ),
        bach.lines());
  }

  @Nested
  class Empty {

    @Test
    void build() {
      var base = PROJECTS.resolve("empty");
      var e = assertThrows(Validation.Error.class, () -> new Probe(base).build());
      assertEquals("expected that base contains a directory: " + base.toUri(), e.getMessage());
    }
  }

  @Nested
  class MissingModule {

    @Test
    void build() {
      var probe = new Probe(PROJECTS.resolve("missing-module"));
      assertThrows(IllegalStateException.class, probe::build);
      assertLinesMatch(List.of(">> BUILD >>", "Loading missing modules: {b=[]}"), probe.lines());
    }
  }

  @Nested
  class RequiresAsm {

    @Test
    void build(@TempDir Path work) {
      var lib = work.resolve("lib");
      var probe =
          new Probe(
              new Configuration(PROJECTS.resolve("requires-asm")) {
                @Override
                public Path getWorkspaceDirectory() {
                  return work;
                }

                @Override
                public List<Path> getLibraryPaths() {
                  return List.of(lib);
                }
              });
      assertDoesNotThrow(probe::build, probe.out.toString());
      assertLinesMatch(
          List.of(
              ">> BUILD >>",
              "Loading missing modules: {org.objectweb.asm=[7.1]}",
              lib.resolve("org.objectweb.asm-7.1.jar")
                  + " <- https://repo1.maven.org/maven2/org/ow2/asm/asm/7.1/asm-7.1.jar",
              ">> COMPILE >>"),
          probe.lines());
    }
  }
}