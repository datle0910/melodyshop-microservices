package com.melodyshop.auth.service;

import com.melodyshop.auth.entity.Role;
import com.melodyshop.auth.entity.User;
import com.melodyshop.auth.repository.RoleRepository;
import com.melodyshop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAutoInitService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@melodyshop.com}")
    private String adminEmail;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.full-name:Administrator}")
    private String adminFullName;

    @Value("${admin.enabled:true}")
    private boolean adminEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminEnabled) {
            log.info("Admin auto-init is disabled (admin.enabled=false). Skipping.");
            return;
        }

        log.info("=== Admin Auto-Init ===");
        log.info("Checking if admin account needs to be created...");
        log.info("Admin email: {}", adminEmail);

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists for email: {}. Skipping creation.", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    log.warn("ROLE_ADMIN not found in database. Creating it now...");
                    Role newRole = Role.builder()
                            .name("ROLE_ADMIN")
                            .description("System Administrator role")
                            .isSystem(true)
                            .build();
                    return roleRepository.save(newRole);
                });

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> {
                    log.warn("ROLE_CUSTOMER not found. Creating it now...");
                    Role newRole = Role.builder()
                            .name("ROLE_CUSTOMER")
                            .description("Default customer role")
                            .isSystem(true)
                            .build();
                    return roleRepository.save(newRole);
                });

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .isActive(true)
                .isVerified(true)
                .loyaltyPoints(0)
                .roles(Set.of(adminRole, customerRole))
                .build();

        userRepository.save(admin);

        log.info("==============================================");
        log.info("  ADMIN ACCOUNT CREATED SUCCESSFULLY!");
        log.info("  Email:    {}", adminEmail);
        log.info("  Password: {}", adminPassword);
        log.info("  (This is shown only during first creation)");
        log.info("==============================================");
    }
}
