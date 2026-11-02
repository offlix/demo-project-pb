package com.offlix.pbx_dmo.guard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevPolicyGuardTest {

    @Test
    @DisplayName("DevPolicyGuard succeeds when AGENTS.md exists and is not ignored")
    void testGuard_Passes() {
        DevPolicyGuard guard = new DevPolicyGuard();
        assertDoesNotThrow(() -> guard.run(null));
    }
}
