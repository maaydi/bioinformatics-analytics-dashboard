package com.bioinformatics.dashboard.providers;

public class ProviderContextHolder {

    private static final ThreadLocal<String> CURRENT_PROVIDER = new ThreadLocal<>();

    public static void set(String provider) {
        CURRENT_PROVIDER.set(provider);
    }

    public static String get() {
        return CURRENT_PROVIDER.get();
    }

    public static void clear() {
        CURRENT_PROVIDER.remove();
    }

}
