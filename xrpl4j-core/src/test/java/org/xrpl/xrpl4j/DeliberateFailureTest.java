package org.xrpl.xrpl4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Fork-only test: proves a failing unit test stops the release pipeline at the CI gate. */
class DeliberateFailureTest {

  @Test
  void failsOnPurpose() {
    fail("deliberate failure to test the release CI gate");
  }
}
