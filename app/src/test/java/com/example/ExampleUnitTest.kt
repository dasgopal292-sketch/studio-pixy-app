package com.example

import com.example.data.model.CustomerOrder
import com.example.data.model.DefaultStudioCatalog
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioService
import com.example.ui.viewmodel.OrderDraft
import com.example.util.formatInr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun priceCalculator_computesTotalCorrectly() {
    val draft = OrderDraft(
      serviceId = "print_4x6",
      serviceName = "4x6 Photo Print",
      category = ServiceCategory.PRINT,
      quantity = 15L,
      unitPrice = 20L,
    )
    assertEquals(300L, draft.totalAmount)
    assertEquals("₹300", formatInr(draft.totalAmount))
  }

  @Test
  fun firestorePayloads_omitDocumentIdAndEnforcePendingStatus() {
    val service = StudioService(
      id = "print_passport",
      name = "Passport Size Photo",
      category = ServiceCategory.PRINT.name,
      size = "35x45 mm",
      price = 80L,
      unitLabel = "per sheet",
    )
    val serviceMap = service.toCreateMap()
    assertFalse(serviceMap.containsKey("id"))
    assertTrue(serviceMap.containsKey("createdAt"))
    assertTrue(serviceMap.containsKey("updatedAt"))

    val order = CustomerOrder(
      id = "order_abc",
      orderCode = "PX-100001",
      serviceId = "print_passport",
      serviceName = "Passport Size Photo",
      quantity = 3L,
      unitPrice = 80L,
      totalAmount = 999L, // Should be recomputed cleanly
      customerName = "Riya Sen",
      customerPhone = "9876543210",
      status = OrderStatus.COMPLETED.name, // Must normalize to PENDING on create
    )
    val orderMap = order.toCreateMap()
    assertFalse(orderMap.containsKey("id"))
    assertEquals(240L, orderMap["totalAmount"])
    assertEquals(OrderStatus.PENDING.name, orderMap["status"])
    assertEquals("", orderMap["adminRemarks"])
  }

  @Test
  fun defaultCatalog_includesPrintsFramesAndPackages() {
    val categories = DefaultStudioCatalog.services.map { it.categoryEnum() }.toSet()
    assertEquals(
      setOf(ServiceCategory.PRINT, ServiceCategory.FRAME, ServiceCategory.PACKAGE),
      categories,
    )
  }
}
