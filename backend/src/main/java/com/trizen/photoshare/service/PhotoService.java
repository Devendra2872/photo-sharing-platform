package com.trizen.photoshare.service;

import com.trizen.photoshare.dto.PhotoResponse;
import com.trizen.photoshare.dto.SelectPhotosRequest;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.ResourceNotFoundException;
import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.Photo;
import com.trizen.photoshare.model.Role;
import com.trizen.photoshare.model.User;
import com.trizen.photoshare.repository.PhotoRepository;
import com.trizen.photoshare.security.SecurityUtils;
import com.trizen.photoshare.service.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/jpg");

    private final PhotoRepository photoRepository;
    private final EventService eventService;
    private final StorageService storageService;
    private final SecurityUtils securityUtils;

    public PhotoService(PhotoRepository photoRepository,
                        EventService eventService,
                        StorageService storageService,
                        SecurityUtils securityUtils) {
        this.photoRepository = photoRepository;
        this.eventService = eventService;
        this.storageService = storageService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public List<PhotoResponse> uploadPhotos(Long eventId, List<MultipartFile> files) {
        User user = securityUtils.getCurrentUser().getUser();
        Event event = eventService.findEventOrThrow(eventId);
        eventService.assertEventAccess(event, user);

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files provided");
        }

        List<PhotoResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            String storageKey = "events/" + eventId + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
            storageService.store(file, storageKey);

            Photo photo = new Photo();
            photo.setEvent(event);
            photo.setUploadedBy(user);
            photo.setFilename(file.getOriginalFilename());
            photo.setStorageKey(storageKey);
            photo.setFileSize(file.getSize());
            photo.setContentType(file.getContentType());
            photoRepository.save(photo);
            responses.add(toResponse(photo));
        }
        return responses;
    }

    public List<PhotoResponse> listPhotos(Long eventId, boolean mineOnly) {
        User user = securityUtils.getCurrentUser().getUser();
        Event event = eventService.findEventOrThrow(eventId);
        eventService.assertEventAccess(event, user);

        List<Photo> photos;
        if (user.getRole() == Role.TEAM_MEMBER || mineOnly) {
            photos = photoRepository.findByEventAndUploadedByOrderByCreatedAtDesc(event, user);
        } else {
            photos = photoRepository.findByEventOrderByCreatedAtDesc(event);
        }

        return photos.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void selectPhotos(Long eventId, SelectPhotosRequest request) {
        User admin = securityUtils.getCurrentUser().getUser();
        Event event = eventService.findEventOrThrow(eventId);
        eventService.assertAdminOwnsEvent(event, admin);

        List<Photo> allPhotos = photoRepository.findByEventOrderByCreatedAtDesc(event);
        allPhotos.forEach(p -> p.setSelectedForGallery(false));

        for (Long photoId : request.getPhotoIds()) {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Photo not found: " + photoId));
            if (!photo.getEvent().getId().equals(eventId)) {
                throw new BadRequestException("Photo does not belong to this event");
            }
            photo.setSelectedForGallery(true);
        }
        photoRepository.saveAll(allPhotos);
    }

    public PhotoResponse toResponse(Photo photo) {
        PhotoResponse response = new PhotoResponse();
        response.setId(photo.getId());
        response.setFilename(photo.getFilename());
        response.setUrl(storageService.getPublicUrl(photo.getStorageKey()));
        response.setFileSize(photo.getFileSize());
        response.setContentType(photo.getContentType());
        response.setSelectedForGallery(photo.isSelectedForGallery());
        response.setUploadedByName(photo.getUploadedBy().getName());
        response.setCreatedAt(photo.getCreatedAt());
        return response;
    }

    public List<PhotoResponse> getSelectedPhotosForEvent(Event event) {
        return photoRepository.findByEventAndSelectedForGalleryTrueOrderByCreatedAtDesc(event)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Empty file upload");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only image files (JPEG, PNG, WebP, GIF) are allowed");
        }
        if (file.getSize() > 25 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 25MB limit");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
