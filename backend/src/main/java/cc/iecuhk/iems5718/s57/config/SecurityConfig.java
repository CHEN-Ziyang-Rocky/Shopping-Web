package cc.iecuhk.iems5718.s57.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${frontend.url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontendUrl));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Authorization", "Idempotency-Key"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    // 🚀 新增：注入我们写的 Session 拦截器
    @Autowired
    private SessionValidationFilter sessionValidationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public FilterRegistrationBean<SessionValidationFilter> sessionFilterRegistration(SessionValidationFilter filter) {
        FilterRegistrationBean<SessionValidationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Run once, inside the security chain after its context is loaded.
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        HttpSessionCsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
        tokenRepository.setHeaderName("X-XSRF-TOKEN");
        XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();
        delegate.setCsrfRequestAttributeName(null);

        http
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/webhook/stripe", "/api/demo/**")
                        .csrfTokenRepository(tokenRepository)
                        .csrfTokenRequestHandler(delegate))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/demo/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/products/**", "/api/vouchers", "/uploads/**").permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register",
                                "/api/webhook/stripe").permitAll()
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/checkout/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionFixation().newSession())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> response.sendError(401))
                        .accessDeniedHandler((request, response, ex) -> response.sendError(403)));

        // 🚀 新增：将拦截器放在上下文过滤器的后面
        http.addFilterAfter(sessionValidationFilter, SecurityContextHolderFilter.class);

        return http.build();
    }
}
