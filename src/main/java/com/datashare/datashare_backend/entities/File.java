package com.datashare.datashare_backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "download_token", nullable = false, unique = true)
    private String downloadToken;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "password")
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "file_tags",
        joinColumns = @JoinColumn(name = "file_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}