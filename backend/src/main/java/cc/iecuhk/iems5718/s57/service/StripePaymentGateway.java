package cc.iecuhk.iems5718.s57.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class StripePaymentGateway implements PaymentGateway {
    private final String apiKey;
    private final String frontendUrl;

    public StripePaymentGateway(@Value("${stripe.api.key:}") String apiKey,
                                @Value("${frontend.url}") String frontendUrl) {
        this.apiKey = apiKey;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public Checkout create(Integer orderId, String currency, List<Line> lines, long discount) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment provider is not configured");
        }
        // SDK retries for one operation reuse this key; it is not cross-request checkout deduplication.
        String attempt = UUID.randomUUID().toString();
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey)
                // v27 changed the default from 0 to 2. Preserve the existing transaction latency.
                .setMaxNetworkRetries(0).setIdempotencyKey("checkout-" + attempt).build();
        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setUiMode(SessionCreateParams.UiMode.HOSTED_PAGE)
                    .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/")
                    .setExpiresAt(Instant.now().getEpochSecond() + 1800)
                    .putMetadata("order_id", orderId.toString());
            for (Line line : lines) {
                params.addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity((long) line.quantity())
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency.toLowerCase(java.util.Locale.ROOT))
                                .setUnitAmount(line.unitAmount())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(line.name()).build()).build()).build());
            }
            if (discount > 0) {
                Coupon coupon = Coupon.create(CouponCreateParams.builder()
                        .setAmountOff(discount).setCurrency(currency.toLowerCase(java.util.Locale.ROOT))
                        .setDuration(CouponCreateParams.Duration.ONCE).build(),
                        RequestOptions.builder().setApiKey(apiKey).setMaxNetworkRetries(0)
                                .setIdempotencyKey("coupon-" + attempt).build());
                params.addDiscount(SessionCreateParams.Discount.builder().setCoupon(coupon.getId()).build());
            }
            Session session = Session.create(params.build(), options);
            return new Checkout(session.getId(), session.getUrl());
        } catch (StripeException e) {
            // Propagate a runtime exception so the surrounding database transaction rolls back.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create payment session. Please try again.", e);
        }
    }
}
