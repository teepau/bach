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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import de.sormuras.bach.api.Paths;
import de.sormuras.bach.api.Scanner;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.Tree;

class BaseProjectTests {

  @Nested
  class MultiRelease {

    @Test
    void build(@TempDir Path temp) {
      var name = "MultiRelease";
      var base = Path.of("src","test.base", "test", "resources", "jdk", name);
      var paths = new Paths(base, temp.resolve("out"), temp.resolve("lib"));
      var project = new Scanner(paths).scan().build();

      var log = new Log();
      var bach = new Bach(log, true, false);
      var summary = bach.build(project);
      var files = Tree.walk(temp);
      try {
        summary.assertSuccessful();
        assertTrue(files.contains("out/Build.java"));
        assertTrue(files.contains("out/documentation/" + name + "-javadoc.jar"));
        assertTrue(files.contains("out/summary.md"));
      } catch (Throwable throwable) {
        files.forEach(System.err::println);
        System.err.println();
        summary.toMarkdown().forEach(System.err::println);
        throw throwable;
      }
    }
  }
}