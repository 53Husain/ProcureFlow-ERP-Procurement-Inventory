package com.procureflow.service;

import com.procureflow.entity.*;
import com.procureflow.enums.*;
import com.procureflow.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseOrderServiceTest {
    private final PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    private final PurchaseRequisitionRepository requisitions = mock(PurchaseRequisitionRepository.class);
    private final MaterialRepository materials = mock(MaterialRepository.class);
    private final VendorRepository vendors = mock(VendorRepository.class);
    private final PurchaseOrderService service = new PurchaseOrderService(orders, requisitions, materials, vendors);

    private PurchaseRequisition approved() {
        var request = new PurchaseRequisition();
        request.setStatus(RequisitionStatus.APPROVED);
        when(requisitions.findById(1L)).thenReturn(Optional.of(request));
        return request;
    }

    private PurchaseRequisitionItem item(long id, int quantity) {
        Material material = new Material(); ReflectionTestUtils.setField(material, "id", id);
        var item = new PurchaseRequisitionItem(); item.setMaterial(material); item.setQuantity(quantity);
        return item;
    }

    @Test
    void calculatesDecimalTotalAndLinksOrderItems() {
        var request = approved(); request.setItems(List.of(item(10L, 3), item(20L, 2)));
        Material first = new Material(); first.setUnitPrice(new BigDecimal("12.50"));
        Material second = new Material(); second.setUnitPrice(new BigDecimal("0.10"));
        Vendor vendor = new Vendor();
        when(materials.findById(10L)).thenReturn(Optional.of(first));
        when(materials.findById(20L)).thenReturn(Optional.of(second));
        when(vendors.findById(2L)).thenReturn(Optional.of(vendor));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));
        var order = service.createPurchaseOrder(1L, 2L, "PO-1", "Demo");
        assertEquals(0, new BigDecimal("37.70").compareTo(order.getTotalAmount()));
        assertEquals(PurchaseOrderStatus.CREATED, order.getStatus());
        assertEquals("PO-1", order.getPurchaseOrderNumber());
        assertSame(vendor, order.getVendor());
        assertSame(request, order.getRequisition());
        assertEquals(2, order.getItems().size());
        assertSame(order, order.getItems().get(0).getPurchaseOrder());
        assertEquals(3, order.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("12.50"), order.getItems().get(0).getUnitPrice());
        assertEquals(RequisitionStatus.CONVERTED_TO_PO, request.getStatus());
        verify(requisitions).save(request);
        verify(orders).save(order);
    }

    @ParameterizedTest
    @EnumSource(value = RequisitionStatus.class, names = "APPROVED", mode = EnumSource.Mode.EXCLUDE)
    void refusesNonApprovedRequests(RequisitionStatus status) {
        var request = approved(); request.setStatus(status);
        assertThrows(IllegalStateException.class, () -> service.createPurchaseOrder(1L, 2L, "PO-1", ""));
        verify(orders, never()).save(any());
        verify(requisitions, never()).save(any());
        verifyNoInteractions(materials, vendors);
    }

    @Test
    void rejectsDuplicateOrderNumber() {
        approved(); when(orders.existsByPurchaseOrderNumber("PO-1")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(1L, 2L, "PO-1", ""));
        verify(orders, never()).save(any());
        verify(requisitions, never()).save(any());
    }

    @Test
    void refusesMissingPriceBeforeConvertingRequest() {
        var request = approved(); request.setItems(List.of(item(10L, 2)));
        when(vendors.findById(2L)).thenReturn(Optional.of(new Vendor()));
        when(materials.findById(10L)).thenReturn(Optional.of(new Material()));
        assertThrows(IllegalStateException.class, () -> service.createPurchaseOrder(1L, 2L, "PO-1", ""));
        assertEquals(RequisitionStatus.APPROVED, request.getStatus());
        verify(requisitions, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void refusesEmptyRequest() {
        var request = approved(); request.setItems(List.of());
        when(vendors.findById(2L)).thenReturn(Optional.of(new Vendor()));
        assertThrows(IllegalStateException.class, () -> service.createPurchaseOrder(1L, 2L, "PO-1", ""));
        verify(orders, never()).save(any());
    }
}
