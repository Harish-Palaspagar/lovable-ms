package com.harish.mapper;

import com.harish.dto.project.ProjectResponse;
import com.harish.dto.project.ProjectSummaryResponse;
import com.harish.entity.Project;
import com.harish.enums.ProjectRole;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse toProjectResponse(Project project);

    ProjectSummaryResponse toProjectSummaryResponse(Project project, ProjectRole role);

    List<ProjectSummaryResponse> toListOfProjectSummaryResponse(List<Project> projects);

}
