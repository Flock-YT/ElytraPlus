package cn.ericcraft.elytraPlus.fabric;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Optional public API lookup. Minecraft parameter types come from the loaded API itself. */
public final class PermissionBridge {
    private final Class<?> api;
    private Method check;
    public PermissionBridge(Class<?> api) { this.api = api; }
    public static PermissionBridge discover(ClassLoader loader) {
        try {
            return new PermissionBridge(Class.forName("me.lucko.fabric.api.permissions.v0.Permissions", false, loader));
        } catch (ClassNotFoundException absent) {
            return new PermissionBridge(null);
        }
    }
    public boolean check(Object source, String node, boolean fallback) {
        if (api == null) return fallback;
        if (check == null) {
            for (Method method : api.getMethods()) {
                Class<?>[] args = method.getParameterTypes();
                if (method.getName().equals("check") && Modifier.isStatic(method.getModifiers()) &&
                        method.getReturnType() == boolean.class && args.length == 3 &&
                        args[0].isInstance(source) && args[1] == String.class && args[2] == boolean.class) {
                    check = method;
                    break;
                }
            }
            if (check == null) throw new IllegalStateException("Incompatible Fabric Permissions API: missing online check");
        }
        try { return (Boolean) check.invoke(null, source, node, fallback); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Fabric permission check failed", error); }
    }
}
