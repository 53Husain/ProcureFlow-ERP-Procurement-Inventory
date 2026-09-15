package com.procureflow.controller;

import com.procureflow.entity.PurchaseRequisitionItem;
import com.procureflow.service.PurchaseRequisitionItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requisition-items")
public class PurchaseRequisitionItemController {

    private final PurchaseRequisitionItemService itemService;

    public PurchaseRequisitionItemController(
            PurchaseRequisitionItemService itemService) {

        this.itemService = itemService;
    }

    // Get all items for a requisition
    @GetMapping("/requisition/{requisitionId}")
    public ResponseEntity<List<PurchaseRequisitionItem>>
    getItemsByRequisitionId(
            @PathVariable Long requisitionId) {

        return ResponseEntity.ok(
                itemService.getItemsByRequisitionId(requisitionId)
        );
    }

    // Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseRequisitionItem> getItemById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                itemService.getItemById(id)
        );
    }

    // Delete an individual item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id) {

        itemService.deleteItem(id);

        return ResponseEntity.noContent().build();
    }
}