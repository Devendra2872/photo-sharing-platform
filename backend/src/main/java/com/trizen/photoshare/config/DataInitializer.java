package com.trizen.photoshare.config;

import com.trizen.photoshare.model.Event;
import com.trizen.photoshare.model.EventMember;
import com.trizen.photoshare.model.Role;
import com.trizen.photoshare.model.User;
import com.trizen.photoshare.repository.EventMemberRepository;
import com.trizen.photoshare.repository.EventRepository;
import com.trizen.photoshare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               EventRepository eventRepository,
                               EventMemberRepository eventMemberRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail("admin@demo.com")) {
                return;
            }

            User admin = new User();
            admin.setName("Demo Admin");
            admin.setEmail("admin@demo.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            User member = new User();
            member.setName("Demo Team Member");
            member.setEmail("member@demo.com");
            member.setPasswordHash(passwordEncoder.encode("member123"));
            member.setRole(Role.TEAM_MEMBER);
            userRepository.save(member);

            Event event = new Event();
            event.setName("Arjun & Priya Wedding");
            event.setDescription("Demo wedding event for photo sharing");
            event.setCreatedBy(admin);
            eventRepository.save(event);

            EventMember eventMember = new EventMember();
            eventMember.setEvent(event);
            eventMember.setUser(member);
            eventMemberRepository.save(eventMember);

            log.info("Demo data seeded: admin@demo.com / admin123, member@demo.com / member123");
        };
    }
}
