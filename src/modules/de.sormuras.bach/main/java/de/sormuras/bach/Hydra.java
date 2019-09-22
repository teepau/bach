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

package de.sormuras.bach;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/*BODY*/
/** Multi-release module compiler. */
public /*STATIC*/ class Hydra {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;

  private final Path modulesDirectory;
  private final Path classesDirectory;

  public Hydra(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;

    var targetDirectory = project.targetDirectory.resolve("realm").resolve(realm.name);
    this.modulesDirectory = targetDirectory.resolve("modules");
    var hydraDirectory = modulesDirectory.resolve("hydra");
    this.classesDirectory = hydraDirectory.resolve("classes");
  }

  public List<Command> compile(Collection<String> modules) {
    bach.log("Generating commands for %s realm multi-release modules(s): %s", realm.name, modules);
    var commands = new ArrayList<Command>();
    for (var module : modules) {
      var unit = (Project.MultiReleaseUnit) realm.modules.get(module);
      compile(commands, unit);
    }
    return List.copyOf(commands);
  }

  private void compile(List<Command> commands, Project.MultiReleaseUnit unit) {
    var sorted = new TreeSet<>(unit.releases.keySet());
    int base = sorted.first();
    bach.log("Base feature release number is: %d", base);
    for (int release : sorted) {
      commands.add(compileRelease(unit, base, release));
    }
    commands.add(jarModule(unit));
    commands.add(jarSources(unit));
  }

  private Command compileRelease(Project.MultiReleaseUnit unit, int base, int release) {
    var source = unit.releases.get(release);
    var module = unit.descriptor.name();
    var baseClasses =
        classesDirectory.resolve(unit.releases.get(base).getFileName()).resolve(module);
    var destination = classesDirectory.resolve(source.getFileName());
    var javac = new Command("javac").addIff(false, "-verbose").add("--release", release);
    if (Util.isModuleInfo(source.resolve("module-info.java"))) {
      javac.add("-d", destination);
      javac.add("--module-version", project.version);
      javac.add("--module-path", project.library.modulePaths);
      javac.add("--module-source-path", realm.moduleSourcePath);
      if (base != release) {
        javac.add("--patch-module", module + '=' + baseClasses);
      }
      javac.add("--module", module);
    } else {
      javac.add("-d", destination.resolve(module));
      var classPath = new ArrayList<Path>();
      if (base != release) {
        classPath.add(baseClasses);
      }
      for (var path : Util.findExisting(project.library.modulePaths)) {
        if (Util.isJarFile(path)) {
          classPath.add(path);
          continue;
        }
        Util.list(path, Util::isJarFile).forEach(jar -> classPath.add(path.resolve(jar)));
      }
      javac.add("--class-path", classPath);
      javac.addEach(Util.find(List.of(source), Util::isJavaFile));
    }
    return javac;
  }

  private Command jarModule(Project.MultiReleaseUnit unit) {
    var releases = new ArrayDeque<>(new TreeSet<>(unit.releases.keySet()));
    var module = unit.descriptor.name();
    var version = unit.descriptor.version();
    var file = module + "-" + version.orElse(project.version);
    var base = unit.releases.get(releases.pop()).getFileName();
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", modulesDirectory.resolve(file + ".jar"))
            .addIff(bach.verbose(), "--verbose")
            .add("-C", classesDirectory.resolve(base).resolve(module))
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var release : releases) {
      var path = unit.releases.get(release).getFileName();
      var classes = classesDirectory.resolve(path).resolve(module);
      if (unit.copyModuleDescriptorToRootRelease == release) {
        jar.add("-C", classes);
        jar.add("module-info.class");
      }
      jar.add("--release", release);
      jar.add("-C", classes);
      jar.add(".");
    }
    return jar;
  }

  private Command jarSources(Project.MultiReleaseUnit unit) {
    var releases = new ArrayDeque<>(new TreeMap<>(unit.releases).entrySet());
    var module = unit.descriptor.name();
    var version = unit.descriptor.version();
    var file = module + "-" + version.orElse(project.version);
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", modulesDirectory.resolve(file + "-sources.jar"))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", releases.removeFirst().getValue())
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var release : releases) {
      jar.add("--release", release.getKey());
      jar.add("-C", release.getValue());
      jar.add(".");
    }
    return jar;
  }
}
