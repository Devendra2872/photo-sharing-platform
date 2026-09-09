package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.GalleryAccessRequest;
import com.trizen.photoshare.dto.GalleryAccessResponse;
import com.trizen.photoshare.dto.PhotoResponse;
import com.trizen.photoshare.service.GalleryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/gallery")
public class PublicGalleryController {

    private final GalleryService galleryService;

    public PublicGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @PostMapping("/{slug}/access")
    public GalleryAccessResponse accessGallery(
            @PathVariable String slug,
            @Valid @RequestBody GalleryAccessRequest request) {
        return galleryService.accessGallery(slug, request);
    }

    @GetMapping("/{slug}/photos")
    public List<PhotoResponse> getPhotos(
            @PathVariable String slug,
            @RequestHeader("X-Gallery-Token") String accessToken) {
        return galleryService.getGalleryPhotos(slug, accessToken);
    }
}
