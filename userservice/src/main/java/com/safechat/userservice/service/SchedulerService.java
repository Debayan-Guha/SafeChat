package com.safechat.userservice.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.safechat.userservice.service.userService.UserWriteService;

@Component
@EnableScheduling
public class SchedulerService {

    private final UserWriteService userWriteService;

    public SchedulerService(UserWriteService userWriteService) {
        this.userWriteService = userWriteService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredAccounts() {
        userWriteService.deleteExpiredAccounts();
    }
}