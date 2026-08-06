package com.waffles.harbor.user;

import com.waffles.harbor.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Container
    @ServiceConnection // uses the give container as a datasource instead of writing config in application.props
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    private User createUser() {
        return User.builder()
                .username("testUser")
                .email("testEmail@gmail.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldFindByUsername(){

        // Arrange the data to be tested on:
        User user = createUser();

        userRepository.save(user);

        // Act - the thing i am actually testing for:
        Optional<User> result = userRepository.findByUsername("testUser");

        // Assert - check the expected behavior
        assertTrue(result.isPresent());

        User found = result.get();

        assertEquals("testUser", found.getUsername());
        assertNotNull(found.getUserId());
        assertEquals(Role.USER, found.getRole());
    }

    @Test
    void shouldFindUserByEmail(){

        User user = createUser();
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail("testEmail@gmail.com");
        assertTrue(result.isPresent());

        User found = result.get();
        assertEquals("testEmail@gmail.com", found.getEmail());
        assertNotNull(found.getUserId());
        assertEquals(Role.USER, found.getRole());
    }

    @Test
    void shouldFindUserByUsernameOrEmail(){
        User user = createUser();

        userRepository.save(user);

        Optional<User> result = userRepository.findByUsernameOrEmail("testUser","testEmail@gmail.com");
        assertTrue(result.isPresent());

        assertTrue(userRepository.findByUsernameOrEmail("testUser","ghost@gmail.com").isPresent());
        assertTrue(userRepository.findByUsernameOrEmail("testUser","testEmail@gmail.com").isPresent());

        User found = result.get();
        assertNotNull(found.getUserId());
        assertEquals(Role.USER, found.getRole());
        assertEquals("testUser", found.getUsername());
        assertEquals("testEmail@gmail.com", found.getEmail());
    }

    @Test
    void shouldReturnEmptyWhenUsernameDoesNotExist(){

        String username = "ghost";
        Optional<User> result = userRepository.findByUsername(username);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist(){

        String email = "ghost@email.com";
        Optional<User> result = userRepository.findByEmail(email);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenEmailOrUsernameDoesNotExist(){

        String email = "ghost@gmail.com";
        String username = "ghost";

        Optional<User> result = userRepository.findByUsernameOrEmail(username,email);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotAllowDuplicateUsername() {

        User first = createUser();

        User second = User.builder()
                .username("testUser")
                .email("another@gmail.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();

        userRepository.save(first);

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(second));
    }

    @Test
    void shouldNotAllowDuplicateEmail() {

        User first = createUser();

        User second = User.builder()
                .username("differentUser")
                .email("testEmail@gmail.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();

        userRepository.save(first);

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(second));
    }
}
