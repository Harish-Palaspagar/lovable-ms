package com.harish.impl;

import com.harish.client.AccountClient;
import com.harish.dto.UserDto;
import com.harish.dto.member.InviteMemberRequest;
import com.harish.dto.member.MemberResponse;
import com.harish.dto.member.UpdateMemberRoleRequest;
import com.harish.entity.Project;
import com.harish.entity.ProjectMember;
import com.harish.entity.ProjectMemberId;
import com.harish.mapper.ProjectMemberMapper;
import com.harish.repository.ProjectMemberRepository;
import com.harish.repository.ProjectRepository;
import com.harish.security.AuthUtil;
import com.harish.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;

    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {

        return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .toList();

    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {

        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        UserDto invitee = accountClient.getUserByEmail(request.username())
                .orElseThrow(
                        () -> new RuntimeException("User not found with email: " + request.username())
                );
        if (invitee.id().equals(userId)) {
            throw new RuntimeException("Cannot invite yourself");
        }
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.id());
        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Cannot invite once again");
        }
        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();
        projectMemberRepository.save(member);
        return projectMemberMapper.toProjectMemberResponseFromMember(member);

    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {

        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);

    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {

        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        if (!projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Member not found in project");
        }
        projectMemberRepository.deleteById(projectMemberId);

    }

    public Project getAccessibleProjectById(Long projectId, Long userId) {

        return projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();

    }

}
