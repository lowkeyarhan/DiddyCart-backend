package com.diddycart.modules.identity.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "gstin", nullable = false, unique = true)
    private String gstin;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}