package com.harish.dto;

import java.util.List;

public record FileTreeDto(
        List<FileNode> files
) {
}