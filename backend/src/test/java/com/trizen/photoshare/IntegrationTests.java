package com.trizen.photoshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trizen.photoshare.dto.*;
import com.trizen.photoshare.model.Role;
import com.trizen.photoshare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMemberRepository eventMemberRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;
    private Long eventId;

    @BeforeEach
    void setUp() throws Exception {
        galleryRepository.deleteAll();
        photoRepository.deleteAll();
        eventMemberRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName("Admin");
        adminReg.setEmail("admin@test.com");
        adminReg.setPassword("password123");
        adminReg.setRole(Role.ADMIN);
        adminToken = registerAndGetToken(adminReg);

        RegisterRequest memberReg = new RegisterRequest();
        memberReg.setName("Member");
        memberReg.setEmail("member@test.com");
        memberReg.setPassword("password123");
        memberReg.setRole(Role.TEAM_MEMBER);
        memberToken = registerAndGetToken(memberReg);

        CreateEventRequest eventRequest = new CreateEventRequest();
        eventRequest.setName("Test Event");
        eventRequest.setDescription("Integration test event");

        MvcResult eventResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        eventId = objectMapper.readTree(eventResult.getResponse().getContentAsString()).get("id").asLong();

        AddMemberRequest addMember = new AddMemberRequest();
        addMember.setEmail("member@test.com");
        mockMvc.perform(post("/api/events/" + eventId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMember)))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("admin@test.com");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teamMemberCannotCreateEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Unauthorized Event");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberCannotAccessUnassignedEvent() throws Exception {
        mockMvc.perform(get("/api/events/" + eventId + "/photos")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        CreateEventRequest otherEvent = new CreateEventRequest();
        otherEvent.setName("Other Event");
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherEvent)))
                .andExpect(status().isCreated())
                .andReturn();
        Long otherEventId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/events/" + otherEventId + "/photos")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void photoUploadAndGalleryPublishFlow() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isCreated());

        MvcResult photosResult = mockMvc.perform(get("/api/events/" + eventId + "/photos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        Long photoId = objectMapper.readTree(photosResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        SelectPhotosRequest selectRequest = new SelectPhotosRequest();
        selectRequest.setPhotoIds(java.util.List.of(photoId));

        mockMvc.perform(put("/api/events/" + eventId + "/photos/select")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)))
                .andExpect(status().isOk());

        PublishGalleryRequest publishRequest = new PublishGalleryRequest();
        publishRequest.setPin("482917");

        MvcResult publishResult = mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String slug = objectMapper.readTree(publishResult.getResponse().getContentAsString())
                .get("slug").asText();

        GalleryAccessRequest accessRequest = new GalleryAccessRequest();
        accessRequest.setPin("482917");

        MvcResult accessResult = mockMvc.perform(post("/api/public/gallery/" + slug + "/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accessRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String galleryToken = objectMapper.readTree(accessResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/public/gallery/" + slug + "/photos")
                        .header("X-Gallery-Token", galleryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(photoId));
    }

    @Test
    void incorrectGalleryPinReturns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        MvcResult photosResult = mockMvc.perform(get("/api/events/" + eventId + "/photos")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        Long photoId = objectMapper.readTree(photosResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        SelectPhotosRequest selectRequest = new SelectPhotosRequest();
        selectRequest.setPhotoIds(java.util.List.of(photoId));
        mockMvc.perform(put("/api/events/" + eventId + "/photos/select")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)));

        PublishGalleryRequest publishRequest = new PublishGalleryRequest();
        publishRequest.setPin("123456");
        MvcResult publishResult = mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andReturn();
        String slug = objectMapper.readTree(publishResult.getResponse().getContentAsString())
                .get("slug").asText();

        GalleryAccessRequest accessRequest = new GalleryAccessRequest();
        accessRequest.setPin("999999");

        mockMvc.perform(post("/api/public/gallery/" + slug + "/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accessRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberCannotPublishGallery() throws Exception {
        PublishGalleryRequest publishRequest = new PublishGalleryRequest();
        publishRequest.setPin("482917");

        mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isForbidden());
    }

    private String registerAndGetToken(RegisterRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
