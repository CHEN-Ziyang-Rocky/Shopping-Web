package cc.iecuhk.iems5718.s57.config;

import cc.iecuhk.iems5718.s57.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SessionValidationFilter extends OncePerRequestFilter {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer demo_") && !request.getRequestURI().startsWith("/api/demo/")) {
            response.setStatus(403); response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demo credentials cannot access account or payment APIs\"}"); return;
        }
        if (request.getRequestURI().startsWith("/api/demo/")) {
            response.setHeader("Cache-Control", "no-store"); filterChain.doFilter(request,response); return;
        }
        HttpSession session = request.getSession(false);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (session != null && authentication != null && authentication.isAuthenticated()) {
            boolean valid = userSessionRepository.findBySessionId(session.getId())
                    .map(record -> record.isValid()).orElse(false);
            if (!valid) {
                session.invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
