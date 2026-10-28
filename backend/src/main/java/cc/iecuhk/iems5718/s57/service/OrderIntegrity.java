package cc.iecuhk.iems5718.s57.service;

import cc.iecuhk.iems5718.s57.entity.Order;
import cc.iecuhk.iems5718.s57.entity.OrderItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Detects accidental snapshot changes. A database-local digest is not a payment signature. */
public final class OrderIntegrity {
    private OrderIntegrity() {}

    public static String digest(Order order) {
        StringBuilder raw = new StringBuilder()
                .append(order.getCurrency()).append('|')
                .append(order.getMerchantEmail()).append('|')
                .append(order.getSalt()).append('|');
        for (OrderItem item : order.getItems()) {
            raw.append(item.getPid()).append('|').append(item.getQuantity()).append('|')
                    .append(Money.cents(item.getPrice())).append('|');
        }
        if (order.getVoucherCode() != null) {
            raw.append(order.getVoucherCode()).append('|').append(Money.cents(order.getDiscountAmount())).append('|');
        }
        raw.append(Money.cents(order.getTotalPrice()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
