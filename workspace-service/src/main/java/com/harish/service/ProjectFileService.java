package com.harish.service;


import com.harish.dto.FileTreeDto;
import com.harish.dto.project.FileContentResponse;

public interface ProjectFileService {

    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFile(Long projectId, String filePath, String fileContent);

}
