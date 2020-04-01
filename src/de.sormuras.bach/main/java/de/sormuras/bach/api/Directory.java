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

package de.sormuras.bach.api;

import de.sormuras.bach.Convention;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/** An optionally targeted directory of Java source files: {@code src/foo/main/java[-11]}. */
public /*static*/ class Directory {

  /** Return directory instance for the given path. */
  public static Directory of(Path path) {
    var release = Convention.javaReleaseFeatureNumber(String.valueOf(path.getFileName()));
    return new Directory(path, release);
  }

  /** Return list of directories by scanning the given common root path: {@code src/foo/main}. */
  public static List<Directory> listOf(Path root) {
    if (Files.notExists(root)) return List.of();
    var directories = new ArrayList<Directory>();
    try (var stream = Files.newDirectoryStream(root, Files::isDirectory)) {
      stream.forEach(path -> directories.add(of(path)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    directories.sort(Comparator.comparingInt(Directory::release));
    return List.copyOf(directories);
  }

  private final Path path;
  private final int release;

  public Directory(Path path, int release) {
    this.path = path;
    this.release = release;
  }

  public Path path() {
    return path;
  }

  public int release() {
    return release;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Directory.class.getSimpleName() + "[", "]")
        .add("path=" + path)
        .add("release=" + release)
        .toString();
  }
}
