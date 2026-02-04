package com.diddycart.modules.identity.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expires_at")
    private String resetPasswordExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

    @Column(name = "updated_at")
    private String updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

}