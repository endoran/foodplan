package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.AuthResponse;
import com.endoran.foodplan.dto.LoginRequest;
import com.endoran.foodplan.dto.RegisterRequest;
import com.endoran.foodplan.model.Organization;
import com.endoran.foodplan.model.User;
import com.endoran.foodplan.model.UserRole;
import com.endoran.foodplan.repository.OrganizationRepository;
import com.endoran.foodplan.repository.UserRepository;
import com.endoran.foodplan.security.JwtConfig;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtEncoder jwtEncoder,
                       JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtConfig = jwtConfig;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Organization org = new Organization();
        org.setName(request.orgName());
        org = organizationRepository.save(org);

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOrgId(org.getId());
        user.setRole(UserRole.OWNER);
        user = userRepository.save(user);

        String token = generateToken(user);
        return new AuthResponse(token, jwtConfig.getExpirationSeconds(), user.getEmail(), user.getOrgId(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = generateToken(user);
        return new AuthResponse(token, jwtConfig.getExpirationSeconds(), user.getEmail(), user.getOrgId(), user.getRole().name());
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("orgId", user.getOrgId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtConfig.getExpirationSeconds()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
