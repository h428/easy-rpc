package com.hao.open.utils.concurrent.pool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class ThreadPoolFactoryUtil {

    /**
     * 通过 threadNamePrefix 来区分不同线程池（可以把相同 threadNamePrefix 的线程池看错是为同一业务场景服务）
     * key: threadNamePrefix
     * value: threadPool
     */
    private static final Map<String, ExecutorService> THREAD_POOLS = new ConcurrentHashMap<>();

    private ThreadPoolFactoryUtil() {

    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix) {
        CustomThreadPoolConfig customThreadPoolConfig = new CustomThreadPoolConfig();
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix,
                                                                 CustomThreadPoolConfig customThreadPoolConfig) {
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(CustomThreadPoolConfig customThreadPoolConfig,
                                                                 String threadNamePrefix, Boolean daemon) {
        ExecutorService threadPool = THREAD_POOLS.computeIfAbsent(threadNamePrefix,
                k -> createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon));
        // 如果线程池被 shutdown 的话重新创建一个
        if (threadPool.isShutdown() || threadPool.isTerminated()) {
            THREAD_POOLS.remove(threadNamePrefix);
            threadPool = createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon);
            THREAD_POOLS.put(threadNamePrefix, threadPool);
        }
        return threadPool;
    }

    public static void shutDownAllThreadPool() {
        log.info("准备关系所有线程池...");
        THREAD_POOLS.entrySet().parallelStream().forEach(entry -> {
            ExecutorService executorService = entry.getValue();
            executorService.shutdown();
            log.info("关闭线程池 {}，当前状态为 {}", entry.getKey(), executorService.isTerminated());
            try {
                // 阻塞等待 terminate
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Thread pool never terminated");
                executorService.shutdownNow();
            }
        });
    }

    private static ExecutorService createThreadPool(CustomThreadPoolConfig customThreadPoolConfig,
                                                    String threadNamePrefix, Boolean daemon) {
        ThreadFactory threadFactory = createThreadFactory(threadNamePrefix, daemon);
        return new ThreadPoolExecutor(customThreadPoolConfig.getCorePoolSize(),
                customThreadPoolConfig.getMaximumPoolSize(),
                customThreadPoolConfig.getKeepAliveTime(), customThreadPoolConfig.getUnit(),
                customThreadPoolConfig.getWorkQueue(), threadFactory);
    }

    /**
     * 创建 ThreadFactory，如果 threadNamePrefix 不为空则使用自建 ThreadFactory，否则使用 defaultThreadFactory
     *
     * @param threadNamePrefix 线程名称前缀，同一个池子的线程前缀一致，并带有自己的序号
     * @param daemon           是否守护线程
     * @return 返回线程工厂
     */
    public static ThreadFactory createThreadFactory(String threadNamePrefix, Boolean daemon) {
        if (threadNamePrefix == null) {
            return Executors.defaultThreadFactory();
        }
        return new ThreadFactoryBuilder()
                .setNameFormat(threadNamePrefix + "-%d")
                .setDaemon(daemon)
                .build();
    }

    public static void printThreadPoolStatus(ThreadPoolExecutor threadPool) {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1,
                createThreadFactory("print-thread-pool-status", false));
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            log.info("============ThreadPool Status=============");
            log.info("ThreadPool Size: [{}]", threadPool.getPoolSize());
            log.info("Active Threads: [{}]", threadPool.getActiveCount());
            log.info("Number of Tasks : [{}]", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());
            log.info("===========================================");
        }, 0, 1, TimeUnit.SECONDS);
    }
}
