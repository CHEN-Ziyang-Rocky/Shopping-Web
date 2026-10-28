package cc.iecuhk.iems5718.s57.component;

import cc.iecuhk.iems5718.s57.entity.User;
import cc.iecuhk.iems5718.s57.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "shop.bootstrap.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public DataInitializer(UserRepository users, PasswordEncoder encoder,
            @Value("${SHOP_ADMIN_EMAIL:}") String email, @Value("${SHOP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set SHOP_ADMIN_EMAIL and a 12-72 byte SHOP_ADMIN_PASSWORD before bootstrapping");
        }
        if (users.findByEmail(email).isEmpty()) {
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(encoder.encode(password));
            admin.setRole("ROLE_ADMIN");
            users.save(admin);
        }
    }
}
