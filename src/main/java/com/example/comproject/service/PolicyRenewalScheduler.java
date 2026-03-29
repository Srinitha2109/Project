package com.example.comproject.service;

import com.example.comproject.entity.PolicyApplication;
import com.example.comproject.repository.PolicyApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PolicyRenewalScheduler {
    private final PolicyApplicationRepository applicationRepository;
    private final AppNotificationService notificationService;

    public PolicyRenewalScheduler(PolicyApplicationRepository applicationRepository,
                                  AppNotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void sendRenewalReminders() {
        LocalDate reminderDate = LocalDate.now().plusDays(30);
        
        List<PolicyApplication> policiesToRemind = applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == PolicyApplication.ApplicationStatus.ACTIVE)
                .filter(app -> app.getEndDate() != null && app.getEndDate().equals(reminderDate))
                .filter(app -> app.getRenewalReminderSentAt() == null)
                .toList();

        for (PolicyApplication app : policiesToRemind) {
            notificationService.notify(app.getUser(),
                "Reminder: Your policy " + app.getPolicyNumber() + " is expiring in 30 days (on " + app.getEndDate() + "). You can now initiate renewal from your dashboard.",
                "RENEWAL_REMINDER");
            
            app.setRenewalReminderSentAt(LocalDate.now());
            applicationRepository.save(app);
        }
    }
}
