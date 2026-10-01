package com.traceusage.traceusage.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static User register(String name, String email, String passwordHash) {
        User user = new User();
        user.name = Objects.requireNonNull(name);
        user.email = Objects.requireNonNull(email);
        user.passwordHash = Objects.requireNonNull(passwordHash);
        user.createdAt = Instant.now();
        return user;
    }
}
