package com.company.ra.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entity representing blacklisted public keys
 */
@Entity
@Table(name = "public_key_blacklist", indexes = {
    @Index(name = "idx_public_key_hash", columnList = "publicKeyHash", unique = true)
})
public class PublicKeyBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String publicKeyHash;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 100)
    private String addedBy;

    @Column(nullable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }

    public PublicKeyBlacklist() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return "PublicKeyBlacklist{" +
                "id=" + id +
                ", publicKeyHash='" + publicKeyHash + '\'' +
                ", reason='" + reason + '\'' +
                ", addedBy='" + addedBy + '\'' +
                ", addedAt=" + addedAt +
                '}';
    }
}
