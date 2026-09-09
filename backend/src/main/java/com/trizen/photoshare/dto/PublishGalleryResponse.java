package com.trizen.photoshare.dto;

public class PublishGalleryResponse {

    private String galleryUrl;
    private String slug;
    private String pin;
    private long selectedPhotoCount;

    public PublishGalleryResponse(String galleryUrl, String slug, String pin, long selectedPhotoCount) {
        this.galleryUrl = galleryUrl;
        this.slug = slug;
        this.pin = pin;
        this.selectedPhotoCount = selectedPhotoCount;
    }

    public String getGalleryUrl() {
        return galleryUrl;
    }

    public void setGalleryUrl(String galleryUrl) {
        this.galleryUrl = galleryUrl;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public long getSelectedPhotoCount() {
        return selectedPhotoCount;
    }

    public void setSelectedPhotoCount(long selectedPhotoCount) {
        this.selectedPhotoCount = selectedPhotoCount;
    }
}
