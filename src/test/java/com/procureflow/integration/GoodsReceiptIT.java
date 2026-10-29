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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Testcontainers
class GoodsReceiptIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--innodb-lock-wait-timeout=10");
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
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MaterialService materialService;
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

    @Test void differentOrdersReceivingSameMaterialKeepBothIncrements() throws Exception {
        Long secondOrder = extraOrder("2", List.of(materialId), List.of(7));
        concurrently(() -> service.receiveGoods(orderId, "GR-A"),
                () -> service.receiveGoods(secondOrder, "GR-B"));
        assertStock(materialId, 15); // initial 3 + first receipt 5 + second receipt 7
        assertTwoReceipts(secondOrder, 2);
    }

    @Test void overlappingTransactionsRefreshPreviouslyLoadedStock() throws Exception {
        Long secondOrder = extraOrder("2", List.of(materialId), List.of(7));
        CountDownLatch snapshotsLoaded = new CountDownLatch(2);
        CountDownLatch firstMayReceive = new CountDownLatch(1);
        CountDownLatch secondMayReceive = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> transaction().executeWithoutResult(status -> {
                assertStock(materialId, 3); // managed entity + MySQL repeatable-read snapshot
                snapshotsLoaded.countDown(); await(firstMayReceive);
                service.receiveGoods(orderId, "GR-A");
            }));
            Future<?> second = pool.submit(() -> transaction().executeWithoutResult(status -> {
                assertStock(materialId, 3);
                snapshotsLoaded.countDown(); await(secondMayReceive);
                service.receiveGoods(secondOrder, "GR-B");
            }));
            assertTrue(snapshotsLoaded.await(10, TimeUnit.SECONDS));
            firstMayReceive.countDown(); first.get(30, TimeUnit.SECONDS);
            // The second transaction is already open with stock=3 cached.
            // It must use the committed 8, not silently overwrite it with 3+7.
            secondMayReceive.countDown(); second.get(30, TimeUnit.SECONDS);
            assertStock(materialId, 15);
            assertTwoReceipts(secondOrder, 2);
        } finally {
            firstMayReceive.countDown(); secondMayReceive.countDown(); stop(pool);
        }
    }

    @Test void oppositeMaterialLineOrderCompletesWithoutLosingStock() throws Exception {
        Long otherMaterial = extraMaterial("MAT-2", 11);
        // Two completely different orders list the same materials in reverse order.
        Long left = extraOrder("LEFT", List.of(materialId, otherMaterial), List.of(5, 2));
        Long right = extraOrder("RIGHT", List.of(otherMaterial, materialId), List.of(4, 7));
        concurrently(() -> service.receiveGoods(left, "GR-L"),
                () -> service.receiveGoods(right, "GR-R"));
        assertStock(materialId, 15); assertStock(otherMaterial, 17);
        assertEquals(2, receipts.count());
        assertEquals(4, jdbc.queryForObject("select count(*) from goods_receipt_items", Integer.class));
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, orders.findById(left).orElseThrow().getStatus());
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, orders.findById(right).orElseThrow().getStatus());
    }

    @Test void repeatedMaterialLinesAccumulateAndFailureRollsBackEveryMaterial() {
        Long otherMaterial = extraMaterial("MAT-2", 11);
        Long repeated = extraOrder("REPEAT", List.of(materialId, otherMaterial, materialId), List.of(2, 4, 5));
        assertThrows(RuntimeException.class, () -> service.receiveGoods(repeated, null));
        assertStock(materialId, 3); assertStock(otherMaterial, 11);
        assertEquals(0, receipts.count());
        assertEquals(0, jdbc.queryForObject("select count(*) from goods_receipt_items", Integer.class));
        assertEquals(PurchaseOrderStatus.CREATED, orders.findById(repeated).orElseThrow().getStatus());
        service.receiveGoods(repeated, "GR-RETRY");
        assertStock(materialId, 10); assertStock(otherMaterial, 15);
        assertEquals(1, receipts.count());
        assertEquals(3, jdbc.queryForObject("select count(*) from goods_receipt_items", Integer.class));
    }

    @Test void receiptRefreshesStockAfterDirectMaterialUpdateCommits() throws Exception {
        CountDownLatch cached = new CountDownLatch(1), edited = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> receipt = pool.submit(() -> transaction().executeWithoutResult(status -> {
                assertStock(materialId, 3); cached.countDown(); await(edited);
                service.receiveGoods(orderId, "GR-A");
            }));
            assertTrue(cached.await(10, TimeUnit.SECONDS));
            Material update = materials.findById(materialId).orElseThrow();
            update.setCurrentStock(20);
            materialService.updateMaterial(materialId, update);
            edited.countDown(); receipt.get(30, TimeUnit.SECONDS);
            assertStock(materialId, 25);
            assertEquals(1, receipts.count());
        } finally { edited.countDown(); stop(pool); }
    }

    @Test void newMaterialCannotMergeAnExistingMaterialId() {
        Material existing = materials.findById(materialId).orElseThrow();
        existing.setMaterialCode("CHANGED-CODE"); existing.setCurrentStock(99);
        assertThrows(IllegalArgumentException.class, () -> materialService.createMaterial(existing));
        assertStock(materialId, 3);
        assertEquals("MAT-1", materials.findById(materialId).orElseThrow().getMaterialCode());
    }

    void assertStock(Long id, int expected) {
        assertEquals(expected, materials.findById(id).orElseThrow().getCurrentStock());
    }

    void assertTwoReceipts(Long secondOrder, int itemCount) {
        assertEquals(2, receipts.count());
        assertEquals(itemCount, jdbc.queryForObject("select count(*) from goods_receipt_items", Integer.class));
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, orders.findById(orderId).orElseThrow().getStatus());
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, orders.findById(secondOrder).orElseThrow().getStatus());
    }

    Long extraMaterial(String code, int stock) {
        Material material = new Material();
        material.setMaterialCode(code); material.setMaterialName(code);
        material.setUnitOfMeasure("PCS"); material.setUnitPrice(BigDecimal.TEN);
        material.setCurrentStock(stock); material.setReorderLevel(1); material.setActive(true);
        return materials.save(material).getId();
    }

    Long extraOrder(String suffix, List<Long> ids, List<Integer> quantities) {
        PurchaseRequisition req = new PurchaseRequisition();
        req.setRequisitionNumber("REQ-" + suffix); req.setRequestedBy("Demo");
        req.setDepartment("Operations"); req.setRequestedDate(LocalDateTime.now());
        req.setStatus(RequisitionStatus.CONVERTED_TO_PO); req = requisitions.save(req);
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseOrderNumber("PO-" + suffix); order.setVendor(vendors.findAll().get(0));
        order.setRequisition(req); order.setOrderDate(LocalDateTime.now());
        order.setStatus(PurchaseOrderStatus.CREATED);
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(order); item.setMaterial(materials.findById(ids.get(i)).orElseThrow());
            item.setQuantity(quantities.get(i)); item.setUnitPrice(BigDecimal.TEN); items.add(item);
        }
        order.setItems(items);
        order.setTotalAmount(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantities.stream().mapToInt(Integer::intValue).sum())));
        return orders.save(order).getId();
    }

    TransactionTemplate transaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(25);
        return template;
    }

    void concurrently(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> left = pool.submit(() -> { ready.countDown(); await(start); first.run(); });
            Future<?> right = pool.submit(() -> { ready.countDown(); await(start); second.run(); });
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            left.get(30, TimeUnit.SECONDS); right.get(30, TimeUnit.SECONDS);
        } finally { start.countDown(); stop(pool); }
    }

    static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(15, TimeUnit.SECONDS), "Timed out waiting for concurrent transaction"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    static void stop(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "Concurrent tasks did not terminate");
    }
}
