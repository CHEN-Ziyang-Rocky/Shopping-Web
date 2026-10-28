package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.repository.VoucherRepository;
import cc.iecuhk.iems5718.s57.service.CheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkout;
    private final VoucherRepository vouchers;

    public CheckoutController(CheckoutService checkout, VoucherRepository vouchers) {
        this.checkout = checkout;
        this.vouchers = vouchers;
    }

    public static class CheckoutRequest {
        @NotEmpty @Size(max = 100)
        public List<@NotNull @Valid CartItemRequest> cartItems;
        @Size(max = 50)
        public String vcode;
    }

    public static class CartItemRequest {
        @NotNull @Positive
        public Integer pid;
        @NotNull @Min(1) @Max(99)
        public Integer quantity;
    }

    @GetMapping("/validate-voucher")
    public ResponseEntity<?> validateVoucher(@RequestParam String vcode) {
        var voucher = vouchers.findByCode(vcode.trim());
        if (voucher.isEmpty() || voucher.get().getQuota() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or exhausted voucher code."));
        }
        return ResponseEntity.ok(Map.of("discount", voucher.get().getDiscount()));
    }

    @PostMapping("/create-session")
    public ResponseEntity<?> createCheckoutSession(@Valid @RequestBody CheckoutRequest request,
                                                   Authentication authentication) {
        var session = checkout.create(request, authentication.getName());
        return ResponseEntity.ok(Map.of("url", session.url()));
    }
}
