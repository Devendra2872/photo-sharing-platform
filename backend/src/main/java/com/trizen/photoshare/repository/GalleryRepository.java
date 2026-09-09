package com.trizen.photoshare.repository;

import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    Optional<Gallery> findBySlug(String slug);
    Optional<Gallery> findByEvent(Event event);
    boolean existsBySlug(String slug);
}
