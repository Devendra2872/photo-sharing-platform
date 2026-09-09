package com.trizen.photoshare.dto;

import java.time.Instant;
import java.util.List;

public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private String createdByName;
    private Instant createdAt;
    private long totalPhotos;
    private long selectedPhotos;
    private boolean galleryPublished;
    private String gallerySlug;
    private List<UserResponse> members;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getTotalPhotos() {
        return totalPhotos;
    }

    public void setTotalPhotos(long totalPhotos) {
        this.totalPhotos = totalPhotos;
    }

    public long getSelectedPhotos() {
        return selectedPhotos;
    }

    public void setSelectedPhotos(long selectedPhotos) {
        this.selectedPhotos = selectedPhotos;
    }

    public boolean isGalleryPublished() {
        return galleryPublished;
    }

    public void setGalleryPublished(boolean galleryPublished) {
        this.galleryPublished = galleryPublished;
    }

    public String getGallerySlug() {
        return gallerySlug;
    }

    public void setGallerySlug(String gallerySlug) {
        this.gallerySlug = gallerySlug;
    }

    public List<UserResponse> getMembers() {
        return members;
    }

    public void setMembers(List<UserResponse> members) {
        this.members = members;
    }
}
