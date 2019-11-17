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

package de.sormuras.bach.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** {@link Path}-related helpers. */
public class Paths {
  private Paths() {}

  /** Convenient short-cut to {@code "user.home"} as a path. */
  public static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  public static List<Path> list(Path directory, Predicate<Path> filter) {
    try (var stream = Files.list(directory)) {
      return stream.filter(filter).sorted().collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("List directory failed: " + directory, e);
    }
  }

  public static List<Path> list(Path directory, String glob) {
    try (var items = Files.newDirectoryStream(directory, glob)) {
      return StreamSupport.stream(items.spliterator(), false)
          .sorted()
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("List directory using glob failed: " + directory, e);
    }
  }

  public static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new RuntimeException("Read all content from file failed: " + path, e);
    }
  }
}