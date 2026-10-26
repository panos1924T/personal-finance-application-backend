package gr.cf9.pants.expense_tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Αυτή η κλάση δημιουργήθηκε μόνο για να κληρονομηθεί απο
 * τις άλλες entities καθώς μοιράζεται πεδία που θα έχουν όλα τα
 * άλλα entities έτσι ώστε να υπάρχει σωστό audit.
 */

@MappedSuperclass       // no db table, just inheritance
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // +JpaAuditing -> automate auditing for fields: CreatedDate and LastModifiedDate
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private Instant deletedAt;

    public void softDelete(Instant now) {
        this.deleted = true;
        this.deletedAt = now;
    }

    @PrePersist
    public void initializeUuid() {
        if (uuid == null) uuid = UUID.randomUUID();
    }
}
