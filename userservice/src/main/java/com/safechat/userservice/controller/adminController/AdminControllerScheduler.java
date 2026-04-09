package com.safechat.userservice.controller.adminController;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.service.scheduledService.DynamicSchedulerService;
import com.safechat.userservice.utility.UrlEncoderUtil;
import com.safechat.userservice.utility.Enumeration.ScheduledTaskType;
import com.safechat.userservice.utility.api.ApiResponseFormatter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/userservice/admin/scheduler")
@Tag(name = "Admin Scheduler Management", description = "APIs for managing scheduled tasks (start, stop, status)")
public class AdminControllerScheduler {

    private final DynamicSchedulerService schedulerService;

    public AdminControllerScheduler(DynamicSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Operation(summary = "Start scheduled task", description = "Starts a specific scheduled task by task name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task started successfully", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "400", description = "Invalid task name", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
    @PostMapping("/{taskName}/start")
    public ResponseEntity<ApiResponseFormatter<Void>> startTask(
            @Parameter(description = "Name of the task to start", required = true) @PathVariable String taskName) {

        String decodedTaskName = UrlEncoderUtil.decode(taskName);

        if (!ScheduledTaskType.isValid(decodedTaskName)) {
            throw new ValidationException("Invalid task name: " + decodedTaskName);
        }
        schedulerService.startTask(decodedTaskName);
        return ResponseEntity
                .ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task started: " + decodedTaskName));
    }

    @Operation(summary = "Stop scheduled task", description = "Stops a specific scheduled task by task name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task stopped successfully", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "400", description = "Invalid task name", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
    @PostMapping("/{taskName}/stop")
    public ResponseEntity<ApiResponseFormatter<Void>> stopTask(
            @Parameter(description = "Name of the task to stop", required = true) @PathVariable String taskName) {

        String decodedTaskName = UrlEncoderUtil.decode(taskName);

        if (!ScheduledTaskType.isValid(decodedTaskName)) {
            throw new ValidationException("Invalid task name: " + decodedTaskName);
        }
        schedulerService.stopTask(decodedTaskName);
        return ResponseEntity
                .ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task stopped: " + decodedTaskName));
    }

    @Operation(summary = "Get task status", description = "Retrieves the running status of a specific scheduled task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status retrieved", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "400", description = "Invalid task name", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
    @GetMapping("/{taskName}/status")
    public ResponseEntity<ApiResponseFormatter<Boolean>> getTaskStatus(
            @Parameter(description = "Name of the task to check", required = true) @PathVariable String taskName) {

        String decodedTaskName = UrlEncoderUtil.decode(taskName);

        if (!ScheduledTaskType.isValid(decodedTaskName)) {
            throw new ValidationException("Invalid task name: " + decodedTaskName);
        }
        boolean isRunning = schedulerService.isTaskRunning(decodedTaskName);
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "Task status", isRunning));
    }

    @Operation(summary = "Get all tasks status", description = "Retrieves the running status of all scheduled tasks.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All tasks status retrieved", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponseFormatter<Map<String, Boolean>>> getAllTaskStatus() {
        return ResponseEntity.ok(ApiResponseFormatter.formatter(HttpStatus.OK.value(), "All tasks status",
                schedulerService.getAllTaskStatus()));
    }
}