package com.trizen.photoshare.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SelectPhotosRequest {

    @NotEmpty
    private List<Long> photoIds;

    public List<Long> getPhotoIds() {
        return photoIds;
    }

    public void setPhotoIds(List<Long> photoIds) {
        this.photoIds = photoIds;
    }
}
