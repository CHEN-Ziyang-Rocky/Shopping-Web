package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    java.util.List<Voucher> findByQuotaGreaterThanOrderByDiscountAsc(Integer quota);

    @Modifying
    @Query("update Voucher v set v.quota = v.quota + 1, v.version = v.version + 1 where v.code = :code")
    int releaseQuota(@Param("code") String code);
}
