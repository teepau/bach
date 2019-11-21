package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import demo.core.PublicCore;
import org.junit.jupiter.api.Test;

class IntegrationTests {
  @Test
  void createPublicCore() {
    new PublicCore();
  }

  @Test
  void createMantleIsIllegal() throws Exception {
    // new demo.mantle.Mantle(); // package demo.mantle is not visible
    var type = Class.forName("demo.mantle.Mantle");
    assertNotNull(type);
    assertEquals("Mantle", type.getSimpleName());
    assertEquals("demo.mantle", type.getModule().getName());
    var ctor = type.getConstructor();
    assertNotNull(ctor);
    var e = assertThrows(IllegalAccessException.class, () -> ctor.newInstance());
    assertEquals(
        "class it.IntegrationTests (in module it)"
            + " cannot access class demo.mantle.Mantle (in module demo.mantle)"
            + " because module demo.mantle does not export demo.mantle to module it",
        e.getMessage());
  }
}