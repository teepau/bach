/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License", "Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing", "software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND", "either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Log;
import de.sormuras.bach.project.Library;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverTests {

  @Test
  void resolveDefaultLibraryLinks() {
    var library = Library.of();
    var resolver = new Resolver(Log.ofNullWriter(), library);
    assertEquals(
        "Link{https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/0/junit-jupiter-0.jar@0}",
        resolver.lookup("org.junit.jupiter", Version.parse("0")).toString());
  }

  @Test
  void resolveUserDefinedLibraryLinks() {
    var custom =
        Map.of(
            "foo", new Library.Link("foo(${VERSION})", Version.parse("1")),
            "bar", new Library.Link("bar(${VERSION})", Version.parse("3")),
            "baz", new Library.Link("baz(5)", Version.parse("5")));  // no placeholder

    var library = new Library(Set.of(), custom, Set.of());
    assertTrue(library.requires().isEmpty());
    assertEquals("Link{foo(${VERSION})@1}", library.links().get("foo").toString());
    assertEquals("Link{bar(${VERSION})@3}", library.links().get("bar").toString());
    assertEquals("Link{baz(5)@5}", library.links().get("baz").toString());

    var resolver = new Resolver(Log.ofNullWriter(), library);
    assertEquals("Link{foo(1)@1}", resolver.lookup("foo", null).toString());
    assertEquals("Link{foo(2)@2}", resolver.lookup("foo", Version.parse("2")).toString());
    assertEquals("Link{bar(4)@4}", resolver.lookup("bar", Version.parse("4")).toString());
    assertEquals("Link{baz(5)@5}", resolver.lookup("baz", null).toString());
    assertEquals("Link{baz(5)@5}", resolver.lookup("baz", Version.parse("6")).toString());
  }

  @Test
  void lookupJUnit4ModuleCreatesLibraryLinkOnTheFly() {
    var library = Library.of();
    var resolver = new Resolver(Log.ofNullWriter(), library);
    assertTrue(
        resolver
            .lookup("junit")
            .toString()
            .startsWith(
                "Link{https://repo1.maven.org/maven2/junit/junit/${VERSION}/junit-${VERSION}.jar@4."));
  }
}
