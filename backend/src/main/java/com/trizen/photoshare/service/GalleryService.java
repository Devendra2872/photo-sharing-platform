package com.trizen.photoshare.service;

import com.trizen.photoshare.dto.*;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.ResourceNotFoundException;
import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.Gallery;
import com.trizen.photoshare.model.User;
import com.trizen.photoshare.repository.GalleryRepository;
import com.trizen.photoshare.repository.PhotoRepository;
import com.trizen.photoshare.security.JwtTokenProvider;
import com.trizen.photoshare.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final PhotoRepository photoRepository;
    private final PhotoService photoService;
    private final EventService eventService;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public GalleryService(GalleryRepository galleryRepository,
                          PhotoRepository photoRepository,
                          PhotoService photoService,
                          EventService eventService,
                          SecurityUtils securityUtils,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.galleryRepository = galleryRepository;
        this.photoRepository = photoRepository;
        this.photoService = photoService;
        this.eventService = eventService;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public PublishGalleryResponse publishGallery(Long eventId, PublishGalleryRequest request) {
        User admin = securityUtils.getCurrentUser().getUser();
        Event event = eventService.findEventOrThrow(eventId);
        eventService.assertAdminOwnsEvent(event, admin);

        long selectedCount = photoRepository.countByEventAndSelectedForGalleryTrue(event);
        if (selectedCount == 0) {
            throw new BadRequestException("Select at least one photo before publishing");
        }

        Gallery gallery = galleryRepository.findByEvent(event).orElseGet(() -> {
            Gallery g = new Gallery();
            g.setEvent(event);
            g.setSlug(eventService.generateUniqueSlug());
            return g;
        });

        gallery.setPinHash(passwordEncoder.encode(request.getPin()));
        gallery.setPublished(true);
        gallery.setPublishedAt(Instant.now());
        galleryRepository.save(gallery);

        return new PublishGalleryResponse(
                eventService.buildGalleryUrl(gallery.getSlug()),
                gallery.getSlug(),
                request.getPin(),
                selectedCount
        );
    }

    public GalleryAccessResponse accessGallery(String slug, GalleryAccessRequest request) {
        Gallery gallery = galleryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.isPublished()) {
            throw new ForbiddenException("Gallery is not published yet");
        }

        if (!passwordEncoder.matches(request.getPin(), gallery.getPinHash())) {
            throw new ForbiddenException("Incorrect PIN");
        }

        List<PhotoResponse> photos = photoService.getSelectedPhotosForEvent(gallery.getEvent());
        String accessToken = jwtTokenProvider.generateGalleryToken(slug);

        GalleryAccessResponse response = new GalleryAccessResponse();
        response.setAccessToken(accessToken);
        response.setEventName(gallery.getEvent().getName());
        response.setPhotoCount(photos.size());
        response.setPhotos(photos);
        return response;
    }

    public List<PhotoResponse> getGalleryPhotos(String slug, String accessToken) {
        validateGalleryToken(slug, accessToken);

        Gallery gallery = galleryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.isPublished()) {
            throw new ForbiddenException("Gallery is not published");
        }

        return photoService.getSelectedPhotosForEvent(gallery.getEvent());
    }

    public void validateGalleryToken(String slug, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ForbiddenException("Gallery access token required");
        }
        try {
            var claims = jwtTokenProvider.parseToken(accessToken);
            if (!jwtTokenProvider.isGalleryToken(claims) || !slug.equals(jwtTokenProvider.getSubject(claims))) {
                throw new ForbiddenException("Invalid gallery access token");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new ForbiddenException("Invalid or expired gallery access token");
        }
    }
}
