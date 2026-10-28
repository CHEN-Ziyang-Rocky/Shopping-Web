package cc.iecuhk.iems5718.s57.service;

import cc.iecuhk.iems5718.s57.entity.Product;
import cc.iecuhk.iems5718.s57.repository.ProductRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Value("${app.upload.dir}")
    private String UPLOAD_DIR;

    // 查
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(Integer catid) {
        return productRepository.findByCatid(catid);
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    // 删
    public void deleteProduct(Integer id) {
        productRepository.deleteById(id);
    }

    // 增 (包含图片处理)
    @Transactional(rollbackFor = IOException.class)
    public Product createProduct(Integer catid, String name, java.math.BigDecimal price, String description, Integer stock, MultipartFile file)
            throws IOException {
        Money.cents(price);
        Product product = new Product();
        product.setCatid(catid);
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        if (stock != null) {
            if (stock < 0) throw new IllegalArgumentException("Invalid stock");
            product.setStock(stock);
        }

        // 先保存获取 PID
        product = productRepository.save(product);

        // 处理图片
        return processAndSaveImage(product, file);
    }

    // 改 (包含图片处理)
    @Transactional(rollbackFor = IOException.class)
    public Optional<Product> updateProduct(Integer id, Integer catid, String name, java.math.BigDecimal price, String description, Integer stock,
            MultipartFile file) throws IOException {
        Money.cents(price);
        Optional<Product> optionalProduct = productRepository.lockById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setCatid(catid);
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            if (stock != null) {
                if (stock < 0) throw new IllegalArgumentException("Invalid stock");
                product.setStock(stock);
            }

            if (file != null && !file.isEmpty()) {
                processAndSaveImage(product, file); // 更新图片
            } else {
                productRepository.save(product); // 仅更新文本
            }
            return Optional.of(product);
        }
        return Optional.empty();
    }

    // 内部私有方法：专门负责图片裁剪的业务逻辑
    private Product processAndSaveImage(Product product, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 10 * 1024 * 1024 ||
                    !java.util.Set.of("image/jpeg", "image/png", "image/webp").contains(java.util.Objects.toString(file.getContentType(), ""))) {
                throw new IllegalArgumentException("Unsupported image");
            }
            try (var input = javax.imageio.ImageIO.createImageInputStream(file.getInputStream())) {
                var readers = javax.imageio.ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000) throw new IllegalArgumentException("Image too large");
                } finally { reader.dispose(); }
            }
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists())
                uploadDir.mkdirs();

            String largeFilename = product.getPid() + "_large.jpg";
            String thumbFilename = product.getPid() + "_thumb.jpg";
            Path largePath = Paths.get(UPLOAD_DIR).resolve(largeFilename);
            Path thumbPath = Paths.get(UPLOAD_DIR).resolve(thumbFilename);

            try (var input = file.getInputStream()) {
                Thumbnails.of(input).size(800, 800).outputFormat("jpg").toFile(largePath.toFile());
            }
            try (var input = file.getInputStream()) {
                Thumbnails.of(input).size(250, 250).outputFormat("jpg").toFile(thumbPath.toFile());
            }

            product.setLargeImage("/uploads/" + largeFilename);
            product.setThumbnailImage("/uploads/" + thumbFilename);
            return productRepository.save(product);
        }
        return product;
    }
}
