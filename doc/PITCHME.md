# Bach.java

---

## Overview

- Motivation
- Features
- Project Model
- Usage
- Outlook

---

## Motivation

- Why doesn't the JDK provide a build tool?
- Why another build tool?

---

## JDK Tools and Build Tools

```text
JDK Foundation Tools                    Multi-Purpose Build Tools
 |                                                  Maven
 |                                Ant(+Ivy)          | Gradle
 |                 Bach.java       |                 |  | Bazel Buildr
 |  Scripts         |              |                 | Buck| sbt |
 |   |              |              |                 |  |  |  |  |
 +---+--------------+--------------+-----------------+--+--+--+--+-----
 |
  \ javac, javap, javadoc, java, jar, jlink, jmod, jdeps, and jdeprscan
```

---

## JDK Tools 2019

- **1** `javac` Compiler for the Java programming language
- **1** `javap` Class file disassembler
- **1** `javadoc` API documentation generator
- **1** `java` Launcher for Java applications
- **1** `jar` Java Archive (JAR) file manager
- **8** `jdeps` Class dependency analyzer
- **8** `jdeprscan` Deprecated API use finder
- **9** `jlink` Custom runtime image assembler
- **9** `jmod` JMOD file manager
- **14** `jpackage` Package self-contained Java applications

---

## Features

- Lightweight
- Java
- Modules

---

## Project Model

- Package `de.sormuras.bach.project`
- Project
  - Name
  - Version
- Structure
  - Folder
  - Realm
  - Unit
    - Source
  - Library
- Deployment

---

## Usage

- `jshell ...`
- `java -p ... -m de.sormuras.bach`

---

## Outlook

- from `Bach.java` to `{JDK_HOME}/bin/jbuild[.exe]`?
- to `jbach`, `jbuild`, `javab`, ...
- create a JEP?