package cc.iecuhk.iems5718.s57.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // 对应数据库的 orders 表
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 关联购买用户的 ID
    private Integer userid;

    // 交易基本信息
    @Column(precision = 12, scale = 2)
    private BigDecimal totalPrice;
    private Long amountTotalMinor;
    private String currency;
    private String merchantEmail;

    // 安全校验字段 (作业核心要求)
    private String salt;
    private String digest;

    // 支付状态：PENDING (待支付), PAID (已支付), FAILED (失败)
    private String status;

    // Stripe 的 Session ID (用于后续 Webhook 验证和对账)
    @Column(unique = true)
    private String transactionId;

    // 订单创建时间 (使用时间戳)
    private Long createdAt;

    // 记录使用的优惠券码（如果没用就是 null）
    private String voucherCode;

    // 记录折扣了多少钱
    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount;

    // 级联保存订单项 (OrderItems)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // 在 order_items 表中生成 order_id 外键
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
    }

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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getAmountTotalMinor() {
        return amountTotalMinor;
    }

    public void setAmountTotalMinor(Long amountTotalMinor) {
        this.amountTotalMinor = amountTotalMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchantEmail() {
        return merchantEmail;
    }

    public void setMerchantEmail(String merchantEmail) {
        this.merchantEmail = merchantEmail;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
