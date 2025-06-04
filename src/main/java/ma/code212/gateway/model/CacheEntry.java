package ma.code212.gateway.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cache_entries")
public class CacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "cache_key", unique = true, nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "cache_value", columnDefinition = "TEXT")
    private String cacheValue;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ElementCollection
    @CollectionTable(name = "cache_entry_tags", joinColumns = @JoinColumn(name = "cache_entry_id"))
    @Column(name = "tag")
    private List<String> tags;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
