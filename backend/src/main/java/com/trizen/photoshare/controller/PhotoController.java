package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.PhotoResponse;
import com.trizen.photoshare.dto.PublishGalleryRequest;
import com.trizen.photoshare.dto.PublishGalleryResponse;
import com.trizen.photoshare.dto.SelectPhotosRequest;
import com.trizen.photoshare.service.GalleryService;
import com.trizen.photoshare.service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}")
public class PhotoController {

    private final PhotoService photoService;
    private final GalleryService galleryService;

    public PhotoController(PhotoService photoService, GalleryService galleryService) {
        this.photoService = photoService;
        this.galleryService = galleryService;
    }

    @PostMapping("/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PhotoResponse> uploadPhotos(
            @PathVariable Long eventId,
            @RequestParam("files") List<MultipartFile> files) {
        return photoService.uploadPhotos(eventId, files);
    }

    @GetMapping("/photos")
    public List<PhotoResponse> listPhotos(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "false") boolean mineOnly) {
        return photoService.listPhotos(eventId, mineOnly);
    }

    @PutMapping("/photos/select")
    public void selectPhotos(@PathVariable Long eventId, @Valid @RequestBody SelectPhotosRequest request) {
        photoService.selectPhotos(eventId, request);
    }

    @PostMapping("/gallery/publish")
    public PublishGalleryResponse publishGallery(
            @PathVariable Long eventId,
            @Valid @RequestBody PublishGalleryRequest request) {
        return galleryService.publishGallery(eventId, request);
    }
}
