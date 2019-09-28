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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Project;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestProjectTests {

  @Test
  void jigsawGreetings(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "jigsaw-greetings");
    var greetings =
            new Project.ModuleUnit(
                    Project.ModuleInfoReference.of(base.resolve("src/com.greetings/module-info.java")),
                    List.of(base.resolve("src/com.greetings")),
                    List.of());
    var main =
            new Project.Realm(
                    "main",
                    false,
                    0,
                    String.join(File.separator, base.toString(), "src"),
                    Map.of("jigsaw", List.of("com.greetings")),
                    Map.of("com.greetings", greetings));
    var library = new Project.Library(temp.resolve("lib"));
    var project =
            new Project(
                    base,
                    temp,
                    "jigsaw-greetings",
                    ModuleDescriptor.Version.parse("0"),
                    library,
                    List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);
  }

  @Test
  void jigsawWorld(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "jigsaw-world");
    var greetings =
            new Project.ModuleUnit(
                    Project.ModuleInfoReference.of(base.resolve("src/main/com.greetings/module-info.java")),
                    List.of(base.resolve("src/main/com.greetings")),
                    List.of());
    var astro =
            new Project.ModuleUnit(
                    Project.ModuleInfoReference.of(base.resolve("src/main/org.astro/module-info.java")),
                    List.of(base.resolve("src/main/org.astro")),
                    List.of());
    var main =
            new Project.Realm(
                    "main",
                    false,
                    0,
                    String.join(File.separator, base.toString(), "src", "main"),
                    Map.of("jigsaw", List.of("com.greetings", "org.astro")),
                    Map.of("com.greetings", greetings, "org.astro", astro));
    var library = new Project.Library(temp.resolve("lib"));
    var project =
            new Project(
                    base,
                    temp,
                    "jigsaw-greetings",
                    ModuleDescriptor.Version.parse("0"),
                    library,
                    List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);
  }

  @Test
  void multiReleaseMultiModule(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "multi-release-multi-module");
    var a =
        new Project.MultiReleaseUnit(
            Project.ModuleInfoReference.of(base.resolve("src/a/main/java-9/module-info.java")),
            9,
            Map.of(
                8, base.resolve("src/a/main/java-8"),
                9, base.resolve("src/a/main/java-9"),
                11, base.resolve("src/a/main/java-11")),
            List.of());
    var b =
        new Project.ModuleUnit(
            Project.ModuleInfoReference.of(base.resolve("src/b/main/java/module-info.java")),
            List.of(base.resolve("src/b/main/java")),
            List.of());
    var c =
        new Project.MultiReleaseUnit(
            Project.ModuleInfoReference.of(base.resolve("src/c/main/java-9/module-info.java")),
            9,
            Map.of(
                8, base.resolve("src/c/main/java-8"),
                9, base.resolve("src/c/main/java-9"),
                10, base.resolve("src/c/main/java-10"),
                11, base.resolve("src/c/main/java-11")),
            List.of());
    var d =
        new Project.ModuleUnit(
            Project.ModuleInfoReference.of(base.resolve("src/d/main/java/module-info.java")),
            List.of(base.resolve("src/d/main/java")),
            List.of());
    var main =
        new Project.Realm(
            "main",
            false,
            0,
            String.join(
                File.pathSeparator,
                String.join(File.separator, base.toString(), "src", "*", "main", "java"),
                String.join(File.separator, base.toString(), "src", "*", "main", "java-9")),
            Map.of("hydra", List.of("a", "c"), "jigsaw", List.of("b", "d")),
            Map.of("a", a, "b", b, "c", c, "d", d));
    var library = new Project.Library(temp);
    var project =
        new Project(
            base,
            temp,
            "multi-release-multi-module",
            ModuleDescriptor.Version.parse("0"),
            library,
            List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);

    var target = project.target(main);
    for (var unit : main.units.values()) {
      assertTrue(Files.exists(target.modularJar(unit)), unit.info.toString());
      assertTrue(Files.exists(target.sourcesJar(unit)), unit.info.toString());
    }
  }

  @Test
  void requiresAsm(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "requires-asm");
    var a =
        new Project.ModuleUnit(
            Project.ModuleInfoReference.of(base.resolve("src/a/main/java/module-info.java")),
            List.of(base.resolve("src/a/main/java")),
            List.of());
    assertEquals(
        ModuleDescriptor.newModule("a")
            .requires(Set.of(), "org.objectweb.asm", ModuleDescriptor.Version.parse("7.1"))
            .build(),
        a.info.descriptor());
    var main =
        new Project.Realm(
            "main",
            false,
            0,
            String.join(File.separator, base.toString(), "src", "*", "main", "java"),
            Map.of("jigsaw", List.of("a")),
            Map.of("a", a));
    var library = new Project.Library(temp.resolve("lib"));
    var project =
        new Project(
            base,
            temp,
            "requires-asm",
            ModuleDescriptor.Version.parse("0"),
            library,
            List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);
  }
}
