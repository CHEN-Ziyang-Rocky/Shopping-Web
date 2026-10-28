package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    // 供登录时通过邮箱查找用户使用
    Optional<User> findByEmail(String email);
}