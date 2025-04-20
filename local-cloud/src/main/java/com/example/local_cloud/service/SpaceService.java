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
public class SpaceService {

    @Value("${notes.base-path}")
    private String basePath;

    public List<String> listSpaces() {
        File[] spaces = new File(basePath).listFiles(File::isDirectory);
        return spaces != null
                ? Arrays.stream(spaces).map(File::getName).collect(Collectors.toList())
                : List.of();
    }

    public void createSpace(String name) throws IOException {
        Path spacePath = Paths.get(basePath, name);
        Files.createDirectories(spacePath);
    }

    public void deleteSpace(String name) throws IOException {
        Path spacePath = Paths.get(basePath, name);
        Files.walk(spacePath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public void renameSpace(String oldName, String newName) throws IOException {
    Path oldPath = Paths.get(basePath, oldName);
    Path newPath = Paths.get(basePath, newName);
    if (Files.exists(oldPath)) {
        Files.move(oldPath, newPath);
    }
}

}
