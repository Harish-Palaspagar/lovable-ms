package com.harish.service;

public interface UsageService {

    void recordTokenUsage(Long userId, int actualTokens);

    void checkDailyTokensUsage();

}
