package cc.iecuhk.iems5718.s57.entity;
import jakarta.persistence.*;
@Entity @Table(name="payment_events")
public class PaymentEvent {
    @Id @Column(length=255) public String id;
    @Column(nullable=false) public Integer orderId;
    @Column(nullable=false) public String eventType;
    @Column(nullable=false) public Long processedAt;
    public PaymentEvent() {}
    public PaymentEvent(String id, Integer orderId, String type) {
        this.id=id; this.orderId=orderId; this.eventType=type; this.processedAt=System.currentTimeMillis();
    }
}
