package cn.jack.exam.config;

import cn.jack.exam.exception.UnauthorizedException;

public final class CandidateUserContextHolder {

    private static final ThreadLocal<CandidateUserContext> HOLDER = new ThreadLocal<>();

    private CandidateUserContextHolder() {
    }

    public static void set(CandidateUserContext context) {
        HOLDER.set(context);
    }

    public static CandidateUserContext getRequired() {
        CandidateUserContext context = HOLDER.get();
        if (context == null) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
