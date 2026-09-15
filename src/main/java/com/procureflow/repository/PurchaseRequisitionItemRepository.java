package com.procureflow.repository;

import com.procureflow.entity.PurchaseRequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequisitionItemRepository
        extends JpaRepository<PurchaseRequisitionItem, Long> {

    List<PurchaseRequisitionItem> findByRequisitionId(Long requisitionId);
}