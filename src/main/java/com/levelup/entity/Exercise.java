package com.levelup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "exercise", indexes = @Index(name = "idx_exercise_name", columnList = "name"))
public class Exercise {
    @Id @UuidGenerator private UUID id;
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(length = 1000) private String description;
    @Column(name = "muscle_group", length = 80) private String muscleGroup;
    @Column(length = 80) private String equipment;
    @Column(name = "movement_type", length = 40) private String movementType;
    @Column(name = "is_compound", nullable = false) private boolean compound;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Exercise() {}
    public Exercise(String name, String description, String muscleGroup, String equipment, String movementType, boolean compound) {
        this.name = name; this.description = description; this.muscleGroup = muscleGroup;
        this.equipment = equipment; this.movementType = movementType; this.compound = compound;
    }
    @PrePersist void createTimestamps() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMuscleGroup() { return muscleGroup; }
    public String getEquipment() { return equipment; }
    public String getMovementType() { return movementType; }
    public boolean isCompound() { return compound; }
}
