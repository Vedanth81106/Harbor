package com.waffles.harbor.auth;

import com.waffles.harbor.auth.dto.AuthResponse;
import com.waffles.harbor.auth.dto.LoginRequest;
import com.waffles.harbor.auth.dto.RegisterRequest;
import com.waffles.harbor.security.JwtService;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService; // this the actual thing we are testing

    private User createUser(String username){
        return User.builder()
                .username(username)
                .email(username+"@gmail.com")
                .role(Role.USER)
                .passwordHash("hash")
                .build();
    }

    private RegisterRequest createRegisterRequest(String username){
        return RegisterRequest.builder()
                .username(username)
                .email(username + "@gmail.com")
                .password("pwd")
                .build();
    }

    private LoginRequest createLoginRequest(String identifier){
        return LoginRequest.builder()
                .usernameOrEmail(identifier)
                .password("pwd")
                .build();
    }

    @Test
    void shouldRejectExistingUsername(){

        User user = createUser("test");
        RegisterRequest registerRequest = createRegisterRequest("test");
        
        // when you go look for user with username "test" pretend that the user that was created before was found
        when(userRepository.findByUsername("test"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        // I expect userRepository to have actually called the method
        verify(userRepository).findByUsername("test");

        // And these were never called
        verify(userRepository,never()).save(any());
        verify(passwordEncoder,never()).encode(any());
        verify(jwtService,never()).generateToken(any());
    }

    @Test
    void shouldRejectExistingEmail(){

        User user = createUser("test");
        RegisterRequest registerRequest = createRegisterRequest("test");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        verify(userRepository,never()).save(any());
        verify(passwordEncoder,never()).encode(any());
        verify(jwtService,never()).generateToken(any());
        verify(userRepository).findByUsername("test");
        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    void shouldRegisterUserSuccessfully(){

        RegisterRequest registerRequest = createRegisterRequest("happy");

        // no user with given username or email exists(or else exception thrown)
        when(userRepository.findByUsername("happy"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("happy@gmail.com"))
                .thenReturn(Optional.empty());
        // pretend password encode gave this password after encoding
        when(passwordEncoder.encode("pwd"))
                .thenReturn("hashed");
        when(jwtService.generateToken(any(User.class)))
                .thenReturn("very-secure-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertEquals("hashed",savedUser.getPasswordHash());
        assertEquals("happy",savedUser.getUsername());
        assertEquals("happy@gmail.com",savedUser.getEmail());
        assertEquals(Role.USER,savedUser.getRole());

        verify(passwordEncoder).encode("pwd");
        verify(jwtService).generateToken(savedUser);

        assertNotNull(response);
        assertEquals("very-secure-jwt-token", response.getAccessToken());
        assertEquals("happy", response.getUsername());
        assertEquals("happy@gmail.com", response.getEmail());
    }

    @Test
    void shouldLoginSuccessfullyWithUsername(){

        LoginRequest loginRequest = createLoginRequest("happy");
        User user = createUser("happy");

        when(userRepository.findByUsernameOrEmail("happy", "happy"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(User.class)))
                .thenReturn("very-secure-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

        assertNotNull(response);
        verify(jwtService).generateToken(user);
        verify(userRepository).findByUsernameOrEmail("happy", "happy");
        assertEquals("very-secure-jwt-token", response.getAccessToken());
        assertEquals("happy", response.getUsername());
        assertEquals("happy@gmail.com", response.getEmail());
    }


    @Test
    void shouldLoginSuccessfullyWithEmail(){

        User user = createUser("happy-email");
        LoginRequest loginRequest = createLoginRequest("happy-email@gmail.com");

        when(userRepository.findByUsernameOrEmail("happy-email@gmail.com","happy-email@gmail.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(User.class)))
                .thenReturn("very-secure-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

        assertNotNull(response);
        verify(jwtService).generateToken(user);
        verify(userRepository).findByUsernameOrEmail("happy-email@gmail.com", "happy-email@gmail.com");
        assertEquals("very-secure-jwt-token", response.getAccessToken());
        assertEquals("happy-email", response.getUsername());
        assertEquals("happy-email@gmail.com", response.getEmail());
    }

    @Test
    void shouldRejectInvalidCredentials(){

        LoginRequest loginRequest = createLoginRequest("happy");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(userRepository, never()).findByUsernameOrEmail(any(), any());
        verify(jwtService, never()).generateToken(any());
    }
}
