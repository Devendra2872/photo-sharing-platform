package com.trizen.photoshare.repository;

import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.Photo;
import com.trizen.photoshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByEventOrderByCreatedAtDesc(Event event);
    List<Photo> findByEventAndUploadedByOrderByCreatedAtDesc(Event event, User user);
    List<Photo> findByEventAndSelectedForGalleryTrueOrderByCreatedAtDesc(Event event);
    long countByEvent(Event event);
    long countByEventAndSelectedForGalleryTrue(Event event);
}
