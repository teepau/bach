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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.util.Strings;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Tree;

class BuildTests {

  @Test
  void buildEmptyDirectoryFails(@TempDir Path temp) {
    var run = new Run(temp);
    var error = assertThrows(AssertionError.class, () -> run.bach().build(API.emptyProject()));
    assertEquals("Build project empty 0 (Task) failed", error.getMessage());
    assertEquals("project validation failed: no unit present", error.getCause().getMessage());
    assertLinesMatch(
        List.of(
            ">> BUILD >>",
            "java.lang.IllegalStateException: project validation failed: no unit present",
            ">> ERROR >>"),
        run.log().lines());
  }

  @Test
  void buildJigsawQuickStartGreetings(@TempDir Path temp) throws Exception {
    var base = temp.resolve("greetings");
    var workspace = Workspace.of(base);
    var example = Projects.exampleOfJigsawQuickStartGreetings(workspace);
    example.deploy(base);
    assertLinesMatch(
        List.of(
            "src",
            ">> PATH>>",
            "src/com.greetings/com/greetings/Main.java",
            "src/com.greetings/module-info.java"),
        Tree.walk(base));

    var run = new Run(workspace.base());
    run.bach().build(example.project());

    run.log().assertThatEverythingIsFine();
    var N = Runtime.version().feature();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), run.log().lines());
    assertLinesMatch(
        List.of(
            ".bach",
            ">> PATHS >>",
            ".bach/workspace/classes/" + N + "/com.greetings/com/greetings/Main.class",
            ".bach/workspace/classes/" + N + "/com.greetings/module-info.class",
            ">> PATHS >>",
            ".bach/workspace/summary.md",
            ">> PATHS >>"),
        Tree.walk(base),
        Strings.text(run.log().lines()));
  }
}
