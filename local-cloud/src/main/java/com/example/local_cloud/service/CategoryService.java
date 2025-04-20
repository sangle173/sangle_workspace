package com.example.local_cloud.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class CategoryService {

    @Value("${notes.base-path}")
    private String basePath;

    public List<String> listCategories(String space) {
        File spaceDir = Paths.get(basePath, space).toFile();
        if (!spaceDir.exists()) return List.of();
        File[] dirs = spaceDir.listFiles(File::isDirectory);
        return dirs != null
                ? Arrays.stream(dirs).map(File::getName).collect(Collectors.toList())
                : List.of();
    }

    public void createCategory(String space, String name) throws Exception {
        Files.createDirectories(Paths.get(basePath, space, name));
    }

    public void deleteCategory(String space, String name) throws Exception {
        Path catPath = Paths.get(basePath, space, name);
        Files.walk(catPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public void renameCategory(String space, String oldName, String newName) throws IOException {
    Path oldPath = Paths.get(basePath, space, oldName);
    Path newPath = Paths.get(basePath, space, newName);
    if (Files.exists(oldPath)) {
        Files.move(oldPath, newPath);
    }
}

}
