import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UtilTests {
  @Test
  void join() {
    assertEquals("<empty>", Bach.Util.join());
    assertEquals("\"\"", Bach.Util.join(""));
    assertEquals("\"<null>\"", Bach.Util.join((Object) null));
    assertEquals("\"1\"", Bach.Util.join(1));
    assertEquals("\"1\", \"2\"", Bach.Util.join(1, 2));
    assertEquals("\"1\", \"2\", \"3\"", Bach.Util.join(1, 2, 3));
  }
}
