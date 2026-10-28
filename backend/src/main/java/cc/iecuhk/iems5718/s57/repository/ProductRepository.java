package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Product p set p.stock = p.stock - :quantity where p.pid = :id and p.stock >= :quantity")
    int reserve(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("quantity") int quantity);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Product p set p.stock = p.stock + :quantity where p.pid = :id")
    int release(@org.springframework.data.repository.query.Param("id") Integer id, @org.springframework.data.repository.query.Param("quantity") int quantity);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.pid = :id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
    // Custom query to fetch products based on category ID (Requirement for main
    // page)
    List<Product> findByCatid(Integer catid);
}