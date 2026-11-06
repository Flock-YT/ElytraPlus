package cn.ericcraft.elytraPlus.fabric;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionBridgeTest {
    public static final class Api {
        public static boolean check(Object source, String permission, boolean fallback) {
            return permission.equals("allow") || (!permission.equals("deny") && fallback);
        }
    }
    @Test void absentApiUsesTheExactDefaultForEachNode() {
        PermissionBridge bridge = new PermissionBridge(null);
        assertTrue(bridge.check(this, "elytraplus.use", true));
        assertFalse(bridge.check(this, "elytraplus.bypass.durability", false));
    }
    @Test void explicitPermissionOverridesDefaultAndUndefinedFallsBack() {
        PermissionBridge bridge = new PermissionBridge(Api.class);
        assertTrue(bridge.check(this, "allow", false)); assertFalse(bridge.check(this, "deny", true));
        assertTrue(bridge.check(this, "undefined", true)); assertFalse(bridge.check(this, "undefined", false));
    }
    @Test void incompatibleApiIsReportedRatherThanGrantingDefaultPermissions() {
        assertThrows(IllegalStateException.class, () -> new PermissionBridge(String.class).check(this, "allow", true));
    }
}
