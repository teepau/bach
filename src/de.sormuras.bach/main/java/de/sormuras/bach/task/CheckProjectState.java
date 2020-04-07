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

package de.sormuras.bach.task;

import de.sormuras.bach.Project;
import de.sormuras.bach.Task;

/** Validate all project components are in a legal and sane state. */
public /*static*/ class CheckProjectState extends Task {

  private final Project project;

  public CheckProjectState(Project project) {
    super("Check project state");
    this.project = project;
  }

  @Override
  public void execute(Execution context) throws IllegalStateException {
    if (project.structure().toUnitNames().isEmpty()) fail("no unit present");
  }

  private static void fail(String message) {
    throw new IllegalStateException("project validation failed: " + message);
  }
}