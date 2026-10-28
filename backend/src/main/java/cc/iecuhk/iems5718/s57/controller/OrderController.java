package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.entity.Order;
import cc.iecuhk.iems5718.s57.entity.OrderItem;
import cc.iecuhk.iems5718.s57.entity.User;
import cc.iecuhk.iems5718.s57.repository.OrderRepository;
import cc.iecuhk.iems5718.s57.repository.ProductRepository;
import cc.iecuhk.iems5718.s57.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderController(OrderRepository orderRepository, UserRepository userRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/payment-status")
    public ResponseEntity<?> getPaymentStatus(@RequestParam String sessionId, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return orderRepository.findByTransactionIdAndUserid(sessionId, user.getUserid())
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of("status", order.getStatus())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 🚀 新增：获取当前用户的所有订单
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        try {
            // 1. 获取当前登录用户
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow();

            // 2. 查出该用户的所有订单
            List<Order> orders = orderRepository.findByUseridOrderByCreatedAtDesc(user.getUserid());

            // 3. 遍历订单，把商品的名称塞给 OrderItem 的 @Transient 字段
            for (Order order : orders) {
                for (OrderItem item : order.getItems()) {
                    productRepository.findById(item.getPid()).ifPresent(p -> item.setName(p.getName()));
                }
            }

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching orders");
        }
    }
}
