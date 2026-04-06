package com.harish.service;

import com.harish.dto.project.ProjectRequest;
import com.harish.dto.project.ProjectResponse;
import com.harish.dto.project.ProjectSummaryResponse;
import com.harish.enums.ProjectPermission;

import java.util.List;

public interface ProjectService {

    List<ProjectSummaryResponse> getUserProjects();

    ProjectSummaryResponse getUserProjectById(Long id);

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(Long id, ProjectRequest request);

    void softDelete(Long id);

    boolean hasPermission(Long projectId, ProjectPermission permission);

}
