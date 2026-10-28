package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.service.PaymentWebhookService;
import com.stripe.model.Event;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    private static final Set<String> EVENTS = Set.of("checkout.session.completed",
            "checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed",
            "checkout.session.expired");
    private final PaymentWebhookService payments;
    private final String secret;

    public WebhookController(PaymentWebhookService payments, @Value("${stripe.webhook.secret:}") String secret) {
        this.payments = payments;
        this.secret = secret;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        if (secret.isBlank()) return ResponseEntity.status(503).body("Webhook is not configured");
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, secret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid webhook");
        }
        if (event == null || !"event".equals(event.getObject())
                || event.getId() == null || event.getId().isBlank()
                || event.getType() == null || event.getType().isBlank() || event.getData() == null) {
            return ResponseEntity.badRequest().body("Invalid snapshot event");
        }
        // Newer SDKs accept the same release family. This endpoint deliberately pins the tested date.
        if (!Stripe.API_VERSION.equals(event.getApiVersion())) {
            return ResponseEntity.status(503).body("Unsupported webhook API version");
        }
        if (!EVENTS.contains(event.getType())) return ResponseEntity.ok("Ignored");
        Session session;
        try {
            var object = event.getDataObjectDeserializer().getObject();
            if (object.isEmpty() || !(object.get() instanceof Session)) {
                return ResponseEntity.badRequest().body("Invalid checkout snapshot");
            }
            session = (Session) object.get();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Invalid checkout snapshot");
        }
        if (session.getId() == null || session.getId().isBlank() || session.getAmountTotal() == null
                || session.getCurrency() == null || session.getCurrency().isBlank()
                || session.getMode() == null || session.getPaymentStatus() == null
                || !Set.of("paid", "unpaid", "no_payment_required").contains(session.getPaymentStatus())) {
            return ResponseEntity.badRequest().body("Incomplete checkout snapshot");
        }
        payments.process(event.getId(), event.getType(), session);
        return ResponseEntity.ok("Processed");
    }
}
