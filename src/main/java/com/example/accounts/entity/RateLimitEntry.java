package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rate_limit_entries", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_window_start", columnList = "window_start")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class RateLimitEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 0;

    @Column(name = "window_start", nullable = false)
    private Long windowStart;

    @Column(name = "window_end", nullable = false)
    private Long windowEnd;
}

