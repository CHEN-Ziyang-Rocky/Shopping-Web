package cc.iecuhk.iems5718.s57.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Voucher code (e.g. EASTER12)
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    // Discount amount (e.g. 5.0)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    // Remaining available spots
    @Column(nullable = false)
    private Integer quota;

    // Hibernate increments the version on each update and rejects stale writes.
    @Version
    private Integer version;

    // ================= Getters & Setters =================
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
