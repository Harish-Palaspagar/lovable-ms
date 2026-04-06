package com.harish.impl;

import com.harish.entity.Project;
import com.harish.entity.ProjectFile;
import com.harish.error.ResourceNotFoundException;
import com.harish.repository.ProjectFileRepository;
import com.harish.repository.ProjectRepository;
import com.harish.service.ProjectTemplateService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private static final String TEMPLATE_BUCKET = "starter-projects";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter";
    private final MinioClient minioClient;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    @Override
    public void initializeProjectFromTemplate(Long projectId) {

        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString()));
        try {
            ensureBucketExists(TEMPLATE_BUCKET);
            ensureBucketExists(TARGET_BUCKET);

            String prefix = TEMPLATE_NAME + "/";
            log.info("Listing template objects from bucket='{}', prefix='{}'", TEMPLATE_BUCKET, prefix);

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            List<ProjectFile> filesToSave = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                String sourceKey = item.objectName();

                // Skip directory markers (zero-byte objects with trailing slash)
                if (sourceKey.endsWith("/")) {
                    log.debug("Skipping directory marker: {}", sourceKey);
                    continue;
                }

                String cleanPath = sourceKey.replaceFirst(TEMPLATE_NAME + "/", "");

                // Skip if cleanPath is empty (the root prefix itself)
                if (cleanPath.isEmpty()) {
                    log.debug("Skipping empty cleanPath for sourceKey: {}", sourceKey);
                    continue;
                }

                String destKey = projectId + "/" + cleanPath;
                log.info("Copying template file: {} -> {}/{}", sourceKey, TARGET_BUCKET, destKey);

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(TARGET_BUCKET)
                                .object(destKey)
                                .source(
                                        CopySource.builder()
                                                .bucket(TEMPLATE_BUCKET)
                                                .object(sourceKey)
                                                .build()
                                )
                                .build()
                );
                ProjectFile pf = ProjectFile.builder()
                        .project(project)
                        .path(cleanPath)
                        .minioObjectKey(destKey)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                filesToSave.add(pf);
            }

            if (filesToSave.isEmpty()) {
                log.warn("No template files found in bucket='{}' with prefix='{}'. "
                                + "The project will be created without initial files. "
                                + "Make sure the template '{}' exists in the '{}' bucket.",
                        TEMPLATE_BUCKET, prefix, TEMPLATE_NAME, TEMPLATE_BUCKET);
            } else {
                log.info("Successfully copied {} template files for project {}", filesToSave.size(), projectId);
            }

            projectFileRepository.saveAll(filesToSave);
        } catch (Exception e) {
            log.error("Failed to initialize project {} from template: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize project from template", e);
        }

    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                log.info("Bucket '{}' does not exist, creating it...", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Bucket '{}' created successfully.", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket '{}' exists: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to ensure bucket exists: " + bucketName, e);
        }
    }

}






















