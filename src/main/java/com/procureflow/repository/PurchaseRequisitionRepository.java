package com.procureflow.repository;

import com.procureflow.entity.PurchaseRequisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRequisitionRepository
        extends JpaRepository<PurchaseRequisition, Long> {

    Optional<PurchaseRequisition> findByRequisitionNumber(
            String requisitionNumber
    );

    boolean existsByRequisitionNumber(
            String requisitionNumber
    );
}