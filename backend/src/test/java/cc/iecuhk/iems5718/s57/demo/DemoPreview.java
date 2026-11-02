package cc.iecuhk.iems5718.s57.demo;
import cc.iecuhk.iems5718.s57.config.SecurityConfig;
import cc.iecuhk.iems5718.s57.exception.GlobalExceptionHandler;
import cc.iecuhk.iems5718.s57.config.SessionValidationFilter;
import cc.iecuhk.iems5718.s57.repository.UserSessionRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.mockito.Mockito.mock;
/** Local-only harness: real security chain + demo controller, no normal DB or account writes. */
@TestConfiguration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
@Import({DemoController.class,SecurityConfig.class,SessionValidationFilter.class,GlobalExceptionHandler.class})
public class DemoPreview {
 @Bean UserSessionRepository sessions(){return mock(UserSessionRepository.class);}
 @Bean UserDetailsService users(){return name->{throw new UsernameNotFoundException("No normal users in preview");};}
 public static void main(String[]args){SpringApplication.run(DemoPreview.class,args);}
}
