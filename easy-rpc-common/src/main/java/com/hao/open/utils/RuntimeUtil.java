package com.hao.open.utils;

/**
 * 运行时工具
 */
public class RuntimeUtil {
    /**
     * 获取 CPU 的核心数
     * ps: 返回的是线程数，比如 6 核 12 线程返回 12
     *
     * @return cpu 的核心数
     */
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }

}
