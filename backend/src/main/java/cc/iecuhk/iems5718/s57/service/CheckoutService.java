package cc.iecuhk.iems5718.s57.service;

import cc.iecuhk.iems5718.s57.controller.CheckoutController.CheckoutRequest;
import cc.iecuhk.iems5718.s57.entity.Order;
import cc.iecuhk.iems5718.s57.entity.OrderItem;
import cc.iecuhk.iems5718.s57.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class CheckoutService {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final VoucherRepository vouchers;
    private final PaymentGateway gateway;
    private final String currency;
    private final String merchant;

    public CheckoutService(ProductRepository products, OrderRepository orders, UserRepository users,
                           VoucherRepository vouchers, PaymentGateway gateway,
                           @Value("${shop.currency}") String currency,
                           @Value("${shop.merchant.email}") String merchant) {
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.vouchers = vouchers;
        this.gateway = gateway;
        this.currency = currency;
        this.merchant = merchant;
    }

    @Transactional
    public PaymentGateway.Checkout create(CheckoutRequest request, String email) {
        // The shop currently uses HKD's two-decimal minor unit.
        if (!"HKD".equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unsupported shop currency");
        }
        Order order = new Order();
        order.setUserid(users.findByEmail(email).orElseThrow().getUserid());
        order.setCurrency(currency);
        order.setMerchantEmail(merchant);
        order.setSalt(UUID.randomUUID().toString());
        List<PaymentGateway.Line> lines = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        long subtotal = 0;
        for (var item : request.cartItems.stream().sorted(java.util.Comparator.comparing(i -> i.pid)).toList()) {
            if (!ids.add(item.pid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate product in cart");
            }
            var product = products.findById(item.pid).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product no longer available"));
            if (products.reserve(item.pid, item.quantity) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
            }
            long unitAmount = Money.cents(product.getPrice());
            if (unitAmount <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product price");
            subtotal = Math.addExact(subtotal, Math.multiplyExact(unitAmount, item.quantity.longValue()));
            // Stripe's amount limit is eight digits in the minor unit.
            if (subtotal > 99_999_999) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart total is too large");
            }
            order.getItems().add(new OrderItem(item.pid, item.quantity, Money.amount(unitAmount)));
            lines.add(new PaymentGateway.Line(product.getName(), unitAmount, item.quantity));
        }

        long discount = 0;
        if (request.vcode != null && !request.vcode.isBlank()) {
            var voucher = vouchers.findByCode(request.vcode.trim()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid voucher code"));
            if (voucher.getQuota() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher quota exceeded");
            }
            discount = Math.min(subtotal, Money.cents(voucher.getDiscount()));
            order.setVoucherCode(voucher.getCode());
            order.setDiscountAmount(Money.amount(discount));
            voucher.setQuota(voucher.getQuota() - 1);
            // Detect stale versions before creating any external payment session.
            vouchers.saveAndFlush(voucher);
        }
        order.setTotalPrice(Money.amount(subtotal - discount));
        order.setAmountTotalMinor(subtotal - discount);
        order.setDigest(OrderIntegrity.digest(order));
        orders.saveAndFlush(order);
        var checkout = gateway.create(order.getId(), currency, lines, discount);
        order.setTransactionId(checkout.id());
        orders.saveAndFlush(order);
        return checkout;
    }
}
