package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.entity.Category;
import cc.iecuhk.iems5718.s57.repository.OrderRepository;
import cc.iecuhk.iems5718.s57.service.CategoryService;
import cc.iecuhk.iems5718.s57.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    // 🚀 新增：注入 OrderRepository 以供管理员查询所有订单
    @Autowired
    private OrderRepository orderRepository;

    // ================= CATEGORY MANAGEMENT =================

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestParam String name) {
        return ResponseEntity.ok(categoryService.createCategory(name));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // ================= PRODUCT MANAGEMENT =================

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(
            @RequestParam("catid") Integer catid,
            @RequestParam("name") String name,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("description") String description,
            @RequestParam(value="stock", required=false) Integer stock,
            @RequestParam("image") MultipartFile file) throws Exception {

        return ResponseEntity.ok(productService.createProduct(catid, name, price, description, stock, file));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestParam("catid") Integer catid,
            @RequestParam("name") String name,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("description") String description,
            @RequestParam(value="stock", required=false) Integer stock,
            @RequestParam(value = "image", required = false) MultipartFile file) throws Exception {

        return productService.updateProduct(id, catid, name, price, description, stock, file)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // ================= ORDER MANAGEMENT =================

    // 🚀 新增：作业要求 Display the DB orders table in admin panel
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}