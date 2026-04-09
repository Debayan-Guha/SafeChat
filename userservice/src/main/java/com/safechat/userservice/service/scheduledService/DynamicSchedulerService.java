package com.safechat.userservice.service.scheduledService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class DynamicSchedulerService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> taskStatus = new ConcurrentHashMap<>();
    private final Map<String, Runnable> taskRunnables = new ConcurrentHashMap<>();
    private final Map<String, String> taskCrons = new ConcurrentHashMap<>();

    public DynamicSchedulerService(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    // Register a task with its cron expression
    public void registerTask(String taskName, Runnable task, String cronExpression) {
        taskRunnables.put(taskName, task);
        taskCrons.put(taskName, cronExpression);
        startTask(taskName); // Auto-start by default
    }

    // Start a specific task
    public void startTask(String taskName) {
        if (tasks.containsKey(taskName) && taskStatus.getOrDefault(taskName, false)) {
            return; // Already running
        }
        
        ScheduledFuture<?> future = taskScheduler.schedule(
            taskRunnables.get(taskName), 
            new CronTrigger(taskCrons.get(taskName))
        );
        tasks.put(taskName, future);
        taskStatus.put(taskName, true);
    }

    // Stop a specific task
    public void stopTask(String taskName) {
        ScheduledFuture<?> future = tasks.get(taskName);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            taskStatus.put(taskName, false);
        }
    }

    // Check if a task is running
    public boolean isTaskRunning(String taskName) {
        return taskStatus.getOrDefault(taskName, false);
    }

    // Get all tasks status
    public Map<String, Boolean> getAllTaskStatus() {
        return new ConcurrentHashMap<>(taskStatus);
    }
}