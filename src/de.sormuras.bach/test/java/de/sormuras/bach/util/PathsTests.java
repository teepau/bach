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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathsTests {

  @Test
  void isEmptyWalkThrough(@TempDir Path directory) throws Exception {
    assertTrue(Paths.isEmpty(directory));
    var file = Files.createFile(directory.resolve("regular.file"));
    assertTrue(Paths.isEmpty(file));
    Files.writeString(file, "Hello world!");
    assertFalse(Paths.isEmpty(file));
    assertFalse(Paths.isEmpty(directory));
    Files.delete(file);
    assertTrue(Paths.isEmpty(directory));
    var subdirectory = Files.createDirectory(directory.resolve("subdirectory"));
    assertFalse(Paths.isEmpty(directory));
    Files.delete(subdirectory);
    assertTrue(Paths.isEmpty(directory));
  }

  @Test
  void isEmptyFailsForNotReadablePath(@TempDir Path temp) throws Exception {
    var sub = Files.createDirectory(temp.resolve("sub"));
    assertTrue(Paths.isEmpty(sub));
    chmod(sub, false, false, false);
    try {
      assertThrows(UncheckedIOException.class, () -> Paths.isEmpty(sub));
    } finally {
      chmod(sub, true, true, true);
    }
  }

  static void chmod(Path path, boolean r, boolean w, boolean x) throws Exception {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      var principals = path.getFileSystem().getUserPrincipalLookupService();
      var user = principals.lookupPrincipalByName(System.getProperty("user.name"));
      var builder = AclEntry.newBuilder();
      var permissions =
          EnumSet.of(
              // AclEntryPermission.EXECUTE, // "x"
              // AclEntryPermission.READ_DATA, // "r"
              AclEntryPermission.READ_ATTRIBUTES,
              AclEntryPermission.READ_NAMED_ATTRS,
              // AclEntryPermission.WRITE_DATA, // "w"
              // AclEntryPermission.APPEND_DATA, // "w"
              AclEntryPermission.WRITE_ATTRIBUTES,
              AclEntryPermission.WRITE_NAMED_ATTRS,
              AclEntryPermission.DELETE_CHILD,
              AclEntryPermission.DELETE,
              AclEntryPermission.READ_ACL,
              AclEntryPermission.WRITE_ACL,
              AclEntryPermission.WRITE_OWNER,
              AclEntryPermission.SYNCHRONIZE);
      if (r) {
        permissions.add(AclEntryPermission.READ_DATA); // == LIST_DIRECTORY
      }
      if (w) {
        permissions.add(AclEntryPermission.WRITE_DATA); // == ADD_FILE
        permissions.add(AclEntryPermission.APPEND_DATA); // == ADD_SUBDIRECTORY
      }
      if (x) {
        permissions.add(AclEntryPermission.EXECUTE);
      }
      builder.setPermissions(permissions);
      builder.setPrincipal(user);
      builder.setType(AclEntryType.ALLOW);
      var aclAttr = Files.getFileAttributeView(path, AclFileAttributeView.class);
      aclAttr.setAcl(List.of(builder.build()));
      return;
    }
    var user = (r ? "r" : "-") + (w ? "w" : "-") + (x ? "x" : "-");
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(user + "------"));
  }
}
