package com.safechat.userservice.service.scheduledService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.safechat.userservice.service.userService.UserScheduledDeletionService;
import com.safechat.userservice.utility.Enumeration.ScheduledTaskType;

//CommandLineRunner in Spring Boot is used to run some code automatically once the application starts
@Component
public class ScheduledTaskInitializer implements CommandLineRunner {

    private final DynamicSchedulerService schedulerService;
    private final UserScheduledDeletionService userScheduledDeletionService;

    public ScheduledTaskInitializer(DynamicSchedulerService schedulerService, UserScheduledDeletionService userScheduledDeletionService) {
        this.schedulerService = schedulerService;
        this.userScheduledDeletionService = userScheduledDeletionService;
    }

    @Override
    public void run(String... args) {
        // Register each scheduled method
        schedulerService.registerTask(
                ScheduledTaskType.DELETE_EXPIRED_ACCOUNTS,
                () -> userScheduledDeletionService.deleteExpiredAccounts(),
                "0 0 * * * *" // Every hour
        );

        schedulerService.registerTask(
                ScheduledTaskType.RETRY_FAILED_USER_DELETIONS,
                () -> userScheduledDeletionService.retryFailedUserDeletions(),
                "0 */10 * * * *" // Every 10 minutes
        );

        // Add more tasks as needed
        // schedulerService.registerTask("cleanupLogs", () -> cleanupService.clean(), "0
        // 0 2 * * *");
        // schedulerService.registerTask("sendReports", () -> reportService.send(), "0
        // */30 * * * *");
    }
}