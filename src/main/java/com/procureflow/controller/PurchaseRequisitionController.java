package com.procureflow.controller;

import com.procureflow.entity.PurchaseRequisition;
import com.procureflow.service.PurchaseRequisitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requisitions")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService requisitionService;

    public PurchaseRequisitionController(
            PurchaseRequisitionService requisitionService) {

        this.requisitionService = requisitionService;
    }

    // Create purchase requisition
    @PostMapping
    public ResponseEntity<PurchaseRequisition> createRequisition(
            @RequestBody PurchaseRequisition requisition) {

        PurchaseRequisition created =
                requisitionService.createRequisition(requisition);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Get all requisitions
    @GetMapping
    public ResponseEntity<List<PurchaseRequisition>>
    getAllRequisitions() {

        return ResponseEntity.ok(
                requisitionService.getAllRequisitions()
        );
    }

    // Get requisition by ID
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseRequisition> getRequisitionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                requisitionService.getRequisitionById(id)
        );
    }

    // Approve requisition
    @PutMapping("/{id}/approve")
    public ResponseEntity<PurchaseRequisition> approveRequisition(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                requisitionService.approveRequisition(id)
        );
    }

    // Reject requisition
    @PutMapping("/{id}/reject")
    public ResponseEntity<PurchaseRequisition> rejectRequisition(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                requisitionService.rejectRequisition(id)
        );
    }
}