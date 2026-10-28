package cc.iecuhk.iems5718.s57.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer pid;
    private Integer quantity;
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    // 🚀 新增：这是一个临时字段，不存入数据库，仅用于向前端返回商品名称
    @Transient
    private String name;

    public OrderItem() {
    }

    public OrderItem(Integer pid, Integer quantity, BigDecimal price) {
        this.pid = pid;
        this.quantity = quantity;
        this.price = price;
    }

    // ================= Getters & Setters =================
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}