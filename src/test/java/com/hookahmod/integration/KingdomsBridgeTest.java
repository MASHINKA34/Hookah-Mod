package com.hookahmod.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KingdomsBridgeTest {
    private static final Class<?>[] NO_PARAMETERS = {};

    @Test
    void absentModAllowsNormalPlay() {
        KingdomsBridge bridge = new KingdomsBridge(() -> false, "missing.Integration");
        assertTrue(bridge.permission("canEquipHookah", NO_PARAMETERS));
        assertTrue(bridge.permission("canMoveHookahBlock", NO_PARAMETERS));
    }

    @Test
    void missingIntegrationOrPermissionMethodDeniesAccess() {
        KingdomsBridge missingClass = new KingdomsBridge(() -> true, "missing.Integration");
        assertFalse(missingClass.permission("canEquipHookah", NO_PARAMETERS));
        KingdomsBridge missingMethod = new KingdomsBridge(() -> true, Permissions.class.getName());
        assertFalse(missingMethod.permission("missingPermission", NO_PARAMETERS));
        assertFalse(missingMethod.permission("wrongReturnType", NO_PARAMETERS));
    }

    @Test
    void transientFailureDeniesAccessButDoesNotDisableRecoveredMethod() {
        KingdomsBridge bridge = new KingdomsBridge(() -> true, Permissions.class.getName());
        Permissions.fail = true;
        assertFalse(bridge.permission("canEquipHookah", NO_PARAMETERS));
        Permissions.fail = false;
        assertTrue(bridge.permission("canEquipHookah", NO_PARAMETERS));
        assertFalse(bridge.permission("canMoveHookahBlock", NO_PARAMETERS));
    }

    public static final class Permissions {
        static boolean fail;

        public static boolean canEquipHookah() {
            if (fail) throw new IllegalStateException("Temporary integration failure");
            return true;
        }

        public static boolean canMoveHookahBlock() {
            return false;
        }

        public static String wrongReturnType() {
            return "true";
        }
    }
}
