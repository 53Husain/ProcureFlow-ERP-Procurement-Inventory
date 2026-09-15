package com.procureflow.controller;

import com.procureflow.entity.GoodsReceipt;
import com.procureflow.service.GoodsReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(
            GoodsReceiptService goodsReceiptService) {

        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping
    public ResponseEntity<GoodsReceipt> receiveGoods(
            @RequestParam Long purchaseOrderId,
            @RequestParam String receiptNumber) {

        GoodsReceipt receipt =
                goodsReceiptService.receiveGoods(
                        purchaseOrderId,
                        receiptNumber
                );

        return new ResponseEntity<>(
                receipt,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<GoodsReceipt>> getAllGoodsReceipts() {
        return ResponseEntity.ok(
                goodsReceiptService.getAllGoodsReceipts()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceipt> getGoodsReceiptById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                goodsReceiptService.getGoodsReceiptById(id)
        );
    }
}