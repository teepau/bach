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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/*BODY*/
/** Modular project model. */
public /*STATIC*/ class Project {

  /** Create default project parsing the passed base directory. */
  public static Project of(Path baseDirectory) {
    var main = new Realm("main", false, 0, "src/*/main/java", Map.of(), Map.of());
    var name = Optional.ofNullable(baseDirectory.toAbsolutePath().getFileName());
    return new Project(
        baseDirectory,
        baseDirectory.resolve("bin"),
        name.orElse(Path.of("project")).toString().toLowerCase(),
        Version.parse("0"),
        new Library(baseDirectory.resolve("lib")),
        List.of(main));
  }

  /** Base directory. */
  public final Path baseDirectory;
  /** Target directory. */
  public final Path targetDirectory;
  /** Name of the project. */
  public final String name;
  /** Version of the project. */
  public final Version version;
  /** Library. */
  public final Library library;
  /** Realms. */
  public final List<Realm> realms;

  public Project(
      Path baseDirectory,
      Path targetDirectory,
      String name,
      Version version,
      Library library,
      List<Realm> realms) {
    this.baseDirectory = Util.requireNonNull(baseDirectory, "base");
    this.targetDirectory = Util.requireNonNull(targetDirectory, "targetDirectory");
    this.version = Util.requireNonNull(version, "version");
    this.name = Util.requireNonNull(name, "name");
    this.library = Util.requireNonNull(library, "library");
    this.realms = List.copyOf(Util.requireNonEmpty(realms, "realms"));
  }

  public Target target(Realm realm) {
    return new Target(targetDirectory, realm);
  }

  /** Manage external 3rd-party modules. */
  public static class Library {
    /** List of library paths to external 3rd-party modules. */
    public final List<Path> modulePaths;
    /** Map external 3rd-party module names to their {@code URI}s. */
    public final Function<String, URI> moduleMapper;
    /** Map external 3rd-party module names to their Maven repository. */
    public final Function<String, URI> mavenRepositoryMapper;
    /** Map external 3rd-party module names to their colon-separated Maven Group and Artifact ID. */
    public final UnaryOperator<String> mavenGroupColonArtifactMapper;
    /** Map external 3rd-party module names to their Maven version. */
    public final UnaryOperator<String> mavenVersionMapper;

    public Library(Path lib) {
      this(
          List.of(lib),
          UnmappedModuleException::throwForURI,
          __ -> URI.create("https://repo1.maven.org/maven2"),
          UnmappedModuleException::throwForString,
          UnmappedModuleException::throwForString);
    }

    public Library(
        List<Path> modulePaths,
        Function<String, URI> moduleMapper,
        Function<String, URI> mavenRepositoryMapper,
        UnaryOperator<String> mavenGroupColonArtifactMapper,
        UnaryOperator<String> mavenVersionMapper) {
      this.modulePaths = List.copyOf(Util.requireNonEmpty(modulePaths, "modulePaths"));
      this.moduleMapper = moduleMapper;
      this.mavenRepositoryMapper = mavenRepositoryMapper;
      this.mavenGroupColonArtifactMapper = mavenGroupColonArtifactMapper;
      this.mavenVersionMapper = mavenVersionMapper;
    }
  }

  /** Java module source unit. */
  public static class ModuleUnit {
    /** Path to the backing {@code module-info.java} file. */
    public final Path info;
    /** Paths to the source directories. */
    public final List<Path> sources;
    /** Paths to the resource directories. */
    public final List<Path> resources;
    /** Associated module descriptor, normally parsed from module {@link #info} file. */
    public final ModuleDescriptor descriptor;

    public ModuleUnit(
        Path info, List<Path> sources, List<Path> resources, ModuleDescriptor descriptor) {
      this.info = info;
      this.sources = List.copyOf(sources);
      this.resources = List.copyOf(resources);
      this.descriptor = descriptor;
    }
  }

  /** Multi-release module source unit */
  public static class MultiReleaseUnit extends ModuleUnit {
    /** Feature release number to source path map. */
    public final Map<Integer, Path> releases;
    /** Copy this module descriptor to the root of the generated modular jar. */
    public final int copyModuleDescriptorToRootRelease;

    public MultiReleaseUnit(
        Path info,
        int copyModuleDescriptorToRootRelease,
        Map<Integer, Path> releases,
        List<Path> resources,
        ModuleDescriptor descriptor) {
      super(info, List.copyOf(new TreeMap<>(releases).values()), resources, descriptor);
      this.copyModuleDescriptorToRootRelease = copyModuleDescriptorToRootRelease;
      this.releases = releases;
    }
  }

  /** Main- and test realms. */
  public static class Realm {
    /** Name of the realm. */
    public final String name;
    /** Enable preview features. */
    public final boolean preview;
    /** Java feature release target number. */
    public final int release;
    /** Module source path specifies where to find input source files for multiple modules. */
    public final String moduleSourcePath;
    /** Map of all declared module source unit. */
    public final Map<String, ModuleUnit> units;
    /** Map of compiler-specific module names. */
    public final Map<String, List<String>> modules;
    /** List of required realms. */
    public final List<Realm> realms;

    public Realm(
        String name,
        boolean preview,
        int release,
        String moduleSourcePath,
        Map<String, List<String>> modules,
        Map<String, ModuleUnit> units,
        Realm... realms) {
      this.name = name;
      this.preview = preview;
      this.release = release;
      this.moduleSourcePath = moduleSourcePath;
      this.modules = Map.copyOf(modules);
      this.units = Map.copyOf(units);
      this.realms = List.of(realms);
    }
  }

  /** Collection of directories and other realm-specific assets. */
  public static class Target {
    public final Path directory;
    public final Path modules;

    private Target(Path projectTargetDirectory, Realm realm) {
      this.directory = projectTargetDirectory.resolve("realm").resolve(realm.name);
      this.modules = directory.resolve("modules");
    }
  }
}