package com.harish;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConfigServiceApplicationTests {

    @Test
    void contextLoads() {
        String workflowTriggerCheck = "config-service workflow trigger check";
        String workflowPermissionFixCheck = "config-service rerun after mvnw chmod fix";
    }

}
