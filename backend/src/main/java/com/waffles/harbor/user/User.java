package com.waffles.harbor.user;

import com.waffles.harbor.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
// Because User references Project and Project references User, calling toString() or hashCode() (which Hibernate often does under the hood)
// will trigger an infinite loop and crash the application with a StackOverflowError. This is the reason why @Data annotation of Lombok is not used here.
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @NotBlank(message = "Username cannot be empty!")
    @Size(max = 100, message = "Username too long!")
    @Column(nullable = false, unique = true)
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email too long!")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();

    public void addProject(Project project){
        projects.add(project);
        project.setUser(this);
    }

    public void removeProject(Project project){
        projects.remove(project);
        project.setUser(null);
    }
}
