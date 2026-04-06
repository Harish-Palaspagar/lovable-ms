package com.harish.controller;

import com.harish.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

//    @GetMapping("/today")
//    public ResponseEntity<UsageTodayResponse> getTodayUsage() {
//        Long userId = 1L;
////        return ResponseEntity.ok(usageService.getTodayUsageOfUser(userId));
//        return null;
//    }

}
