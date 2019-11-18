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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectTests {
  @Test
  void createSimpleProjectAndVerifyItsComponents() {
    var base = Path.of("simple");
    var realm = new Realm("realm", Set.of(), List.of(), List.of());
    var unit = new Unit(realm, ModuleDescriptor.newModule("unit").version("1").build(), List.of());
    var structure = new Structure(Folder.of(base), List.of(realm), List.of(unit));
    var project = new Project("simple", Version.parse("0"), structure);
    assertEquals("simple", project.name());
    assertEquals("0", project.version().toString());
    var folder = project.folder();
    assertEquals(base, folder.base());
    assertEquals(base.resolve(".bach/out"), folder.out());
    assertEquals(base.resolve(".bach/out/README.md"), folder.out("README.md"));
    assertEquals(base.resolve(".bach/out/log"), folder.log());
    assertEquals(base.resolve(".bach/out/log/123.log"), folder.log("123.log"));
    assertEquals(base.resolve(".bach/out/realm"), folder.realm("realm"));
    assertEquals(base.resolve(".bach/out/realm/classes"), folder.realm("realm", "classes"));
    assertEquals(base.resolve(".bach/out/realm/modules"), folder.modules("realm"));
    assertEquals(base.resolve(".bach/out/realm/modules/m.jar"), folder.modules("realm", "m.jar"));

    assertEquals(base.resolve("lib"), folder.lib());
    assertSame(structure, project.structure());
    assertSame(unit, project.unit("realm", "unit").orElseThrow());
    assertEquals("1", project.version(unit).toString());
    assertEquals(base.resolve(".bach/out/realm/modules/unit-1.jar"), project.modularJar(unit));
  }

  @Test
  void executeRuntimeExceptionThrowingTaskIsReportedAsAnError() {
    var exception = new RuntimeException("!");
    class RuntimeExceptionThrowingTask implements Task {
      @Override
      public void execute(Bach bach) {
        throw exception;
      }
    }

    var structure = new Structure(Folder.of(), List.of(), List.of());
    var project = new Project("zero", Version.parse("0"), structure);
    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(new RuntimeExceptionThrowingTask()));
    assertSame(exception, error.getCause());
    assertEquals("Task failed to execute: java.lang.RuntimeException: !", error.getMessage());
  }

  @Test
  void executeNonZeroToolProviderIsReportedAsAnError() {
    var structure = new Structure(Folder.of(), List.of(), List.of());
    var project = new Project("zero", Version.parse("0"), structure);
    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(new Call("javac", "*")));
    assertEquals(
        "Call exited with non-zero status code: 2 <- Call{name='javac', arguments=[*]}",
        error.getMessage());
    assertLinesMatch(List.of("Bach.java 2.0-ea initialized.", "| javac(*)"), log.lines());
    assertLinesMatch(
        List.of(
            "error: invalid flag: *",
            "Usage: javac <options> <source files>",
            "use --help for a list of possible options"),
        log.errors());
  }

  @Test
  void buildProjectInEmptyDirectoryThrowsError(@TempDir Path temp) {
    var main = new Realm("main", Set.of(), List.of(), List.of());
    var unit = new Unit(main, ModuleDescriptor.newModule("unit").build(), List.of());
    var structure = new Structure(Folder.of(temp), List.of(main), List.of(unit));
    var project = new Project("empty", Version.parse("0"), structure);

    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(Task.build()));
    assertEquals("Base directory is empty: " + temp.toUri(), error.getMessage());
  }
}
