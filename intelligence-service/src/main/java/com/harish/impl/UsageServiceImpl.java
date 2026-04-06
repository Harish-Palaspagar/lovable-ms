package com.harish.impl;

import com.harish.client.AccountClient;
import com.harish.dto.PlanDto;
import com.harish.entity.UsageLog;
import com.harish.repository.UsageLogRepository;
import com.harish.security.AuthUtil;
import com.harish.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {

    private final UsageLogRepository usageLogRepository;
    private final AuthUtil authUtil;
    private final AccountClient accountClient;

    @Override
    public void recordTokenUsage(Long userId, int actualTokens) {

        LocalDate today = LocalDate.now();
        UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today).
                orElseGet(() -> createNewDailyLog(userId, today));
        todayLog.setTokensUsed(todayLog.getTokensUsed() + actualTokens);
        usageLogRepository.save(todayLog);

    }

    @Override
    public void checkDailyTokensUsage() {

        Long userId = authUtil.getCurrentUserId();
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();
        LocalDate today = LocalDate.now();
        UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today).
                orElseGet(() -> createNewDailyLog(userId, today));
        if (plan.unlimitedAi()) return;
        int currentUsage = todayLog.getTokensUsed();
        int limit = plan.maxTokensPerDay();
        if (currentUsage >= limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit reached, Upgrade now");
        }

    }

    private UsageLog createNewDailyLog(Long userId, LocalDate date) {

        UsageLog newLog = UsageLog.builder()
                .userId(userId)
                .date(date)
                .tokensUsed(0)
                .build();
        return usageLogRepository.save(newLog);

    }

}
