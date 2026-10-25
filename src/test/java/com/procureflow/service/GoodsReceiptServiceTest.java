package com.procureflow.service;

import com.procureflow.entity.*;
import com.procureflow.enums.*;
import com.procureflow.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoodsReceiptServiceTest {
    private final GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
    private final PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    private final MaterialRepository materials = mock(MaterialRepository.class);
    private final GoodsReceiptService service = new GoodsReceiptService(receipts, orders, materials);

    private PurchaseOrder order() {
        var material = new Material(); ReflectionTestUtils.setField(material, "id", 10L);
        var item = new PurchaseOrderItem(); item.setMaterial(material); item.setQuantity(4);
        var order = new PurchaseOrder(); order.setStatus(PurchaseOrderStatus.CREATED); order.setItems(List.of(item));
        when(orders.findById(1L)).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    void addsReceivedQuantityAndLinksReceipt() {
        var order = order();
        var material = new Material(); material.setCurrentStock(7);
        when(materials.findById(10L)).thenReturn(Optional.of(material));
        when(receipts.save(any(GoodsReceipt.class))).thenAnswer(i -> i.getArgument(0));
        var receipt = service.receiveGoods(1L, "GR-1");
        assertEquals(11, material.getCurrentStock());
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, order.getStatus());
        assertEquals(GoodsReceiptStatus.RECEIVED, receipt.getStatus());
        assertEquals("GR-1", receipt.getReceiptNumber());
        assertSame(order, receipt.getPurchaseOrder());
        assertEquals(1, receipt.getItems().size());
        assertEquals(4, receipt.getItems().get(0).getReceivedQuantity());
        assertSame(material, receipt.getItems().get(0).getMaterial());
        assertSame(receipt, receipt.getItems().get(0).getGoodsReceipt());
        verify(materials).save(material);
        verify(orders).save(order);
        verify(receipts).save(receipt);
    }

    @Test
    void duplicateReceiptNumberDoesNotTouchStock() {
        var order = order();
        when(receipts.existsByReceiptNumber("GR-1")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.receiveGoods(1L, "GR-1"));
        assertEquals(PurchaseOrderStatus.CREATED, order.getStatus());
        verifyNoInteractions(materials);
        verify(orders, never()).save(any());
        verify(receipts, never()).save(any());
    }

    @Test
    void emptyOrderDoesNotSaveReceipt() {
        order().setItems(List.of());
        assertThrows(IllegalStateException.class, () -> service.receiveGoods(1L, "GR-1"));
        verifyNoInteractions(materials);
        verify(receipts, never()).save(any());
    }

    @Test
    void missingMaterialPreventsFirstStockWrite() {
        var order = order();
        when(materials.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.receiveGoods(1L, "GR-1"));
        assertEquals(PurchaseOrderStatus.CREATED, order.getStatus());
        verify(materials, never()).save(any());
        verify(orders, never()).save(any());
        verify(receipts, never()).save(any());
    }
}
