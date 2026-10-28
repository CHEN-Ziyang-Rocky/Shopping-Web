package cc.iecuhk.iems5718.s57.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userid;

    // 存储 Spring 的 JSESSIONID
    @Column(nullable = false, unique = true)
    @JsonIgnore
    private String sessionId;

    // 登录的 IP 地址
    @Column(nullable = false)
    private String ipAddress;

    // 登录时间戳
    @Column(nullable = false)
    private Long loginTime;

    // 是否有效 (如果是 false，说明被远程踢下线了)
    @Column(nullable = false)
    private boolean isValid;

    // ================= Getters & Setters =================
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }
}
