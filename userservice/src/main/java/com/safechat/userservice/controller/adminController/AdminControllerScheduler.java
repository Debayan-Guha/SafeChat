package com.safechat.userservice.controller.adminController;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.service.scheduledService.DynamicSchedulerService;
import com.safechat.userservice.utility.Enumeration.ScheduledTaskType;
import com.safechat.userservice.utility.api.ApiResponseFormatter;

@RestController
@RequestMapping("/api/v1/admin/scheduler")
public class AdminControllerScheduler {

    private final DynamicSchedulerService schedulerService;

    public AdminControllerScheduler(DynamicSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/{taskName}/start")
    public ResponseEntity<ApiResponseFormatter<Void>> startTask(@PathVariable String taskName) {
        if (!ScheduledTaskType.isValid(taskName)) {
            throw new ValidationException("Invalid task name: " + taskName);
        }
        schedulerService.startTask(taskName);
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task started: " + taskName));
    }

    @PostMapping("/{taskName}/stop")
    public ResponseEntity<ApiResponseFormatter<Void>> stopTask(@PathVariable String taskName) {
        if (!ScheduledTaskType.isValid(taskName)) {
            throw new ValidationException("Invalid task name: " + taskName);
        }
        schedulerService.stopTask(taskName);
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task stopped: " + taskName));
    }

    @GetMapping("/{taskName}/status")
    public ResponseEntity<ApiResponseFormatter<Boolean>> getTaskStatus(@PathVariable String taskName) {
        if (!ScheduledTaskType.isValid(taskName)) {
            throw new ValidationException("Invalid task name: " + taskName);
        }
        boolean isRunning = schedulerService.isTaskRunning(taskName);
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task status", isRunning));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponseFormatter<Map<String, Boolean>>> getAllTaskStatus() {
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "All tasks status",
                schedulerService.getAllTaskStatus()));
    }
}