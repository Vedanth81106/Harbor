package com.waffles.harbor.auth;

import com.waffles.harbor.auth.dto.AuthResponse;
import com.waffles.harbor.auth.dto.LoginRequest;
import com.waffles.harbor.auth.dto.RegisterRequest;
import com.waffles.harbor.security.JwtAuthenticationFilter;
import com.waffles.harbor.security.JwtService;
import com.waffles.harbor.security.SecurityConfig;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    // ---------- LOGIN -----------------
    @Test
    void shouldLoginSuccessfully() throws Exception {

        AuthResponse response = AuthResponse.builder()
                .accessToken("fake-jwt")
                .email("happy@gmail.com")
                .username("happy")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "usernameOrEmail": "happy",
                                            "password": "pwd"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt"))
                .andExpect(jsonPath("$.username").value("happy"))
                .andExpect(jsonPath("$.email").value("happy@gmail.com"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldRejectLoginWithBadCredentials() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "usernameOrEmail": "happy",
                                "password": "wrong-password"
                            }
                            """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithMissingPassword() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "usernameOrEmail": "happy"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());
    }

    // ---------------- REGISTER --------------
    @Test
    void shouldRegisterSuccessfully() throws Exception{
        AuthResponse response = AuthResponse.builder()
                .accessToken("fake-jwt")
                .email("happy@gmail.com")
                .username("happy")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "happy",
                                            "password": "happypassword",
                                            "email":"happy@gmail.com"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt"))
                .andExpect(jsonPath("$.username").value("happy"))
                .andExpect(jsonPath("$.email").value("happy@gmail.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() throws Exception {

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username is already taken!"));

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "username": "happy",
                                "password": "password123",
                                "email": "happy@gmail.com"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRegistrationWithInvalidUsername() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "hi",
                                            "password": "password123",
                                            "email":"happy@gmail.com"
                                        }
                            """)
                )
                .andExpect(status().isBadRequest());
    }
}