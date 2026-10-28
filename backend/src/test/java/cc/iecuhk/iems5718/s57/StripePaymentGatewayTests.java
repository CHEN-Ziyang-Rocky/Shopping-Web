package cc.iecuhk.iems5718.s57;

import cc.iecuhk.iems5718.s57.service.PaymentGateway;
import cc.iecuhk.iems5718.s57.service.StripePaymentGateway;
import com.stripe.Stripe;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;

/** Exercise the real SDK HTTP encoding/response parser against loopback, never api.stripe.com. */
class StripePaymentGatewayTests {
    private HttpServer server;
    private String originalApiBase;
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private String failingPath = "";
    private int failureStatus = 500;
    private StripePaymentGateway gateway;

    record RecordedRequest(String path, String method, String apiVersion, String idempotencyKey,
                           Map<String, String> form) {}

    @BeforeEach void startStub() throws Exception {
        originalApiBase = Stripe.getApiBase();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            Map<String, String> form = new LinkedHashMap<>();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            for (String part : body.split("&")) {
                String[] pair = part.split("=", 2);
                form.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
            requests.add(new RecordedRequest(path, exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Stripe-Version"),
                    exchange.getRequestHeaders().getFirst("Idempotency-Key"), form));
            int status = path.equals(failingPath) ? failureStatus : 200;
            String response = status != 200 ? "{\"error\":{\"type\":\"api_error\",\"message\":\"Synthetic provider failure\"}}"
                    : path.equals("/v1/coupons") ? "{\"id\":\"coupon_local\",\"object\":\"coupon\",\"duration\":\"once\",\"amount_off\":500,\"currency\":\"hkd\"}"
                    : "{\"id\":\"cs_test_local\",\"object\":\"checkout.session\",\"url\":\"https://checkout.stripe.com/local-test\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        Stripe.overrideApiBase("http://127.0.0.1:" + server.getAddress().getPort());
        gateway = new StripePaymentGateway("sk_test_local_stub_not_a_real_key", "https://shop.imrocky.com");
    }

    @AfterEach void stopStub() {
        Stripe.overrideApiBase(originalApiBase);
        if (server != null) server.stop(0);
    }

    @Test void sdkPinsExpectedVersion() {
        assertThat(Stripe.VERSION).isEqualTo("32.0.0");
        assertThat(Stripe.API_VERSION).isEqualTo("2026-03-25.dahlia");
    }

    @Test void hostedCheckoutPreservesMinorUnitsUrlsAndOrderMetadata() {
        long before = Instant.now().getEpochSecond();
        var result = gateway.create(42, "HKD", List.of(new PaymentGateway.Line("Desk & Lamp", 1999, 3)), 0);
        assertThat(result.id()).isEqualTo("cs_test_local");
        assertThat(result.url()).isEqualTo("https://checkout.stripe.com/local-test");
        assertThat(requests).hasSize(1);
        var request = requests.get(0);
        assertThat(request.path()).isEqualTo("/v1/checkout/sessions");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.apiVersion()).isEqualTo("2026-03-25.dahlia");
        assertThat(request.idempotencyKey()).startsWith("checkout-");
        assertThat(request.form()).containsEntry("mode", "payment").containsEntry("ui_mode", "hosted_page")
                .containsEntry("success_url", "https://shop.imrocky.com/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                .containsEntry("cancel_url", "https://shop.imrocky.com/").containsEntry("metadata[order_id]", "42")
                .containsEntry("line_items[0][quantity]", "3")
                .containsEntry("line_items[0][price_data][currency]", "hkd")
                .containsEntry("line_items[0][price_data][unit_amount]", "1999")
                .containsEntry("line_items[0][price_data][product_data][name]", "Desk & Lamp")
                .doesNotContainKey("discounts[0][coupon]");
        assertThat(Long.parseLong(request.form().get("expires_at")))
                .isBetween(before + 1800, Instant.now().getEpochSecond() + 1800);
    }

    @Test void voucherCreatesOnceCouponInMinorUnitsAndLinksItToCheckout() {
        gateway.create(42, "HKD", List.of(new PaymentGateway.Line("Lamp", 1999, 3)), 500);
        assertThat(requests).hasSize(2);
        var coupon = requests.get(0);
        var session = requests.get(1);
        assertThat(coupon.path()).isEqualTo("/v1/coupons");
        assertThat(coupon.apiVersion()).isEqualTo(Stripe.API_VERSION);
        assertThat(coupon.form()).containsEntry("amount_off", "500").containsEntry("currency", "hkd")
                .containsEntry("duration", "once");
        assertThat(session.form()).containsEntry("discounts[0][coupon]", "coupon_local")
                .containsEntry("metadata[order_id]", "42");
        assertThat(coupon.idempotencyKey()).isEqualTo(session.idempotencyKey().replace("checkout-", "coupon-"));
    }

    @ParameterizedTest @ValueSource(ints = {400, 429, 500})
    void providerErrorsRemainRetriableApplicationErrorsWithoutNewSdkRetries(int status) {
        failingPath = "/v1/checkout/sessions";
        failureStatus = status;
        assertThatThrownBy(() -> gateway.create(42, "HKD", List.of(new PaymentGateway.Line("Lamp", 1999, 1)), 0))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        assertThat(requests).hasSize(1);
    }

    @Test void couponFailureDoesNotCreateSession() {
        failingPath = "/v1/coupons";
        assertThatThrownBy(() -> gateway.create(42, "HKD", List.of(new PaymentGateway.Line("Lamp", 1999, 1)), 500))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).path()).isEqualTo("/v1/coupons");
    }

    @Test void missingKeyDoesNotMakeAnHttpRequest() {
        var missingKey = new StripePaymentGateway("", "https://shop.imrocky.com");
        assertThatThrownBy(() -> missingKey.create(42, "HKD", List.of(), 0))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(requests).isEmpty();
    }
}
