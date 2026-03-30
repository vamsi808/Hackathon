package com.hackathon.backend.controller;

import com.hackathon.backend.model.Role;
import com.hackathon.backend.model.User;
import com.hackathon.backend.payload.request.*;
import com.hackathon.backend.payload.response.JwtResponse;
import com.hackathon.backend.payload.response.MessageResponse;
import com.hackathon.backend.repository.UserRepository;
import com.hackathon.backend.security.JwtUtils;
import com.hackathon.backend.security.UserDetailsImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import com.hackathon.backend.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    LoginAttemptService loginAttemptService;

    @Autowired
    HttpServletRequest request;

    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many login attempts. Please try again later."));
        }

        String usernameOrEmail = loginRequest.getIdentifier();

        // Check if user exists by email or username
        boolean exists = userRepository.existsByEmail(usernameOrEmail) || userRepository.existsByUsername(usernameOrEmail);
        if (!exists) {
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Email not registered"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();    

            loginAttemptService.loginSucceeded(ip);

            return ResponseEntity.ok(new JwtResponse(jwt, 
                                                     userDetails.getId(), 
                                                     userDetails.getUsername(), 
                                                     userDetails.getEmail(), 
                                                     userDetails.getAuthorities().stream().findFirst().get().getAuthority()));
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Incorrect password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (signUpRequest.getDob() != null) {
            java.time.Period period = java.time.Period.between(signUpRequest.getDob(), java.time.LocalDate.now());
            if (period.getYears() <= 10) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: User must be strictly older than 10 years!"));
            }
        }

        // Create new user's account
        User user = new User();
        user.setFullName(signUpRequest.getFullName());
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhone(signUpRequest.getPhone());
        user.setDob(signUpRequest.getDob());
        user.setGender(signUpRequest.getGender());
        user.setAddress(signUpRequest.getAddress());
        user.setRole(Role.USER); // Default Role
        user.setVerified(true); // Auto-verify

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

}
