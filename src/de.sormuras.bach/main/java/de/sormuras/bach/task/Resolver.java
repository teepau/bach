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

package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.util.Maven;
import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Uris;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

class Resolver {

  private final Log log;
  private final Project project;
  private final Path lib;
  private final Library library;

  Resolver(Bach bach) {
    this.log = bach.getLog();
    this.project = bach.getProject();
    this.lib = project.folder().lib();
    this.library = project.structure().library();
  }

  public void resolve() throws Exception {
    var systemModulesSurvey = Modules.Survey.of(ModuleFinder.ofSystem());
    var missing = findMissingModules(systemModulesSurvey);
    if (missing.isEmpty()) {
      log.debug("All required modules are locatable.");
      return;
    }

    var downloader = new Downloader();
    do {
      downloader.loadMissingModules(missing);
      missing.clear();
      var libraryModulesSurvey = Modules.Survey.of(ModuleFinder.of(lib));
      libraryModulesSurvey.putAllRequiresTo(missing);
      libraryModulesSurvey.declaredModules().forEach(missing::remove);
      systemModulesSurvey.declaredModules().forEach(missing::remove);
    } while (!missing.isEmpty());
  }

  Map<String, Set<Version>> findMissingModules(Modules.Survey systemModulesSurvey) {
    var units = project.structure().units().stream().map(Unit::info).collect(Collectors.toList());
    var projectModulesSurvey = Modules.Survey.of(units);
    var libraryModulesSurvey = Modules.Survey.of(ModuleFinder.of(lib));

    log.debug("Project modules survey of %s unit(s) -> %s", units.size(), units);
    log.debug("  declared -> " + projectModulesSurvey.declaredModules());
    log.debug("  requires -> " + projectModulesSurvey.requiredModules());
    log.debug("Library modules survey of -> %s", lib.toUri());
    log.debug("  declared -> " + libraryModulesSurvey.declaredModules());
    log.debug("  requires -> " + libraryModulesSurvey.requiredModules());
    log.debug("System contains %d modules.", systemModulesSurvey.declaredModules().size());

    var missing = new TreeMap<String, Set<Version>>();
    projectModulesSurvey.putAllRequiresTo(missing);
    libraryModulesSurvey.putAllRequiresTo(missing);
    if (library.addMissingJUnitTestEngines()) Library.addJUnitTestEngines(missing);
    if (library.addMissingJUnitPlatformConsole()) Library.addJUnitPlatformConsole(missing);
    projectModulesSurvey.declaredModules().forEach(missing::remove);
    libraryModulesSurvey.declaredModules().forEach(missing::remove);
    systemModulesSurvey.declaredModules().forEach(missing::remove);
    return missing;
  }

  private class Downloader {
    HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    Uris uris = new Uris(log, http);
    Maven maven = createMaven(uris);
    Properties moduleUriProperties = load(new Properties(), lib.resolve("module-uri.properties"));

    Downloader() throws Exception {}

    void loadMissingModules(Map<String, Set<Version>> missing) throws Exception {
      log.debug("Loading modules: %s", missing);
      for (var entry : missing.entrySet()) {
        var module = entry.getKey();
        var direct = moduleUriProperties.getProperty(module);
        if (direct != null) {
          var uri = URI.create(direct);
          var jar = lib.resolve(module + ".jar");
          uris.copy(uri, jar, StandardCopyOption.COPY_ATTRIBUTES);
          continue;
        }
        var versions = entry.getValue();
        var version = singleton(versions).map(Object::toString).orElse(maven.version(module));
        var ga = maven.lookup(module, version).split(":");
        var group = ga[0];
        var artifact = ga[1];
        var repository = library.mavenRepositoryMapper().apply(group, version);
        uris.copy(
            maven.toUri(repository, group, artifact, version),
            lib.resolve(module + '-' + version + ".jar"),
            StandardCopyOption.COPY_ATTRIBUTES);
      }
    }

    Maven createMaven(Uris uris) throws Exception {
      var cache =
          Files.createDirectories(
              Path.of(System.getProperty("user.home")).resolve(".bach/modules"));
      var artifactPath =
          uris.copy(
              URI.create("https://github.com/sormuras/modules/raw/master/module-maven.properties"),
              cache.resolve("module-maven.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);

      var artifactLookup =
          new Maven.Lookup(
              library.mavenGroupColonArtifactMapper(),
              map(load(new Properties(), lib.resolve("module-maven.properties"))),
              map(load(new Properties(), artifactPath)));

      var versionPath =
          uris.copy(
              URI.create(
                  "https://github.com/sormuras/modules/raw/master/module-version.properties"),
              cache.resolve("module-version.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);

      var versionLookup =
          new Maven.Lookup(
              library.mavenVersionMapper(),
              map(load(new Properties(), lib.resolve("module-version.properties"))),
              map(load(new Properties(), versionPath)));
      return new Maven(log, uris, artifactLookup, versionLookup);
    }

    Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (Exception e) {
          throw new RuntimeException("Reading properties failed: " + path, e);
        }
      }
      return properties;
    }

    /**
     * Convert all {@link String}-based properties in an instance of {@code Map<String, String>}.
     */
    Map<String, String> map(Properties properties) {
      var map = new HashMap<String, String>();
      for (var name : properties.stringPropertyNames()) {
        map.put(name, properties.getProperty(name));
      }
      return Map.copyOf(map);
    }

    <T> Optional<T> singleton(Collection<T> collection) {
      if (collection.isEmpty()) {
        return Optional.empty();
      }
      if (collection.size() != 1) {
        throw new IllegalStateException("Too many elements: " + collection);
      }
      return Optional.of(collection.iterator().next());
    }
  }
}