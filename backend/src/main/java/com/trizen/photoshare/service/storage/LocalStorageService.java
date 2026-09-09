package com.trizen.photoshare.service.storage;

import com.trizen.photoshare.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path rootPath;
    private final String baseUrl;

    public LocalStorageService(
            @Value("${app.storage.local.path}") String localPath,
            @Value("${server.port:8080}") int port) {
        this.rootPath = Paths.get(localPath).toAbsolutePath().normalize();
        this.baseUrl = "http://localhost:" + port + "/api/files/";
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String key) {
        try {
            Path destination = rootPath.resolve(key).normalize();
            if (!destination.startsWith(rootPath)) {
                throw new BadRequestException("Invalid storage key");
            }
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            Path file = rootPath.resolve(key).normalize();
            if (!Files.exists(file) || !file.startsWith(rootPath)) {
                throw new BadRequestException("File not found");
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file");
        }
    }

    @Override
    public String getPublicUrl(String key) {
        return baseUrl + key;
    }

    @Override
    public void delete(String key) {
        try {
            Path file = rootPath.resolve(key).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BadRequestException("Failed to delete file");
        }
    }
}
