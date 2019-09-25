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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void banner() {
    assertFalse(Bach.of().getBanner().isBlank());
  }

  @Test
  void checkDefaultValues() {
    var bach = Bach.of();
    assertNotNull(bach.out);
    assertNotNull(bach.err);
    assertEquals(Path.of(""), bach.configuration.getBaseDirectory());
    assertEquals(Path.of("bin"), bach.configuration.getWorkspaceDirectory());
    assertEquals("Bach.java", bach.configuration.getProjectName());
    assertEquals(Version.parse(Bach.VERSION), bach.configuration.getProjectVersion());
  }

  @Test
  void versionIsLegalByModuleDescriptorVersionsParseFactoryContract() {
    assertDoesNotThrow(() -> Version.parse(Bach.VERSION));
  }
}