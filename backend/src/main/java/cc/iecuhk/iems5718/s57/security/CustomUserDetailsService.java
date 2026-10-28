package cc.iecuhk.iems5718.s57.security;

import cc.iecuhk.iems5718.s57.entity.User;
import cc.iecuhk.iems5718.s57.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 根据邮箱从数据库查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. 将我们的 User 实体转换成 Spring Security 认识的 UserDetails 对象
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(), // 这里的密码是加盐哈希值，Spring 会自动比对
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
    }
}