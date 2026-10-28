package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.entity.User;
import cc.iecuhk.iems5718.s57.entity.UserSession;
import cc.iecuhk.iems5718.s57.repository.UserRepository;
import cc.iecuhk.iems5718.s57.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🚀 现代密码标准正则：至少8位，包含大写字母、数字、特殊字符
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(csrfToken);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("email", authentication.getName());
        response.put("role", authentication.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Passwords do not match.");
        }
        if (email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || email.length() > 254 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body("Invalid email or password length.");
        }

        // 🚀 后端强制校验密码强度
        if (!pattern.matcher(password).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Password must be at least 8 characters long, include uppercase, number, and special character.");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Email is already registered.");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_USER");
        userRepository.save(newUser);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request, HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            // Controller-based login must explicitly rotate the session and persist a fresh context.
            HttpSession previous = request.getSession(false);
            if (previous != null) {
                userSessionRepository.findBySessionId(previous.getId()).ifPresent(s -> {
                    s.setValid(false);
                    userSessionRepository.save(s);
                });
                previous.invalidate();
            }

            HttpSession session = request.getSession(true);

            User user = userRepository.findByEmail(email).orElseThrow();
            String ip = request.getRemoteAddr();

            UserSession userSession = new UserSession();
            userSession.setUserid(user.getUserid());
            userSession.setSessionId(session.getId());
            userSession.setIpAddress(ip);
            userSession.setLoginTime(System.currentTimeMillis());
            userSession.setValid(true);
            userSessionRepository.save(userSession);

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, servletResponse);
            new HttpSessionCsrfTokenRepository().saveToken(null, request, servletResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("role", authentication.getAuthorities().iterator().next().getAuthority());
            response.put("email", authentication.getName());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password.");
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        List<UserSession> activeSessions = userSessionRepository
                .findByUseridAndIsValidTrueOrderByLoginTimeDesc(user.getUserid());

        String current = request.getSession(false).getId();
        return ResponseEntity.ok(activeSessions.stream().map(s -> sessionView(s, current)).toList());
    }

    @PostMapping("/sessions/kick")
    public ResponseEntity<?> kickSession(@RequestParam Integer dbSessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Optional<UserSession> sessionOpt = userSessionRepository.findById(dbSessionId);

        if (sessionOpt.isPresent()) {
            UserSession targetSession = sessionOpt.get();
            if (targetSession.getUserid().equals(user.getUserid())) {
                targetSession.setValid(false);
                userSessionRepository.save(targetSession);
                return ResponseEntity.ok("Session successfully terminated.");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid session operation.");
    }

    @GetMapping("/admin/sessions")
    public ResponseEntity<?> getAllActiveSessions(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserSession> allSessions = userSessionRepository.findAll().stream()
                .filter(UserSession::isValid)
                .toList();

        String current = request.getSession(false).getId();
        return ResponseEntity.ok(allSessions.stream().map(s -> sessionView(s, current)).toList());
    }

    private record SessionView(Integer id, Integer userid, String ipAddress, Long loginTime,
                               boolean valid, boolean current) {}

    private SessionView sessionView(UserSession session, String currentId) {
        return new SessionView(session.getId(), session.getUserid(), session.getIpAddress(),
                session.getLoginTime(), session.isValid(), session.getSessionId().equals(currentId));
    }

    @PostMapping("/admin/sessions/kick")
    public ResponseEntity<?> adminKickSession(@RequestParam Integer dbSessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<UserSession> sessionOpt = userSessionRepository.findById(dbSessionId);
        if (sessionOpt.isPresent()) {
            UserSession targetSession = sessionOpt.get();
            targetSession.setValid(false);
            userSessionRepository.save(targetSession);
            return ResponseEntity.ok("Session successfully terminated by Admin.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session not found.");
    }

    // 🚀 加入 HttpServletRequest 用于登出 Session
    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 🚀 后端强制校验新密码强度
        if (!pattern.matcher(newPassword).matches() || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: New password must meet complexity requirements.");
        }

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect old password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke every device, including sessions that are not currently making requests.
        var activeSessions = userSessionRepository.findByUseridAndIsValidTrueOrderByLoginTimeDesc(user.getUserid());
        activeSessions.forEach(s -> s.setValid(false));
        userSessionRepository.saveAll(activeSessions);

        HttpSession session = request.getSession(false);
        if (session != null) {
            userSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
                s.setValid(false);
                userSessionRepository.save(s);
            });
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("Password changed successfully. Please log in again.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            userSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
                s.setValid(false);
                userSessionRepository.save(s);
            });
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }
}
