package com.procureflow.repository;

import com.procureflow.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPurchaseOrderNumber(String purchaseOrderNumber);

    boolean existsByPurchaseOrderNumber(String purchaseOrderNumber);
}