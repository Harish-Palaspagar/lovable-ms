package com.harish.service;

import com.harish.dto.deploy.DeployResponse;

public interface DeploymentService {

    DeployResponse deploy(Long projectId);

}
