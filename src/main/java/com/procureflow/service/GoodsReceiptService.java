package com.procureflow.service;

import com.procureflow.entity.GoodsReceipt;
import com.procureflow.entity.GoodsReceiptItem;
import com.procureflow.entity.Material;
import com.procureflow.entity.PurchaseOrder;
import com.procureflow.entity.PurchaseOrderItem;
import com.procureflow.enums.GoodsReceiptStatus;
import com.procureflow.enums.PurchaseOrderStatus;
import com.procureflow.repository.GoodsReceiptRepository;
import com.procureflow.repository.MaterialRepository;
import com.procureflow.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockLock materialStockLock;

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            MaterialRepository materialRepository,
            MaterialStockLock materialStockLock) {

        this.goodsReceiptRepository = goodsReceiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.materialRepository = materialRepository;
        this.materialStockLock = materialStockLock;
    }

    @Transactional
    public GoodsReceipt receiveGoods(
            Long purchaseOrderId,
            String receiptNumber) {

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByIdForReceipt(purchaseOrderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase order not found with id: "
                                        + purchaseOrderId));

        // This workflow receives every line in full: partial/repeated receipts are unsupported.
        // Check while holding the order lock, before any stock changes.
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.CREATED
                && purchaseOrder.getStatus() != PurchaseOrderStatus.SENT_TO_VENDOR) {
            throw new ReceiptConflictException("Purchase order is not eligible for a full receipt");
        }
        if (goodsReceiptRepository.existsByPurchaseOrderId(purchaseOrderId)) {
            throw new ReceiptConflictException("Purchase order already has a goods receipt");
        }

        if (purchaseOrder.getItems() == null ||
                purchaseOrder.getItems().isEmpty()) {

            throw new IllegalStateException(
                    "Purchase order does not contain any items");
        }

        if (goodsReceiptRepository.existsByReceiptNumber(receiptNumber)) {
            throw new IllegalArgumentException(
                    "Receipt number already exists");
        }

        List<GoodsReceiptItem> receiptItems = new ArrayList<>();

        // Different orders can contain the same materials. Lock every distinct
        // material in the same order to avoid opposite-line-order deadlocks.
        // Refresh each only once: repeated lines must accumulate, not reset stock.
        Map<Long, Material> lockedMaterials = new TreeMap<>();
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            lockedMaterials.put(item.getMaterial().getId(), null);
        }
        for (Long materialId : lockedMaterials.keySet()) {
            lockedMaterials.put(materialId, materialStockLock.lock(materialId));
        }

        for (PurchaseOrderItem orderItem : purchaseOrder.getItems()) {

            Material material = lockedMaterials.get(orderItem.getMaterial().getId());

            int receivedQuantity = orderItem.getQuantity();

            material.setCurrentStock(
                    material.getCurrentStock() + receivedQuantity
            );

            materialRepository.save(material);

            GoodsReceiptItem receiptItem = new GoodsReceiptItem();

            receiptItem.setMaterial(material);
            receiptItem.setReceivedQuantity(receivedQuantity);

            receiptItems.add(receiptItem);
        }

        GoodsReceipt goodsReceipt = new GoodsReceipt();

        goodsReceipt.setReceiptNumber(receiptNumber);
        goodsReceipt.setPurchaseOrder(purchaseOrder);
        goodsReceipt.setReceiptDate(LocalDateTime.now());
        goodsReceipt.setStatus(GoodsReceiptStatus.RECEIVED);

        for (GoodsReceiptItem item : receiptItems) {
            item.setGoodsReceipt(goodsReceipt);
        }

        goodsReceipt.setItems(receiptItems);

        purchaseOrder.setStatus(
                PurchaseOrderStatus.FULLY_RECEIVED
        );

        purchaseOrderRepository.save(purchaseOrder);

        return goodsReceiptRepository.save(goodsReceipt);
    }

    public List<GoodsReceipt> getAllGoodsReceipts() {
        return goodsReceiptRepository.findAll();
    }

    public GoodsReceipt getGoodsReceiptById(Long id) {
        return goodsReceiptRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Goods receipt not found with id: " + id));
    }
}
