package com.harish.impl;

import com.harish.client.AccountClient;
import com.harish.dto.PlanDto;
import com.harish.dto.project.ProjectRequest;
import com.harish.dto.project.ProjectResponse;
import com.harish.dto.project.ProjectSummaryResponse;
import com.harish.entity.Project;
import com.harish.entity.ProjectMember;
import com.harish.entity.ProjectMemberId;
import com.harish.enums.ProjectPermission;
import com.harish.enums.ProjectRole;
import com.harish.error.BadRequestException;
import com.harish.error.ResourceNotFoundException;
import com.harish.mapper.ProjectMapper;
import com.harish.repository.ProjectMemberRepository;
import com.harish.repository.ProjectRepository;
import com.harish.security.AuthUtil;
import com.harish.security.SecurityExpressions;
import com.harish.service.ProjectService;
import com.harish.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        if (!canCreateProject()) {
            throw new BadRequestException("User cannot create a New project with current Plan, Upgrade plan now.");
        }
        Long ownerUserId = authUtil.getCurrentUserId();
        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();
        project = projectRepository.save(project);
        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), ownerUserId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);
        log.info("Project {} created, initializing from template...", project.getId());
        projectTemplateService.initializeProjectFromTemplate(project.getId());
        log.info("Project {} template initialization complete.", project.getId());
        return projectMapper.toProjectResponse(project);

    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {

        Long userId = authUtil.getCurrentUserId();
        var projectsWithRoles = projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRoles.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(), p.getRole()))
                .toList();

    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project Not Found"));
        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole());

    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {

        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        project.setName(request.name());
        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);

    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);

    }

    @Override
    public boolean hasPermission(Long projectId, ProjectPermission permission) {

        return securityExpressions.hasPermission(projectId, permission);

    }

    public Project getAccessibleProjectById(Long projectId, Long userId) {

        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

    }

    private boolean canCreateProject() {

        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();
        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);
        return ownedCount < maxAllowed;

    }

}
