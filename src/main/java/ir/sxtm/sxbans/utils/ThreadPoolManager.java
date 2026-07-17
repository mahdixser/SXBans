package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Thread pool manager for handling asynchronous tasks.
 */
public class ThreadPoolManager {
    private final SXBans plugin;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ForkJoinPool forkJoinPool;
    private boolean shutdown;

    public ThreadPoolManager(SXBans plugin) {
        this.plugin = plugin;

        // Main executor for general tasks
        this.executorService = Executors.newCachedThreadPool(
                new NamedThreadFactory("SXBans-Worker")
        );

        // Scheduled executor for delayed/repeating tasks
        this.scheduledExecutorService = Executors.newScheduledThreadPool(
                4,
                new NamedThreadFactory("SXBans-Scheduler")
        );

        // ForkJoinPool for parallel processing
        this.forkJoinPool = ForkJoinPool.commonPool();

        this.shutdown = false;
    }

    /**
     * Execute a task asynchronously.
     *
     * @param task The task to execute
     * @return Future representing the task
     */
    public CompletableFuture<Void> execute(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }

    /**
     * Execute a task asynchronously and return a result.
     *
     * @param task The task to execute
     * @param <T> The result type
     * @return Future representing the task
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    /**
     * Execute a task asynchronously and return a result.
     *
     * @param task The task to execute
     * @param <T> The result type
     * @return Future representing the task
     */
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executorService);
    }

    /**
     * Schedule a task with a delay.
     *
     * @param task The task to execute
     * @param delay The delay in milliseconds
     * @return ScheduledFuture representing the task
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay) {
        return scheduledExecutorService.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a task with a delay and return a result.
     *
     * @param task The task to execute
     * @param delay The delay in milliseconds
     * @param <T> The result type
     * @return ScheduledFuture representing the task
     */
    public <T> ScheduledFuture<T> schedule(Callable<T> task, long delay) {
        return scheduledExecutorService.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a repeating task with a fixed delay.
     *
     * @param task The task to execute
     * @param initialDelay The initial delay in milliseconds
     * @param period The period in milliseconds
     * @return ScheduledFuture representing the task
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long period) {
        return scheduledExecutorService.scheduleWithFixedDelay(
                task, initialDelay, period, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Schedule a repeating task at a fixed rate.
     *
     * @param task The task to execute
     * @param initialDelay The initial delay in milliseconds
     * @param period The period in milliseconds
     * @return ScheduledFuture representing the task
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period) {
        return scheduledExecutorService.scheduleAtFixedRate(
                task, initialDelay, period, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Execute a task using ForkJoinPool for parallel processing.
     *
     * @param task The task to execute
     * @param <T> The result type
     * @return Future representing the task
     */
    public <T> ForkJoinTask<T> forkJoin(Callable<T> task) {
        return forkJoinPool.submit(task);
    }

    /**
     * Execute a batch of tasks in parallel.
     *
     * @param tasks The tasks to execute
     * @param <T> The result type
     * @return List of futures
     */
    public <T> List<Future<T>> executeBatch(List<Callable<T>> tasks) {
        try {
            return executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Execute a batch of tasks and wait for completion.
     *
     * @param tasks The tasks to execute
     * @param <T> The result type
     * @return List of results
     */
    public <T> List<T> executeBatchAndWait(List<Callable<T>> tasks) {
        try {
            return executorService.invokeAll(tasks).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            plugin.getSXBansLogger().warning("Task failed: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(obj -> (T) obj)
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Shutdown the thread pool.
     */
    public void shutdown() {
        if (shutdown) return;

        shutdown = true;

        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown scheduled executor
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // ForkJoinPool doesn't need explicit shutdown
    }

    /**
     * Check if the thread pool is shutdown.
     *
     * @return true if shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Get thread pool statistics.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            stats.put("executor_active", tpe.getActiveCount());
            stats.put("executor_pool", tpe.getPoolSize());
            stats.put("executor_queue", tpe.getQueue().size());
            stats.put("executor_completed", tpe.getCompletedTaskCount());
        }

        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            stats.put("scheduled_pool", stpe.getPoolSize());
            stats.put("scheduled_queue", stpe.getQueue().size());
            stats.put("scheduled_completed", stpe.getCompletedTaskCount());
        }

        stats.put("forkjoin_pool", forkJoinPool.getPoolSize());
        stats.put("forkjoin_active", forkJoinPool.getActiveThreadCount());
        stats.put("forkjoin_parallelism", forkJoinPool.getParallelism());

        return stats;
    }

    /**
     * Named thread factory for better debugging.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private int counter;

        public NamedThreadFactory(String name) {
            this.name = name;
            this.counter = 0;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name + "-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }
}