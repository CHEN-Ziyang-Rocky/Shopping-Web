package cc.iecuhk.iems5718.s57.service;

import java.util.List;

public interface PaymentGateway {
    record Line(String name, long unitAmount, int quantity) {}
    record Checkout(String id, String url) {}
    Checkout create(Integer orderId, String currency, List<Line> lines, long discount);
}
