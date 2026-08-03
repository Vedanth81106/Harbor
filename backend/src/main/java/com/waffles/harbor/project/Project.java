package com.waffles.harbor.project;

import com.waffles.harbor.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uk_user_project_name",
                        columnNames = {"user_id", "project_name"}
                )
        }
)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID projectId;

    @NotBlank(message = "Project name cannot be blank!")
    @Size(max = 100)
    @Column(nullable = false, name = "project_name", length = 100)
    private String projectName;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) // Many projects to one user
    @JoinColumn(name = "user_id", nullable = false)// Maps to user id field in User class
    private User user;

}
