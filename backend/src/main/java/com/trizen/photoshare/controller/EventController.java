package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.*;
import com.trizen.photoshare.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<EventResponse> listEvents() {
        return eventService.listEvents();
    }

    @GetMapping("/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @PostMapping("/{eventId}/members")
    public EventResponse addMember(@PathVariable Long eventId, @Valid @RequestBody AddMemberRequest request) {
        return eventService.addMember(eventId, request);
    }
}
