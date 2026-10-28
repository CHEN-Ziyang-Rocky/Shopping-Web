package cc.iecuhk.iems5718.s57.controller;

import cc.iecuhk.iems5718.s57.entity.Category;
import cc.iecuhk.iems5718.s57.entity.Product;
import cc.iecuhk.iems5718.s57.service.CategoryService;
import cc.iecuhk.iems5718.s57.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // 🚀 修改：接收 String 类型的 catid，并提取数字
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts(@RequestParam(required = false) String catid) {
        if (catid != null && !catid.isEmpty()) {
            try {
                // 将 "2-Fruits" 拆分，只取数字 "2"
                Integer realCatId = Integer.parseInt(catid.split("-")[0]);
                return ResponseEntity.ok(productService.getProductsByCategory(realCatId));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // receive the id of type String (slug) and extract the number
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        try {
            // Split "9-apple" and only take the number "9"
            Integer realId = Integer.parseInt(id.split("-")[0]);
            return productService.getProductById(realId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}