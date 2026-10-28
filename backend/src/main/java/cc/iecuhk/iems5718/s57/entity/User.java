package cc.iecuhk.iems5718.s57.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // SQLite 中 user 可能是保留字，用 users 更安全
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userid;

    // 邮箱作为唯一登录凭证 (Unique Key)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 存储加盐哈希后的密码，绝不能存明文！
    @Column(nullable = false)
    private String password;

    // 区分管理员和普通用户 (例如："ROLE_ADMIN" 或 "ROLE_USER")
    @Column(nullable = false)
    private String role;

    // ================= Getters & Setters =================
    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}