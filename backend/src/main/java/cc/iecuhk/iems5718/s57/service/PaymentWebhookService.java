package cc.iecuhk.iems5718.s57.service;

import cc.iecuhk.iems5718.s57.repository.OrderRepository;
import cc.iecuhk.iems5718.s57.repository.VoucherRepository;
import cc.iecuhk.iems5718.s57.repository.ProductRepository;
import cc.iecuhk.iems5718.s57.repository.PaymentEventRepository;
import cc.iecuhk.iems5718.s57.entity.PaymentEvent;
import com.stripe.model.checkout.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Objects;

@Service
public class PaymentWebhookService {
    private final OrderRepository orders;
    private final VoucherRepository vouchers;

    private final ProductRepository products;
    private final PaymentEventRepository events;
    public PaymentWebhookService(OrderRepository orders, VoucherRepository vouchers,
            ProductRepository products,
            PaymentEventRepository events) {
        this.products = products;
        this.events = events;
        this.orders = orders;
        this.vouchers = vouchers;
    }

    @Transactional
    public void process(String eventId, String eventType, Session session) {
        String orderId = session.getMetadata() == null ? null : session.getMetadata().get("order_id");
        if (orderId == null) return; // Other applications may share this Stripe account.
        int id;
        try {
            id = Integer.parseInt(orderId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order reference");
        }
        var order = orders.lockById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order is not available yet"));
        if (events.existsById(eventId)) return;
        events.save(new PaymentEvent(eventId, id, eventType));
        if (order.getTransactionId() == null || !order.getTransactionId().equals(session.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment session mismatch");
        }
        long expected = order.getAmountTotalMinor() != null
                ? order.getAmountTotalMinor() : Money.cents(order.getTotalPrice());
        if (!Objects.equals(session.getAmountTotal(), expected)
                || session.getCurrency() == null || !order.getCurrency().equalsIgnoreCase(session.getCurrency())
                || !"payment".equals(session.getMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount or currency mismatch");
        }
        if (!OrderIntegrity.digest(order).equals(order.getDigest())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order snapshot integrity check failed");
        }
        if ("checkout.session.expired".equals(eventType)
                || "checkout.session.async_payment_failed".equals(eventType)) {
            String terminal = eventType.endsWith("expired") ? "EXPIRED" : "FAILED";
            if (orders.transitionPending(id, terminal) == 1) {
                for (var item : order.getItems()) {
                    if (products.release(item.getPid(), item.getQuantity()) != 1) throw new IllegalStateException("Reserved product missing");
                }
                if (order.getVoucherCode() != null && vouchers.releaseQuota(order.getVoucherCode()) != 1) {
                    throw new IllegalStateException("Reserved voucher is missing");
                }
            }
            return;
        }
        boolean settled = "paid".equals(session.getPaymentStatus())
                || (expected == 0 && "no_payment_required".equals(session.getPaymentStatus()));
        if (!settled) return; // completed may precede a delayed payment's eventual result.
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order requires payment reconciliation");
        }
        // Database compare-and-set makes concurrent/repeated deliveries safe.
        orders.transitionPending(id, "PAID");
    }
}
