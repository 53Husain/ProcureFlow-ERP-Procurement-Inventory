package com.procureflow.repository;

import com.procureflow.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);

    boolean existsByPurchaseOrderId(Long purchaseOrderId);

    boolean existsByReceiptNumber(String receiptNumber);
}
