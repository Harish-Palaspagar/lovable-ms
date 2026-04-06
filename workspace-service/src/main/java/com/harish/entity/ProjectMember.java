package com.harish.entity;

import com.harish.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_members")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectMember {

    @EmbeddedId
    ProjectMemberId id;
    @ManyToOne
    @MapsId("projectId")
    Project project;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProjectRole projectRole;
    Instant invitedAt;
    Instant acceptedAt;

}
