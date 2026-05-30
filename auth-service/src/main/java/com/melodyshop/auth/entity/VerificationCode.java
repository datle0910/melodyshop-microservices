package com.melodyshop.auth.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VerificationCode extends BaseEntity {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "purpose", nullable = false, length = 20)
    @Builder.Default
    private String purpose = "REGISTRATION";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
