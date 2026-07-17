package com.jtprince.coordinateoffset.folia.lib.org.geysermc.hurricane;

import org.bukkit.Bukkit;

public final class NMSReflection {
    private static String version;

    public static boolean mojmap = true;

    public static String getVersion() {
        return version == null ? version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] : version;
    }

    public static Class<?> getMojmapNMSClass(String name) {
        try {
            return Class.forName("net.minecraft." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> getNMSClass(String post1_16Prefix, String... names) {
        for (String name : names) {
            Class<?> newNMSClass = getMojmapNMSClass(post1_16Prefix + "." + name);
            if (newNMSClass != null) {
                return newNMSClass;
            }
        }

        mojmap = false;
        Exception ex = null;
        for (String name : names) {
            try {
                return Class.forName("net.minecraft.server." + getVersion() + "." + name);
            } catch (ClassNotFoundException e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }
        if (ex != null) {
            ex.printStackTrace();
        }
        return null;
    }
}
