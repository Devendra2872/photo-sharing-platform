package com.trizen.photoshare.repository;

import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.EventMember;
import com.trizen.photoshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {
    List<EventMember> findByEvent(Event event);
    Optional<EventMember> findByEventAndUser(Event event, User user);
    boolean existsByEventAndUser(Event event, User user);
}
