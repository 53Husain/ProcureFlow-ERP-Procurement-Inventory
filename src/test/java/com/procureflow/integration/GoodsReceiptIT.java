package com.procureflow.integration;

import com.procureflow.entity.*;
import com.procureflow.enums.*;
import com.procureflow.repository.*;
import com.procureflow.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Testcontainers
class GoodsReceiptIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired GoodsReceiptService service;
    @Autowired MaterialRepository materials;
    @Autowired VendorRepository vendors;
    @Autowired PurchaseRequisitionRepository requisitions;
    @Autowired PurchaseOrderRepository orders;
    @Autowired GoodsReceiptRepository receipts;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    Long orderId, materialId;

    @BeforeEach
    void fixture() {
        for (String table : List.of("goods_receipt_items", "goods_receipts", "purchase_order_items",
                "purchase_orders", "purchase_requisition_items", "purchase_requisitions", "vendors", "materials")) {
            jdbc.update("delete from " + table);
        }
        Material material = new Material();
        material.setMaterialCode("MAT-1"); material.setMaterialName("Bolt");
        material.setUnitOfMeasure("PCS"); material.setUnitPrice(BigDecimal.TEN);
        material.setCurrentStock(3); material.setReorderLevel(1); material.setActive(true);
        material = materials.save(material); materialId = material.getId();
        Vendor vendor = new Vendor();
        vendor.setVendorCode("VEN-1"); vendor.setVendorName("Demo");
        vendor.setEmail("vendor@example.com"); vendor.setActive(true); vendor = vendors.save(vendor);
        PurchaseRequisition req = new PurchaseRequisition();
        req.setRequisitionNumber("REQ-1"); req.setRequestedBy("Demo"); req.setDepartment("Operations");
        req.setRequestedDate(LocalDateTime.now()); req.setStatus(RequisitionStatus.CONVERTED_TO_PO);
        req = requisitions.save(req);
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseOrderNumber("PO-1"); order.setVendor(vendor); order.setRequisition(req);
        order.setOrderDate(LocalDateTime.now()); order.setStatus(PurchaseOrderStatus.CREATED);
        order.setTotalAmount(BigDecimal.valueOf(50));
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order); item.setMaterial(material); item.setQuantity(5); item.setUnitPrice(BigDecimal.TEN);
        order.setItems(List.of(item)); orderId = orders.save(order).getId();
    }
    void assertReceivedOnce() {
        assertEquals(8, materials.findById(materialId).orElseThrow().getCurrentStock());
        assertEquals(1, receipts.count());
        assertEquals(1, jdbc.queryForObject("select count(*) from goods_receipt_items", Integer.class));
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, orders.findById(orderId).orElseThrow().getStatus());
    }
    @Test void repeatWithDifferentNumberIsRejectedWithoutStockChange() {
        service.receiveGoods(orderId, "GR-1");
        assertThrows(ReceiptConflictException.class, () -> service.receiveGoods(orderId, "GR-2"));
        assertReceivedOnce();
    }
    @Test void simultaneousReceiptsForSameOrderOnlyCommitOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> attempt("GR-A", ready, start));
            Future<Boolean> second = pool.submit(() -> attempt("GR-B", ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertReceivedOnce();
        } finally { start.countDown(); pool.shutdownNow(); }
    }
    boolean attempt(String number, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start gate timed out");
        try { service.receiveGoods(orderId, number); return true; }
        catch (ReceiptConflictException expected) { return false; }
    }
    @Test void failedReceiptInsertRollsBackStockAndOrderThenAllowsRetry() {
        // A null mandatory receipt number fails when persisting, after stock was changed in memory.
        assertThrows(RuntimeException.class, () -> service.receiveGoods(orderId, null));
        assertEquals(3, materials.findById(materialId).orElseThrow().getCurrentStock());
        assertEquals(0, receipts.count());
        assertEquals(PurchaseOrderStatus.CREATED, orders.findById(orderId).orElseThrow().getStatus());
        service.receiveGoods(orderId, "GR-RETRY");
        assertReceivedOnce();
    }
    @Test void existingReceiptStillBlocksAfterManualStatusReset() {
        service.receiveGoods(orderId, "GR-1");
        jdbc.update("update purchase_orders set status = 'CREATED' where id = ?", orderId);
        assertThrows(ReceiptConflictException.class, () -> service.receiveGoods(orderId, "GR-2"));
        assertEquals(8, materials.findById(materialId).orElseThrow().getCurrentStock());
        assertEquals(1, receipts.count());
    }
    @Test void duplicateOrderReceiptReturnsHttpConflict() throws Exception {
        service.receiveGoods(orderId, "GR-1");
        mvc.perform(post("/api/goods-receipts").param("purchaseOrderId", orderId.toString())
            .param("receiptNumber", "GR-2")).andExpect(status().isConflict());
        assertReceivedOnce();
    }
    @Test void sentToVendorOrderCanBeReceived() {
        jdbc.update("update purchase_orders set status = 'SENT_TO_VENDOR' where id = ?", orderId);
        service.receiveGoods(orderId, "GR-1");
        assertReceivedOnce();
    }
}
