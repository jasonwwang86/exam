package cn.jack.exam.config;

public final class AdminUserContextHolder {

    private static final ThreadLocal<AdminUserContext> HOLDER = new ThreadLocal<>();

    private AdminUserContextHolder() {
    }

    public static void set(AdminUserContext context) {
        HOLDER.set(context);
    }

    public static AdminUserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
