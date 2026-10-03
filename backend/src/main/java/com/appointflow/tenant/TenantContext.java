package com.appointflow.tenant;

public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    public static Long get() {
        return currentTenantId.get();
    }

    public static Long getTenantId() {
        return currentTenantId.get();
    }

    public static void set(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public static void clear() {
        currentTenantId.remove();
    }
}
