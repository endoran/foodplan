package com.endoran.foodplan.config;

import com.endoran.foodplan.model.Organization;
import com.endoran.foodplan.model.User;
import com.endoran.foodplan.model.UserRole;
import com.endoran.foodplan.repository.OrganizationRepository;
import com.endoran.foodplan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        if (userRepository.count() == 0) {
            log.info("Seeding admin user: {}", adminEmail);
            Organization org = new Organization();
            org.setName("Default");
            org = organizationRepository.save(org);

            User user = new User();
            user.setEmail(adminEmail);
            user.setPasswordHash(passwordEncoder.encode(adminPassword));
            user.setOrgId(org.getId());
            user.setRole(UserRole.OWNER);
            userRepository.save(user);
            log.info("Admin user seeded successfully");
            return;
        }

        // Update existing admin if env vars changed
        var existing = userRepository.findByEmail(adminEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!passwordEncoder.matches(adminPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(adminPassword));
                userRepository.save(user);
                log.info("Updated password for admin user: {}", adminEmail);
            }
        } else {
            // Email changed — find OWNER user and update
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.OWNER)
                    .findFirst()
                    .ifPresent(user -> {
                        log.info("Updating admin email from {} to {}", user.getEmail(), adminEmail);
                        user.setEmail(adminEmail);
                        user.setPasswordHash(passwordEncoder.encode(adminPassword));
                        userRepository.save(user);
                    });
        }
    }
}
