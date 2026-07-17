package ir.sxtm.sxbans.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for reflection operations.
 */
public class ReflectionUtils {
    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    /**
     * Get a class by name.
     *
     * @param name The class name
     * @return The class, or null if not found
     */
    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get a class in the net.minecraft.server package.
     *
     * @param name The class name
     * @return The class, or null if not found
     */
    public static Class<?> getNMSClass(String name) {
        return getClass("net.minecraft.server." + VERSION + "." + name);
    }

    /**
     * Get a class in the org.bukkit.craftbukkit package.
     *
     * @param name The class name
     * @return The class, or null if not found
     */
    public static Class<?> getCraftClass(String name) {
        return getClass("org.bukkit.craftbukkit." + VERSION + "." + name);
    }

    /**
     * Get a method from a class.
     *
     * @param clazz The class
     * @param name The method name
     * @param parameterTypes The parameter types
     * @return The method, or null if not found
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Get a field from a class.
     *
     * @param clazz The class
     * @param name The field name
     * @return The field, or null if not found
     */
    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Get a constructor from a class.
     *
     * @param clazz The class
     * @param parameterTypes The parameter types
     * @return The constructor, or null if not found
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Invoke a method on an object.
     *
     * @param obj The object
     * @param method The method
     * @param args The arguments
     * @return The result, or null if failed
     */
    public static Object invokeMethod(Object obj, Method method, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a field value from an object.
     *
     * @param obj The object
     * @param field The field
     * @return The field value, or null if failed
     */
    public static Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set a field value on an object.
     *
     * @param obj The object
     * @param field The field
     * @param value The value
     * @return true if successful
     */
    public static boolean setFieldValue(Object obj, Field field, Object value) {
        try {
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create a new instance of a class.
     *
     * @param constructor The constructor
     * @param args The arguments
     * @return The new instance, or null if failed
     */
    public static Object newInstance(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the NMS handle of a CraftBukkit object.
     *
     * @param obj The CraftBukkit object
     * @return The NMS handle, or null if failed
     */
    public static Object getHandle(Object obj) {
        try {
            Method method = obj.getClass().getMethod("getHandle");
            return method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a CraftBukkit object to its NMS counterpart.
     *
     * @param obj The CraftBukkit object
     * @param nmsClass The NMS class
     * @return The NMS object, or null if failed
     */
    public static Object toNMS(Object obj, Class<?> nmsClass) {
        Object handle = getHandle(obj);
        if (handle != null && nmsClass.isAssignableFrom(handle.getClass())) {
            return handle;
        }
        return null;
    }

    /**
     * Convert an NMS object to its CraftBukkit counterpart.
     *
     * @param obj The NMS object
     * @param craftClass The CraftBukkit class
     * @return The CraftBukkit object, or null if failed
     */
    public static Object toCraft(Object obj, Class<?> craftClass) {
        try {
            Method method = craftClass.getMethod("asCraft", obj.getClass());
            return method.invoke(null, obj);
        } catch (Exception e) {
            return null;
        }
    }
}