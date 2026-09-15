package com.procureflow.service;

import com.procureflow.entity.Material;
import com.procureflow.entity.PurchaseRequisition;
import com.procureflow.entity.PurchaseRequisitionItem;
import com.procureflow.enums.RequisitionStatus;
import com.procureflow.repository.MaterialRepository;
import com.procureflow.repository.PurchaseRequisitionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository requisitionRepository;
    private final MaterialRepository materialRepository;

    public PurchaseRequisitionService(
            PurchaseRequisitionRepository requisitionRepository,
            MaterialRepository materialRepository) {

        this.requisitionRepository = requisitionRepository;
        this.materialRepository = materialRepository;
    }

    // Create a new purchase requisition
    public PurchaseRequisition createRequisition(
            PurchaseRequisition requisition) {

        if (requisitionRepository.existsByRequisitionNumber(
                requisition.getRequisitionNumber())) {

            throw new IllegalArgumentException(
                    "Requisition number already exists");
        }

        if (requisition.getItems() == null ||
                requisition.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "Purchase requisition must contain at least one item");
        }

        for (PurchaseRequisitionItem item : requisition.getItems()) {

            if (item.getQuantity() == null ||
                    item.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Item quantity must be greater than zero");
            }

            if (item.getMaterial() == null ||
                    item.getMaterial().getId() == null) {

                throw new IllegalArgumentException(
                        "Each item must contain a valid material ID");
            }

            Material material = materialRepository
                    .findById(item.getMaterial().getId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Material not found with id: "
                                            + item.getMaterial().getId()));

            item.setMaterial(material);
            item.setRequisition(requisition);
        }

        if (requisition.getRequestedDate() == null) {
            requisition.setRequestedDate(LocalDateTime.now());
        }

        requisition.setStatus(
                RequisitionStatus.PENDING_APPROVAL
        );

        return requisitionRepository.save(requisition);
    }

    // Get all purchase requisitions
    public List<PurchaseRequisition> getAllRequisitions() {

        return requisitionRepository.findAll();
    }

    // Get requisition by ID
    public PurchaseRequisition getRequisitionById(Long id) {

        return requisitionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase requisition not found with id: "
                                        + id));
    }

    // Approve requisition
    public PurchaseRequisition approveRequisition(Long id) {

        PurchaseRequisition requisition =
                getRequisitionById(id);

        if (requisition.getStatus()
                != RequisitionStatus.PENDING_APPROVAL) {

            throw new IllegalStateException(
                    "Only pending requisitions can be approved");
        }

        requisition.setStatus(
                RequisitionStatus.APPROVED
        );

        return requisitionRepository.save(requisition);
    }

    // Reject requisition
    public PurchaseRequisition rejectRequisition(Long id) {

        PurchaseRequisition requisition =
                getRequisitionById(id);

        if (requisition.getStatus()
                != RequisitionStatus.PENDING_APPROVAL) {

            throw new IllegalStateException(
                    "Only pending requisitions can be rejected");
        }

        requisition.setStatus(
                RequisitionStatus.REJECTED
        );

        return requisitionRepository.save(requisition);
    }
}