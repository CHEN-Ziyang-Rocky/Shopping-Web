package cc.iecuhk.iems5718.s57;

import cc.iecuhk.iems5718.s57.controller.CheckoutController;
import cc.iecuhk.iems5718.s57.entity.*;
import cc.iecuhk.iems5718.s57.repository.*;
import cc.iecuhk.iems5718.s57.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
         "spring.jpa.show-sql=false",
        "shop.bootstrap.enabled=false", "stripe.webhook.secret=whsec_test_only",
        "app.upload.dir=target/test-uploads/",
        "frontend.url=http://localhost:3000", "DEMO_ENABLED=true"
})
@AutoConfigureMockMvc
class ShopIntegrationTests extends MySqlTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PaymentEventRepository events;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired UserSessionRepository sessions;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired VoucherRepository vouchers;
    @Autowired PasswordEncoder encoder;
    @Autowired CheckoutService checkout;
    @Autowired PaymentWebhookService webhooks;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean PaymentGateway gateway;

    private User alice;
    private Product product;

    @BeforeEach
    void setup() {
        events.deleteAll();
        orders.deleteAll();
        sessions.deleteAll();
        vouchers.deleteAll();
        products.deleteAll();
        users.deleteAll();
        alice = user("alice@example.com");
        product = new Product();
        categories.deleteAll();
        Category category = new Category(); category.setName("Test");
        product.setCatid(categories.saveAndFlush(category).getCatid());
        product.setStock(100);
        product.setName("Test product");
        product.setPrice(new java.math.BigDecimal("19.99"));
        product = products.saveAndFlush(product);
        when(gateway.create(anyInt(), anyString(), anyList(), anyLong())).thenAnswer(invocation ->
                new PaymentGateway.Checkout("cs_test_" + invocation.getArgument(0), "https://checkout.stripe.com/test"));
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode("TestPass1!"));
        user.setRole("ROLE_USER");
        return users.saveAndFlush(user);
    }

    private String csrf(MockHttpSession session) throws Exception {
        var response = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("token").asText();
    }

    private MockHttpSession login(String email) throws Exception {
        MockHttpSession anonymous = new MockHttpSession();
        MvcResult response = mvc.perform(post("/api/auth/login").session(anonymous)
                .header("X-XSRF-TOKEN", csrf(anonymous)).param("email", email).param("password", "TestPass1!"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) response.getRequest().getSession(false);
    }

    private Voucher voucher(int quota) {
        Voucher voucher = new Voucher();
        voucher.setCode("SAVE");
        voucher.setDiscount(new java.math.BigDecimal("5.0"));
        voucher.setQuota(quota);
        return vouchers.saveAndFlush(voucher);
    }

    private CheckoutController.CheckoutRequest request(String code) {
        var request = new CheckoutController.CheckoutRequest();
        var line = new CheckoutController.CartItemRequest();
        line.pid = product.getPid();
        line.quantity = 3;
        request.cartItems = List.of(line);
        request.vcode = code;
        return request;
    }

    private Order createOrder(String code) {
        var result = checkout.create(request(code), alice.getEmail());
        return orders.findByTransactionId(result.id());
    }

    private Session paidSession(Order order) {
        Session session = new Session();
        session.setId(order.getTransactionId());
        session.setMode("payment");
        session.setAmountTotal(order.getAmountTotalMinor());
        session.setCurrency("hkd");
        session.setPaymentStatus("paid");
        session.setMetadata(Map.of("order_id", order.getId().toString()));
        return session;
    }

    @Test
    void publicVoucherOffersExcludeExhaustedCodesAndInternalFields() throws Exception {
        voucher(0);
        Voucher available = new Voucher();
        available.setCode("PUBLIC20");
        available.setDiscount(new java.math.BigDecimal("20.00"));
        available.setQuota(3);
        vouchers.saveAndFlush(available);
        mvc.perform(get("/api/vouchers")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("PUBLIC20"))
                .andExpect(jsonPath("$[0].discount").value(20.00))
                .andExpect(jsonPath("$[0].quota").doesNotExist())
                .andExpect(jsonPath("$[0].version").doesNotExist())
                .andExpect(jsonPath("$[0].id").doesNotExist());
        mvc.perform(post("/api/checkout/create-session").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request("PUBLIC20")))).andExpect(status().isForbidden());
        verifyNoInteractions(gateway);
    }

    @Test
    void loginRotatesSessionAndDoesNotExposeOrOverwriteSessionIds() throws Exception {
        MockHttpSession anonymous = new MockHttpSession();
        String oldId = anonymous.getId();
        var result = mvc.perform(post("/api/auth/login").session(anonymous)
                .header("X-XSRF-TOKEN", csrf(anonymous)).param("email", alice.getEmail()).param("password", "TestPass1!"))
                .andExpect(status().isOk()).andReturn();
        var loggedIn = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(loggedIn.getId()).isNotEqualTo(oldId);
        assertThat(anonymous.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/sessions").session(loggedIn)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").doesNotExist()).andExpect(jsonPath("$[0].current").value(true));
        assertThat(sessions.findBySessionId(loggedIn.getId())).isPresent();
        mvc.perform(get("/api/auth/me").session(loggedIn)).andExpect(status().isOk());
    }

    @Test
    void csrfIsRequiredForCheckoutAndLogout() throws Exception {
        var session = login(alice.getEmail());
        mvc.perform(post("/api/checkout/create-session").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(null)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).header("X-XSRF-TOKEN", csrf(session)))
                .andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        assertThat(sessions.findAll()).allMatch(s -> !s.isValid());
        verifyNoInteractions(gateway);
    }

    @Test
    void csrfTokenFromBeforeLoginIsRejectedAfterLogin() throws Exception {
        var anonymous = new MockHttpSession();
        String token = csrf(anonymous);
        var result = mvc.perform(post("/api/auth/login").session(anonymous).header("X-XSRF-TOKEN", token)
                .param("email", alice.getEmail()).param("password", "TestPass1!"))
                .andExpect(status().isOk()).andReturn();
        var authenticated = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/api/auth/logout").session(authenticated).header("X-XSRF-TOKEN", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokedAndUntrackedSessionsCannotAccessAccount() throws Exception {
        var first = login(alice.getEmail());
        var second = login(alice.getEmail());
        int id = sessions.findBySessionId(first.getId()).orElseThrow().getId();
        mvc.perform(post("/api/auth/sessions/kick").session(second).header("X-XSRF-TOKEN", csrf(second))
                .param("dbSessionId", Integer.toString(id))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(first)).andExpect(status().isUnauthorized());
        sessions.deleteAll();
        mvc.perform(get("/api/auth/me").session(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void usersCannotRevokeOtherUsersSessionsOrAccessAdmin() throws Exception {
        user("bob@example.com");
        var aliceSession = login(alice.getEmail());
        var bobSession = login("bob@example.com");
        int bobId = sessions.findBySessionId(bobSession.getId()).orElseThrow().getId();
        mvc.perform(post("/api/auth/sessions/kick").session(aliceSession).header("X-XSRF-TOKEN", csrf(aliceSession))
                .param("dbSessionId", Integer.toString(bobId))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/orders").session(aliceSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/admin/sessions").session(aliceSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(bobSession)).andExpect(status().isOk());
    }

    @Test
    void changingPasswordRevokesAllDevices() throws Exception {
        var first = login(alice.getEmail());
        var second = login(alice.getEmail());
        mvc.perform(post("/api/auth/change-password").session(first).header("X-XSRF-TOKEN", csrf(first))
                .param("oldPassword", "TestPass1!").param("newPassword", "DifferentPass2!"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(second)).andExpect(status().isUnauthorized());
        assertThat(sessions.findAll()).allMatch(s -> !s.isValid());
        assertThat(encoder.matches("DifferentPass2!", users.findById(alice.getUserid()).orElseThrow().getPassword())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"cartItems\":[{\"pid\":1,\"quantity\":1.5}]}", "{}", "{\"cartItems\":[]}", "{\"cartItems\":[null]}",
            "{\"cartItems\":[{\"pid\":1}]}", "{\"cartItems\":[{\"pid\":1,\"quantity\":0}]}",
            "{\"cartItems\":[{\"pid\":1,\"quantity\":100}]}", "{\"cartItems\":[{\"pid\":-1,\"quantity\":1}]}"})
    void invalidCartReturns400WithoutCreatingPayment(String body) throws Exception {
        var session = login(alice.getEmail());
        mvc.perform(post("/api/checkout/create-session").session(session).header("X-XSRF-TOKEN", csrf(session))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
        verifyNoInteractions(gateway);
    }

    @Test
    void checkoutRepricesOnServerAndUsesExactCents() throws Exception {
        voucher(1);
        var session = login(alice.getEmail());
        String body = "{\"cartItems\":[{\"pid\":" + product.getPid() + ",\"quantity\":3,\"price\":0.01}],\"vcode\":\"SAVE\",\"totalPrice\":0.01}";
        mvc.perform(post("/api/checkout/create-session").session(session).header("X-XSRF-TOKEN", csrf(session))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        var order = orders.findAll().get(0);
        assertThat(order.getAmountTotalMinor()).isEqualTo(5497);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("54.97");
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isZero();
        verify(gateway).create(eq(order.getId()), eq("HKD"), eq(List.of(new PaymentGateway.Line("Test product", 1999, 3))), eq(500L));
    }

    @Test
    void failedGatewayRollsBackOrderAndVoucher() {
        voucher(1);
        when(gateway.create(anyInt(), anyString(), anyList(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider unavailable"));
        assertThatThrownBy(() -> createOrder("SAVE")).isInstanceOf(ResponseStatusException.class);
        assertThat(orders.count()).isZero();
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isEqualTo(1);
    }

    @Test
    void staleVoucherVersionIsRejectedByDatabase() {
        Voucher first = voucher(1);
        Voucher stale = vouchers.findById(first.getId()).orElseThrow();
        first.setQuota(0);
        vouchers.saveAndFlush(first);
        stale.setQuota(0);
        assertThatThrownBy(() -> vouchers.saveAndFlush(stale))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        assertThat(vouchers.findById(first.getId()).orElseThrow().getQuota()).isZero();
    }

    @Test
    void lastVoucherCannotBeClaimedByTwoConcurrentCheckouts() throws Exception {
        voucher(1);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> claim = () -> {
            start.await();
            try { checkout.create(request("SAVE"), alice.getEmail()); return true; }
            catch (RuntimeException conflict) { return false; }
        };
        try {
            var one = pool.submit(claim);
            var two = pool.submit(claim);
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isZero();
            assertThat(orders.count()).isEqualTo(1);
            verify(gateway, times(1)).create(anyInt(), anyString(), anyList(), anyLong());
        } finally { pool.shutdownNow(); }
    }

    @Test
    void completedButUnpaidWaitsForAsyncSuccessAndDuplicatesAreHarmless() {
        Order order = createOrder(null);
        Session event = paidSession(order);
        event.setPaymentStatus("unpaid");
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        event.setPaymentStatus("paid");
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.async_payment_succeeded", event);
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void expirationReleasesVoucherOnlyOnce() {
        voucher(1);
        Order order = createOrder("SAVE");
        Session event = paidSession(order);
        event.setPaymentStatus("unpaid");
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.expired", event);
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.expired", event);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("EXPIRED");
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isEqualTo(1);
    }

    @Test
    void paidOrderCannotBeExpiredAndQuotaIsNotReleased() {
        voucher(1);
        Order order = createOrder("SAVE");
        Session event = paidSession(order);
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event);
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.expired", event);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency", "session", "mode"})
    void paymentMismatchNeverMarksPaid(String mismatch) {
        Order order = createOrder(null);
        Session event = paidSession(order);
        switch (mismatch) {
            case "amount" -> event.setAmountTotal(1L);
            case "currency" -> event.setCurrency("usd");
            case "session" -> event.setId("cs_another");
            case "mode" -> event.setMode("subscription");
        }
        assertThatThrownBy(() -> webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void zeroAmountVoucherCheckoutAcceptsNoPaymentRequired() {
        var voucher = voucher(1);
        voucher.setDiscount(new java.math.BigDecimal("100.0"));
        vouchers.saveAndFlush(voucher);
        Order order = createOrder("SAVE");
        assertThat(order.getAmountTotalMinor()).isZero();
        Session event = paidSession(order);
        event.setPaymentStatus("no_payment_required");
        webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
    }

    @Test
    void paymentStatusIsVisibleOnlyToOwner() throws Exception {
        Order order = createOrder(null);
        user("bob@example.com");
        mvc.perform(get("/api/orders/payment-status").param("sessionId", order.getTransactionId())
                .session(login("bob@example.com"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/payment-status").param("sessionId", order.getTransactionId())
                .session(login(alice.getEmail()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void duplicateProductAndUnknownProductAreRejected() {
        var duplicate = request(null);
        duplicate.cartItems = List.of(duplicate.cartItems.get(0), duplicate.cartItems.get(0));
        assertThatThrownBy(() -> checkout.create(duplicate, alice.getEmail())).isInstanceOf(ResponseStatusException.class);
        var unknown = request(null);
        unknown.cartItems.get(0).pid = Integer.MAX_VALUE;
        assertThatThrownBy(() -> checkout.create(unknown, alice.getEmail())).isInstanceOf(ResponseStatusException.class);
        assertThat(orders.count()).isZero();
        verifyNoInteractions(gateway);
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsForeignOrigin() throws Exception {
        mvc.perform(options("/api/checkout/create-session")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/checkout/create-session")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreateCategoryWithCsrf() throws Exception {
        alice.setRole("ROLE_ADMIN");
        users.saveAndFlush(alice);
        var session = login(alice.getEmail());
        mvc.perform(post("/api/admin/categories").session(session).header("X-XSRF-TOKEN", csrf(session))
                .param("name", "Integration test category")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/admin/sessions").session(session)).andExpect(status().isOk());
    }

    @Test
    void invalidSignatureIsRejectedWithoutCsrfToken() throws Exception {
        mvc.perform(post("/api/webhook/stripe").header("Stripe-Signature", "invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signedWebhookTraversesHttpEndpointAndRejectsChangedPayload() throws Exception {
        Order order = createOrder(null);
        String payload = json.writeValueAsString(Map.of("id", "evt_test", "object", "event",
                "type", "checkout.session.completed", "api_version", Stripe.API_VERSION,
                "data", Map.of("object", Map.of("object", "checkout.session", "id", order.getTransactionId(),
                        "mode", "payment", "payment_status", "paid", "amount_total", order.getAmountTotalMinor(),
                        "currency", "hkd", "metadata", Map.of("order_id", order.getId().toString())))));
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test_only".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        mvc.perform(post("/api/webhook/stripe").header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
        mvc.perform(post("/api/webhook/stripe").header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON).content(payload.replace("hkd", "usd")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void snapshotTamperingIsRejected() {
        Order order = createOrder(null);
        Session event = paidSession(order);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var persisted = orders.findById(order.getId()).orElseThrow();
            persisted.getItems().get(0).setQuantity(10);
        });
        assertThatThrownBy(() -> webhooks.process(java.util.UUID.randomUUID().toString(), "checkout.session.completed", event))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Autowired org.flywaydb.core.Flyway flyway;
    @Autowired javax.sql.DataSource dataSource;

    @Test void migrationIsRepeatableAndUserCannotReadMysqlSystemTables() throws Exception {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(products.findById(product.getPid())).isPresent();
        try (var c = dataSource.getConnection(); var st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeQuery("SELECT * FROM mysql.user"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    @Test void concurrentLastStockAllowsExactlyOneOrder() throws Exception {
        product.setStock(3); products.saveAndFlush(product);
        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            Callable<Boolean> buy = () -> { gate.await();
                try { checkout.create(request(null), alice.getEmail()); return true; }
                catch (ResponseStatusException e) { assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); return false; }
            };
            var first = pool.submit(buy); var second = pool.submit(buy); gate.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isZero();
            assertThat(orders.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void repeatedConcurrentEventIsPersistedOnceAndReleasesStockOnce() throws Exception {
        voucher(1);
        Order order = createOrder("SAVE");
        Session event = paidSession(order);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> webhooks.process("evt_repeat", "checkout.session.expired", event));
            var second = pool.submit(() -> webhooks.process("evt_repeat", "checkout.session.expired", event));
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
            assertThat(events.count()).isEqualTo(1);
            assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(100);
            assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void invalidPaymentRollsBackEventRecordAndKeepsReservation() {
        Order order = createOrder(null); Session event = paidSession(order); event.setCurrency("usd");
        assertThatThrownBy(() -> webhooks.process("evt_bad", "checkout.session.completed", event))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(events.count()).isZero();
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
    }

    @Test void mysqlPreservesCaseSensitiveIdentifiersAndForeignKeys() {
        user("ALICE@example.com");
        assertThat(users.findByEmail("Alice@example.com")).isEmpty();
        assertThatThrownBy(() -> categories.deleteAllInBatch())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void bootstrapDoesNotResetExistingPasswordOrRole() {
        String original = alice.getPassword();
        new cc.iecuhk.iems5718.s57.component.DataInitializer(users, encoder, alice.getEmail(), "AnotherPassword123!").run();
        var unchanged = users.findById(alice.getUserid()).orElseThrow();
        assertThat(unchanged.getPassword()).isEqualTo(original);
        assertThat(unchanged.getRole()).isEqualTo("ROLE_USER");
    }

    @Test void dumpRestoresIntoIsolatedDatabaseAndApplicationCannotAccessIt() throws Exception {
        createOrder(null);
        var result = MYSQL.execInContainer("sh", "-c",
                "export MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\"; " +
                "mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF shopping_test > /tmp/shop-restore.sql && " +
                "mysql -uroot -e 'CREATE DATABASE shopping_restore' && " +
                "mysql -uroot shopping_restore < /tmp/shop-restore.sql && " +
                "mysql -uroot -N -e 'SELECT COUNT(*) FROM shopping_restore.orders'");
        assertThat(result.getExitCode()).isZero();
        assertThat(result.getStdout().trim()).isEqualTo("1");
        try (var c = dataSource.getConnection(); var st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeQuery("SELECT * FROM shopping_restore.orders"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    @Test void dataSurvivesMysqlRestart() throws Exception {
        try (var isolated = new org.testcontainers.containers.MySQLContainer<>("mysql:8.4")) {
            isolated.start();
            assertThat(isolated.execInContainer("sh", "-c", "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql -uroot test -e 'CREATE TABLE restart_probe(id INT PRIMARY KEY); INSERT INTO restart_probe VALUES(7)' ").getExitCode()).isZero();
            isolated.getDockerClient().restartContainerCmd(isolated.getContainerId()).exec();
            boolean recovered = false;
            for (int attempt = 0; attempt < 30; attempt++) {
                var result = isolated.execInContainer("sh", "-c", "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql -uroot -N test -e 'SELECT id FROM restart_probe'");
                if (result.getExitCode() == 0 && result.getStdout().trim().equals("7")) { recovered = true; break; }
                Thread.sleep(1000);
            }
            assertThat(recovered).isTrue();
        }
    }

    @Test void demoSeedIsRepeatableAndDoesNotResetStock() throws Exception {
        try (var c = dataSource.getConnection()) {
            var seed = new org.springframework.core.io.FileSystemResource("demo/seed.sql");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c, seed);
            try (var st = c.createStatement()) { st.executeUpdate("UPDATE products SET stock=2 WHERE name='Demo Desk Lamp'"); }
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c, seed);
            try (var st = c.createStatement(); var rows = st.executeQuery("SELECT COUNT(*),MIN(stock) FROM products WHERE name IN ('Demo Desk Lamp','Demo Notebook')")) {
                rows.next(); assertThat(rows.getInt(1)).isEqualTo(2); assertThat(rows.getInt(2)).isEqualTo(2);
            }
        }
    }

    @Test void adminImageUploadValidatesBytesAndStoresDecimalAndStock() throws Exception {
        alice.setRole("ROLE_ADMIN"); users.saveAndFlush(alice); var session = login(alice.getEmail());
        var invalid = new org.springframework.mock.web.MockMultipartFile("image", "bad.jpg", "image/jpeg", "not an image".getBytes());
        mvc.perform(multipart("/api/admin/products").file(invalid).session(session)
                .header("X-XSRF-TOKEN", csrf(session)).param("catid", product.getCatid().toString())
                .param("name", "Bad").param("price", "12.34").param("stock", "4").param("description", "Test"))
                .andExpect(status().isBadRequest());
        assertThat(products.count()).isEqualTo(1);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var valid = new org.springframework.mock.web.MockMultipartFile("image", "../../image.png", "image/png", output.toByteArray());
        mvc.perform(multipart("/api/admin/products").file(valid).session(session)
                .header("X-XSRF-TOKEN", csrf(session)).param("catid", product.getCatid().toString())
                .param("name", "Uploaded").param("price", "12.34").param("stock", "4").param("description", "Test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(4))
                .andExpect(jsonPath("$.price").value(12.34));
        var uploaded = products.findAll().stream().filter(p -> p.getName().equals("Uploaded")).findFirst().orElseThrow();
        assertThat(java.nio.file.Files.exists(java.nio.file.Path.of("target/test-uploads/" + uploaded.getPid() + "_large.jpg"))).isTrue();
    }

    private com.fasterxml.jackson.databind.node.ObjectNode snapshot(Order order, String eventId,
                                                                    String type, String paymentStatus) {
        var root = json.createObjectNode();
        root.put("id", eventId).put("object", "event").put("type", type)
                .put("api_version", "2026-03-25.dahlia").put("livemode", false);
        var session = root.putObject("data").putObject("object");
        session.put("object", "checkout.session").put("id", order.getTransactionId())
                .put("mode", "payment").put("payment_status", paymentStatus)
                .put("amount_total", order.getAmountTotalMinor()).put("currency", "hkd");
        session.putObject("metadata").put("order_id", order.getId().toString());
        return root;
    }

    private org.springframework.test.web.servlet.ResultActions signedSnapshot(String payload, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test_only".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(
                mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return mvc.perform(post("/api/webhook/stripe").header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON).content(payload));
    }

    private org.springframework.test.web.servlet.ResultActions signedSnapshot(com.fasterxml.jackson.databind.JsonNode payload) throws Exception {
        return signedSnapshot(json.writeValueAsString(payload), Instant.now().getEpochSecond());
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkout.session.completed", "checkout.session.async_payment_succeeded"})
    void dahliaPaidSnapshotAndRedeliveryDoNotRepeatStockOrVoucherUpdates(String type) throws Exception {
        voucher(1); Order order = createOrder("SAVE");
        var event = snapshot(order, "evt_paid", type, "paid");
        signedSnapshot(event).andExpect(status().isOk());
        signedSnapshot(event).andExpect(status().isOk());
        assertThat(events.count()).isEqualTo(1);
        signedSnapshot(snapshot(order, "evt_late_failure", "checkout.session.async_payment_failed", "unpaid"))
                .andExpect(status().isOk());
        signedSnapshot(snapshot(order, "evt_late_expiry", "checkout.session.expired", "unpaid"))
                .andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isZero();
    }

    @Test void dahliaAsyncPaymentWaitsForSettlement() throws Exception {
        voucher(1); Order order = createOrder("SAVE");
        signedSnapshot(snapshot(order, "evt_wait", "checkout.session.completed", "unpaid"))
                .andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
        signedSnapshot(snapshot(order, "evt_settled", "checkout.session.async_payment_succeeded", "paid"))
                .andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isZero();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "checkout.session.completed,unpaid", "checkout.session.async_payment_succeeded,unpaid",
            "checkout.session.completed,no_payment_required"})
    void nonzeroDahliaOrderRequiresPaidStatus(String type, String paymentStatus) throws Exception {
        Order order = createOrder(null);
        signedSnapshot(snapshot(order, "evt_unpaid", type, paymentStatus)).andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"checkout.session.expired,EXPIRED", "checkout.session.async_payment_failed,FAILED"})
    void dahliaFailureReleasesReservationsOnceAndLateSuccessRequiresReconciliation(String type, String terminal) throws Exception {
        voucher(1); Order order = createOrder("SAVE");
        var event = snapshot(order, "evt_release", type, "unpaid");
        signedSnapshot(event).andExpect(status().isOk());
        signedSnapshot(event).andExpect(status().isOk());
        event.put("id", "evt_second_delivery");
        signedSnapshot(event).andExpect(status().isOk());
        signedSnapshot(snapshot(order, "evt_late_success", "checkout.session.async_payment_succeeded", "paid"))
                .andExpect(status().isConflict());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(terminal);
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(100);
        assertThat(vouchers.findByCode("SAVE").orElseThrow().getQuota()).isEqualTo(1);
        assertThat(events.count()).isEqualTo(2);
        assertThat(events.existsById("evt_late_success")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-10-16", "2025-11-17.clover", "2026-04-29.dahlia", ""})
    void untestedSnapshotVersionsAreRejectedEvenWithinDahlia(String version) throws Exception {
        Order order = createOrder(null);
        var event = snapshot(order, "evt_version", "checkout.session.completed", "paid");
        event.put("api_version", version);
        signedSnapshot(event).andExpect(status().isServiceUnavailable());
        assertThat(events.count()).isZero();
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @ParameterizedTest
    @ValueSource(strings = {"thin", "missing_event_id", "missing_type", "missing_version", "missing_data",
            "missing_object", "wrong_object", "missing_session_id", "missing_amount", "missing_currency",
            "missing_mode", "missing_payment_status", "unknown_payment_status", "malformed_json"})
    void malformedOrThinSnapshotsNeverAcknowledgeSuccess(String problem) throws Exception {
        Order order = createOrder(null);
        var event = snapshot(order, "evt_invalid", "checkout.session.completed", "paid");
        var object = (com.fasterxml.jackson.databind.node.ObjectNode) event.path("data").path("object");
        switch (problem) {
            case "thin" -> event.put("object", "v2.core.event");
            case "missing_event_id" -> event.remove("id");
            case "missing_type" -> event.remove("type");
            case "missing_version" -> event.remove("api_version");
            case "missing_data" -> event.remove("data");
            case "missing_object" -> ((com.fasterxml.jackson.databind.node.ObjectNode) event.get("data")).remove("object");
            case "wrong_object" -> object.put("object", "payment_intent");
            case "missing_session_id" -> object.remove("id");
            case "missing_amount" -> object.remove("amount_total");
            case "missing_currency" -> object.remove("currency");
            case "missing_mode" -> object.remove("mode");
            case "missing_payment_status" -> object.remove("payment_status");
            case "unknown_payment_status" -> object.put("payment_status", "unexpected_status");
        }
        String payload = problem.equals("malformed_json") ? "{" : json.writeValueAsString(event);
        int status = signedSnapshot(payload, Instant.now().getEpochSecond()).andReturn().getResponse().getStatus();
        assertThat(status).isBetween(400, 599);
        assertThat(events.count()).isZero();
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(products.findById(product.getPid()).orElseThrow().getStock()).isEqualTo(97);
    }

    @Test void staleButCorrectSignatureIsRejected() throws Exception {
        Order order = createOrder(null);
        String payload = json.writeValueAsString(snapshot(order, "evt_old_signature", "checkout.session.completed", "paid"));
        signedSnapshot(payload, Instant.now().getEpochSecond() - 600).andExpect(status().isBadRequest());
        assertThat(events.count()).isZero();
    }

    @Test void demoKeepsNormalSessionAndCannotUseItsPrivileges() throws Exception {
        var normal = login(alice.getEmail());
        var created = mvc.perform(post("/api/demo/session").session(normal)
            .header("Idempotency-Key", "e".repeat(64)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andReturn();
        String token=json.readTree(created.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/demo/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/orders/my-orders").session(normal).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/checkout/create-session").session(normal).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"cartItems\":[]}" )).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(normal)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(alice.getEmail()));
        mvc.perform(post("/api/demo/session").header("Idempotency-Key","f".repeat(64)).header("Origin","https://attacker.invalid")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test void registrationCreatesRestrictedUserAndRejectsDuplicates() throws Exception {
        var anonymous=new MockHttpSession();String token=csrf(anonymous);
        String email="portfolio-registration@example.invalid";
        mvc.perform(post("/api/auth/register").session(anonymous).header("X-XSRF-TOKEN",token)
            .param("email",email).param("password","RegistrationFixture1!").param("confirmPassword","RegistrationFixture1!"))
            .andExpect(status().isOk());
        var registered=users.findByEmail(email).orElseThrow();
        assertThat(registered.getRole()).isEqualTo("ROLE_USER");
        assertThat(encoder.matches("RegistrationFixture1!",registered.getPassword())).isTrue();
        mvc.perform(post("/api/auth/register").session(anonymous).header("X-XSRF-TOKEN",token)
            .param("email",email).param("password","RegistrationFixture1!").param("confirmPassword","RegistrationFixture1!"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").session(anonymous).header("X-XSRF-TOKEN",token)
            .param("email","invalid").param("password","RegistrationFixture1!").param("confirmPassword","RegistrationFixture1!"))
            .andExpect(status().isBadRequest());
    }
}
