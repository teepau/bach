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

package de.sormuras.bach.project;

import de.sormuras.bach.project.structure.Location;
import de.sormuras.bach.project.structure.Realm;
import de.sormuras.bach.project.structure.Unit;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/** A modular project structure consisting of realms and other components. */
public /*static*/ class Structure {

  private final Location location;
  private final List<Realm> realms;

  public Structure(Location location, List<Realm> realms) {
    this.location = location;
    this.realms = realms;
  }

  public Location location() {
    return location;
  }

  public List<Realm> realms() {
    return realms;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
        .add("location=" + location)
        .add("realms=" + realms)
        .toString();
  }

  public List<String> toRealmNames() {
    return realms.stream().map(Realm::name).collect(Collectors.toList());
  }

  public List<String> toUnitNames() {
    return realms.stream()
        .flatMap(realm -> realm.units().stream())
        .map(Unit::name)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }
}
