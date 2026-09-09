package com.trizen.photoshare.service;

import com.trizen.photoshare.dto.*;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.ResourceNotFoundException;
import com.trizen.photoshare.model.*;
import com.trizen.photoshare.repository.*;
import com.trizen.photoshare.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final PhotoRepository photoRepository;
    private final GalleryRepository galleryRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EventService(EventRepository eventRepository,
                        EventMemberRepository eventMemberRepository,
                        PhotoRepository photoRepository,
                        GalleryRepository galleryRepository,
                        UserRepository userRepository,
                        SecurityUtils securityUtils) {
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.photoRepository = photoRepository;
        this.galleryRepository = galleryRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        User admin = securityUtils.getCurrentUser().getUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can create events");
        }

        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCreatedBy(admin);
        eventRepository.save(event);
        return toResponse(event, admin);
    }

    public List<EventResponse> listEvents() {
        User user = securityUtils.getCurrentUser().getUser();
        List<Event> events;
        if (user.getRole() == Role.ADMIN) {
            events = eventRepository.findByCreatedByOrderByCreatedAtDesc(user);
        } else {
            events = eventRepository.findAssignedEvents(user);
        }
        return events.stream().map(e -> toResponse(e, user)).collect(Collectors.toList());
    }

    public EventResponse getEvent(Long eventId) {
        User user = securityUtils.getCurrentUser().getUser();
        Event event = findEventOrThrow(eventId);
        assertEventAccess(event, user);
        return toResponse(event, user);
    }

    @Transactional
    public EventResponse addMember(Long eventId, AddMemberRequest request) {
        User admin = securityUtils.getCurrentUser().getUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can add team members");
        }

        Event event = findEventOrThrow(eventId);
        if (!event.getCreatedBy().getId().equals(admin.getId())) {
            throw new ForbiddenException("You can only manage your own events");
        }

        User member = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (member.getRole() != Role.TEAM_MEMBER) {
            throw new BadRequestException("User must have TEAM_MEMBER role");
        }

        if (eventMemberRepository.existsByEventAndUser(event, member)) {
            throw new BadRequestException("User is already a member of this event");
        }

        EventMember eventMember = new EventMember();
        eventMember.setEvent(event);
        eventMember.setUser(member);
        eventMemberRepository.save(eventMember);

        return toResponse(event, admin);
    }

    public Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    public void assertEventAccess(Event event, User user) {
        if (user.getRole() == Role.ADMIN && event.getCreatedBy().getId().equals(user.getId())) {
            return;
        }
        if (eventMemberRepository.existsByEventAndUser(event, user)) {
            return;
        }
        throw new ForbiddenException("You do not have access to this event");
    }

    public void assertAdminOwnsEvent(Event event, User user) {
        if (user.getRole() != Role.ADMIN || !event.getCreatedBy().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the event admin can perform this action");
        }
    }

    private EventResponse toResponse(Event event, User currentUser) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setCreatedByName(event.getCreatedBy().getName());
        response.setCreatedAt(event.getCreatedAt());
        response.setTotalPhotos(photoRepository.countByEvent(event));
        response.setSelectedPhotos(photoRepository.countByEventAndSelectedForGalleryTrue(event));

        galleryRepository.findByEvent(event).ifPresent(gallery -> {
            response.setGalleryPublished(gallery.isPublished());
            response.setGallerySlug(gallery.getSlug());
        });

        if (currentUser.getRole() == Role.ADMIN) {
            List<UserResponse> members = eventMemberRepository.findByEvent(event).stream()
                    .map(em -> new UserResponse(
                            em.getUser().getId(),
                            em.getUser().getName(),
                            em.getUser().getEmail(),
                            em.getUser().getRole()))
                    .collect(Collectors.toList());
            response.setMembers(members);
        }

        return response;
    }

    public String generateUniqueSlug() {
        String slug;
        do {
            slug = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (galleryRepository.existsBySlug(slug));
        return slug;
    }

    public String generatePin() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String buildGalleryUrl(String slug) {
        return frontendUrl + "/gallery/" + slug;
    }
}
