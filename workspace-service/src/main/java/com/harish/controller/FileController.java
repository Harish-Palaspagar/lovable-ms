package com.harish.controller;

import com.harish.dto.FileTreeDto;
import com.harish.dto.project.FileContentResponse;
import com.harish.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
public class FileController {

    private final ProjectFileService projectFileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId) {

        return ResponseEntity.ok(projectFileService.getFileTree(projectId));

    }

    @GetMapping("/content")
    public ResponseEntity<String> getFile(
            @PathVariable Long projectId,
            @RequestParam String path) {

        return ResponseEntity.ok(projectFileService.getFileContent(projectId, path));

    }

}
