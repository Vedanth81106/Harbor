package com.waffles.harbor.user;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
// Because User references Project and Project references User, calling toString() or hashCode() (which Hibernate often does under the hood)
// will trigger an infinite loop and crash the application with a StackOverflowError. This is the reason why @Data annotation of Lombok is not used here.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

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

    @Enumerated(EnumType.STRING)
    private Role role;

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

    // Spring security:
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Formats your Role enum into a standard Spring Security authority list
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // Tells Spring where the encoded password sits
    }

    @Override
    public String getUsername() {
        return this.username; // Confirms the unique login identifier string for JWT payload packaging
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // True means account hasn't reached an end date
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // True means credentials/passwords haven't aged out
    }

    @Override
    public boolean isEnabled() {
        return true; // True means active status
    }
}
