package com.procureflow.service;

import com.procureflow.entity.Material;
import com.procureflow.entity.PurchaseOrder;
import com.procureflow.entity.PurchaseOrderItem;
import com.procureflow.entity.PurchaseRequisition;
import com.procureflow.entity.PurchaseRequisitionItem;
import com.procureflow.entity.Vendor;
import com.procureflow.enums.PurchaseOrderStatus;
import com.procureflow.enums.RequisitionStatus;
import com.procureflow.repository.MaterialRepository;
import com.procureflow.repository.PurchaseOrderRepository;
import com.procureflow.repository.PurchaseRequisitionRepository;
import com.procureflow.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final MaterialRepository materialRepository;
    private final VendorRepository vendorRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseRequisitionRepository requisitionRepository,
            MaterialRepository materialRepository,
            VendorRepository vendorRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.requisitionRepository = requisitionRepository;
        this.materialRepository = materialRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(
            Long requisitionId,
            Long vendorId,
            String purchaseOrderNumber,
            String remarks) {

        PurchaseRequisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase requisition not found with id: " + requisitionId));

        if (requisition.getStatus() != RequisitionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved requisitions can be converted into a purchase order");
        }

        if (purchaseOrderRepository.existsByPurchaseOrderNumber(purchaseOrderNumber)) {
            throw new IllegalArgumentException(
                    "Purchase order number already exists");
        }

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vendor not found with id: " + vendorId));

        if (requisition.getItems() == null || requisition.getItems().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot create purchase order without requisition items");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();

        purchaseOrder.setPurchaseOrderNumber(purchaseOrderNumber);
        purchaseOrder.setVendor(vendor);
        purchaseOrder.setRequisition(requisition);
        purchaseOrder.setOrderDate(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrderStatus.CREATED);
        purchaseOrder.setRemarks(remarks);

        List<PurchaseOrderItem> orderItems = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseRequisitionItem requisitionItem : requisition.getItems()) {

            Long materialId = requisitionItem.getMaterial().getId();

            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Material not found with id: " + materialId));

            if (material.getUnitPrice() == null) {
                throw new IllegalStateException(
                        "Material " + material.getMaterialCode()
                                + " does not have a unit price");
            }

            PurchaseOrderItem orderItem = new PurchaseOrderItem();

            orderItem.setPurchaseOrder(purchaseOrder);
            orderItem.setMaterial(material);
            orderItem.setQuantity(requisitionItem.getQuantity());
            orderItem.setUnitPrice(material.getUnitPrice());

            orderItems.add(orderItem);

            BigDecimal itemTotal = material.getUnitPrice()
                    .multiply(
                            BigDecimal.valueOf(requisitionItem.getQuantity())
                    );

            totalAmount = totalAmount.add(itemTotal);
        }

        purchaseOrder.setItems(orderItems);
        purchaseOrder.setTotalAmount(totalAmount);

        requisition.setStatus(RequisitionStatus.CONVERTED_TO_PO);

        requisitionRepository.save(requisition);

        return purchaseOrderRepository.save(purchaseOrder);
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase order not found with id: " + id));
    }
}