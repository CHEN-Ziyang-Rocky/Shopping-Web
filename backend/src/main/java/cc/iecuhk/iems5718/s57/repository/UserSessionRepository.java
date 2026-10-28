package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {
    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> findByUseridAndIsValidTrueOrderByLoginTimeDesc(Integer userid);
}