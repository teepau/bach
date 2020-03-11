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

package de.sormuras.bach.execution.task;

import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Snippet;
import de.sormuras.bach.execution.Task;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Creates a directory by creating all nonexistent parent directories first. */
public /*static*/ class CreateDirectories extends Task {

  private final Path path;

  public CreateDirectories(Path path) {
    super("Create directories " + path, false, List.of());
    this.path = path;
  }

  @Override
  public ExecutionResult execute(ExecutionContext context) {
    try {
      Files.createDirectories(path);
      return context.ok();
    } catch (Exception e) {
      return context.failed(e);
    }
  }

  @Override
  public Snippet toSnippet() {
    return new Snippet(
        Set.of(Files.class, Path.class),
        List.of(String.format("Files.createDirectories(%s);", $(path))));
  }
}