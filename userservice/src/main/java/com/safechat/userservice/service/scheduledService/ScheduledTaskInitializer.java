package com.safechat.userservice.service.scheduledService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.safechat.userservice.service.kafkaService.KafkaService;
import com.safechat.userservice.service.userService.UserWriteService;
import com.safechat.userservice.utility.Enumeration.ScheduledTaskType;

//CommandLineRunner in Spring Boot is used to run some code automatically once the application starts
@Component
public class ScheduledTaskInitializer implements CommandLineRunner {

    private final DynamicSchedulerService schedulerService;
    private final UserWriteService userWriteService;
    private final KafkaService kafkaService;

    public ScheduledTaskInitializer(DynamicSchedulerService schedulerService, UserWriteService userWriteService,
            KafkaService kafkaService) {
        this.schedulerService = schedulerService;
        this.userWriteService = userWriteService;
        this.kafkaService = kafkaService;
    }

    @Override
    public void run(String... args) {
        // Register each scheduled method
        schedulerService.registerTask(
                ScheduledTaskType.DELETE_EXPIRED_ACCOUNTS,
                () -> userWriteService.deleteExpiredAccounts(),
                "0 0 * * * *" // Every hour
        );

        schedulerService.registerTask(
                ScheduledTaskType.RETRY_FAILED_KAFKA_DELETIONS,
                () -> kafkaService.retryFailedDeletions(),
                "0 0 */12 * * *");

        // Add more tasks as needed
        // schedulerService.registerTask("cleanupLogs", () -> cleanupService.clean(), "0
        // 0 2 * * *");
        // schedulerService.registerTask("sendReports", () -> reportService.send(), "0
        // */30 * * * *");
    }
}