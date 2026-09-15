package com.procureflow.controller;

import com.procureflow.entity.PurchaseOrder;
import com.procureflow.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(
            @RequestParam Long requisitionId,
            @RequestParam Long vendorId,
            @RequestParam String purchaseOrderNumber,
            @RequestParam(required = false) String remarks) {

        PurchaseOrder purchaseOrder =
                purchaseOrderService.createPurchaseOrder(
                        requisitionId,
                        vendorId,
                        purchaseOrderNumber,
                        remarks
                );

        return new ResponseEntity<>(
                purchaseOrder,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {

        return ResponseEntity.ok(
                purchaseOrderService.getAllPurchaseOrders()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrderById(id)
        );
    }
}