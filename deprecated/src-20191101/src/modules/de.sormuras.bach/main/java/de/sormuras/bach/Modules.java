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

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*BODY*/
/** Static helper for modules and their friends. */
public /*STATIC*/ class Modules {

  private static final Pattern MAIN_CLASS = Pattern.compile("//\\s*(?:--main-class)\\s+([\\w.]+)");

  private static final Pattern MODULE_NAME_PATTERN =
      Pattern.compile(
          "(?:module)" // key word
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
              + "\\s*\\{"); // end marker

  private static final Pattern MODULE_REQUIRES_PATTERN =
      Pattern.compile(
          "(?:requires)" // key word
              + "(?:\\s+[\\w.]+)?" // optional modifiers
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
              + "\\s*;"); // end marker

  private static final Pattern MODULE_PROVIDES_PATTERN =
      Pattern.compile(
          "(?:provides)" // key word
              + "\\s+([\\w.]+)" // service name
              + "\\s+with" // separator
              + "\\s+([\\w.,\\s]+)" // comma separated list of type names
              + "\\s*;"); // end marker

  private Modules() {}

  /** Module descriptor parser. */
  public static ModuleDescriptor describe(String source) {
    // "module name {"
    var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
    if (!nameMatcher.find()) {
      throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
    }
    var name = nameMatcher.group(1).trim();
    var builder = ModuleDescriptor.newModule(name);
    // "// --main-class name"
    var mainClassMatcher = MAIN_CLASS.matcher(source);
    if (mainClassMatcher.find()) {
      var mainClass = mainClassMatcher.group(1);
      builder.mainClass(mainClass);
    }
    // "requires module /*version*/;"
    var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
    while (requiresMatcher.find()) {
      var requiredName = requiresMatcher.group(1);
      Optional.ofNullable(requiresMatcher.group(2))
          .ifPresentOrElse(
              version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
              () -> builder.requires(requiredName));
    }
    // "provides service with type, type, ...;"
    var providesMatcher = MODULE_PROVIDES_PATTERN.matcher(source);
    while (providesMatcher.find()) {
      var providesService = providesMatcher.group(1);
      var providesTypes = providesMatcher.group(2);
      builder.provides(providesService, List.of(providesTypes.trim().split("\\s*,\\s*")));
    }
    return builder.build();
  }

  /** Compute module's source path. */
  public static String moduleSourcePath(Path path, String module) {
    var directory = Files.isDirectory(path) ? path : Objects.requireNonNull(path.getParent());
    if (Files.notExists(directory.resolve("module-info.java"))) {
      throw new IllegalArgumentException("No 'module-info.java' file found in: " + directory);
    }
    var names = new ArrayList<String>();
    directory.forEach(element -> names.add(element.toString()));
    int frequency = Collections.frequency(names, module);
    if (frequency == 0) {
      return directory.toString();
    }
    if (frequency == 1) {
      if (directory.endsWith(module)) {
        return Optional.ofNullable(directory.getParent()).map(Path::toString).orElse(".");
      }
      var elements = names.stream().map(name -> name.equals(module) ? "*" : name);
      return String.join(File.separator, elements.collect(Collectors.toList()));
    }
    throw new IllegalArgumentException("Ambiguous module source path: " + path);
  }
}