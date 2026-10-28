package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") Integer id);

    @Modifying
    @Query("update Order o set o.status = :status where o.id = :id and o.status = 'PENDING'")
    int transitionPending(@Param("id") Integer id, @Param("status") String status);

    Optional<Order> findByTransactionIdAndUserid(String transactionId, Integer userid);

    // 修改点：去掉 Top5。这样“我的订单”页面才能加载出用户所有的历史记录。
    // Spring Data JPA 会自动生成 SQL：SELECT * FROM orders WHERE userid = ? ORDER BY
    // created_at DESC
    List<Order> findByUseridOrderByCreatedAtDesc(Integer userid);

    // 保留：用于 Webhook 校验防重。这是非常专业的做法，能确保同一笔 Stripe 交易不会被系统处理两次。
    Order findByTransactionId(String transactionId);
}
