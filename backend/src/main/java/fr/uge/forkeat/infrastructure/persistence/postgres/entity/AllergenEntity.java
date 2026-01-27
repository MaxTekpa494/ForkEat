package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.AllergenSeverity;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "allergens")
public class AllergenEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;
    @Column(nullable = false)
    private String  name;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AllergenSeverity severity;
//    @Column(name = "created_at", nullable = false)
//    private Instant createdAt;
//    @Column(name = "update_at")
//    private Instant updateAt;

    public AllergenEntity(){}

    public AllergenEntity(String name, AllergenSeverity severity) {
        this.name = name;
        this.severity = severity;
    }

    @PrePersist
    private void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AllergenSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AllergenSeverity severity) {
        this.severity = severity;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if (!(o instanceof AllergenEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
