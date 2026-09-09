package com.trizen.photoshare.repository;

import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    @Query("""
            SELECT e FROM Event e
            JOIN EventMember em ON em.event = e
            WHERE em.user = :user
            ORDER BY e.createdAt DESC
            """)
    List<Event> findAssignedEvents(@Param("user") User user);
}
