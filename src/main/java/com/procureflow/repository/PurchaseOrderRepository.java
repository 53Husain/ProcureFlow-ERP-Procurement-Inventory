package com.procureflow.repository;

import com.procureflow.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Serialize receipt attempts for one order until the surrounding transaction ends.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findByIdForReceipt(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<PurchaseOrder> findByPurchaseOrderNumber(String purchaseOrderNumber);

    boolean existsByPurchaseOrderNumber(String purchaseOrderNumber);
}
