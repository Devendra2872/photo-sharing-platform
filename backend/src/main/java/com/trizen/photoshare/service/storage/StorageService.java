package com.trizen.photoshare.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {
    String store(MultipartFile file, String key);
    InputStream load(String key);
    String getPublicUrl(String key);
    void delete(String key);
}
