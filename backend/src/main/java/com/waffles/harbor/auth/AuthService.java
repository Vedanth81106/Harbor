package com.waffles.harbor.auth;

import com.waffles.harbor.auth.dto.AuthResponse;
import com.waffles.harbor.auth.dto.LoginRequest;
import com.waffles.harbor.auth.dto.RegisterRequest;
import com.waffles.harbor.security.JwtService;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest registerRequest){

        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()) throw new IllegalArgumentException("Username is already taken!");
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent()) throw new IllegalArgumentException("Email is already used!");

        var user = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                        .accessToken(jwtToken)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build();
    }

    public AuthResponse login(LoginRequest loginRequest){

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(),loginRequest.getPassword())
        );

        String identifier = loginRequest.getUsernameOrEmail();
        var user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("Username or password is invalid!"));

        var jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
