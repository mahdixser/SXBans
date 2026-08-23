package ir.sxtm.sxbans.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {
    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> getNMSClass(String name) {
        return getClass("net.minecraft.server." + VERSION + "." + name);
    }

    public static Class<?> getCraftClass(String name) {
        return getClass("org.bukkit.craftbukkit." + VERSION + "." + name);
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Object invokeMethod(Object obj, Method method, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean setFieldValue(Object obj, Field field, Object value) {
        try {
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Object newInstance(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getHandle(Object obj) {
        try {
            Method method = obj.getClass().getMethod("getHandle");
            return method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object toNMS(Object obj, Class<?> nmsClass) {
        Object handle = getHandle(obj);
        if (handle != null && nmsClass.isAssignableFrom(handle.getClass())) {
            return handle;
        }
        return null;
    }

    public static Object toCraft(Object obj, Class<?> craftClass) {
        try {
            Method method = craftClass.getMethod("asCraft", obj.getClass());
            return method.invoke(null, obj);
        } catch (Exception e) {
            return null;
        }
    }
}