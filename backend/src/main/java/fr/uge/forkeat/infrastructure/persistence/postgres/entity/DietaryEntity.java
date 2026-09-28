package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "dietary")
public class DietaryEntity {
  @Id
  @Column(columnDefinition = "UUID")
  private UUID id;

  @Column(nullable = false)
  private String name;

  public DietaryEntity() {}

  public DietaryEntity(String name) {
    this.name = name;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @PrePersist
  private void onCreate() {
    if (id == null){
      id = UUID.randomUUID();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof DietaryEntity other)) return false;
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
