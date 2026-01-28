package com.example.spring_seatreservation.common;

import java.util.HashMap;

public class R extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    // 成功状态码
    public static final int SUCCESS = 200;
    // 失败状态码
    public static final int ERROR = 500;

    public R() {
        put("code", SUCCESS);
        put("msg", "success");
    }

    /* ===================== 静态构建方法 ===================== */

    public static R ok() {
        return new R();
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R error() {
        return error(ERROR, "error");
    }

    public static R error(String msg) {
        return error(ERROR, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    /* ===================== 常用链式方法 ===================== */

    public R data(Object data) {
        put("data", data);
        return this;
    }

    public R total(long total) {
        put("total", total);
        return this;
    }

    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
