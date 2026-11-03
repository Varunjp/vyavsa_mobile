package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.SaleCalculationUtils
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.SaleConfirmation
import com.vyavsa.app.core.printer.InvoiceDataMapper
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticContraAdjustmentTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testLineSaleSerializationWithContraFields() {
        val payload = """
            {
                "id": "sale-uuid-1",
                "customer_id": "cust-uuid-1",
                "customer_name": "Agro Supplies Ltd",
                "total_amount": "25000.00",
                "total_cash_in": "0.00",
                "bank_amount": "0.00",
                "collected_amount": "0.00",
                "contra_amount": "15000.00",
                "previous_payable": "15000.00",
                "balance": "10000.00",
                "updated_balance": "10000.00"
            }
        """.trimIndent()

        val sale = json.decodeFromString<LineSale>(payload)
        assertEquals("sale-uuid-1", sale.id)
        assertEquals("15000.00", sale.contraAmount)
        assertEquals("15000.00", sale.previousPayable)
        assertEquals("10000.00", sale.balance)
        assertEquals("25000.00", sale.totalAmount)
    }

    @Test
    fun testLineSaleSerializationHistoricalWithoutContraFields() {
        val payload = """
            {
                "id": "legacy-sale-uuid",
                "customer_id": "cust-uuid-2",
                "customer_name": "Legacy Customer",
                "total_amount": "5000.00",
                "total_cash_in": "5000.00"
            }
        """.trimIndent()

        val sale = json.decodeFromString<LineSale>(payload)
        assertEquals("legacy-sale-uuid", sale.id)
        assertEquals("0.00", sale.contraAmount)
        assertEquals("0.00", sale.previousPayable)
    }

    @Test
    fun testCounterSaleSerializationWithContraFields() {
        val payload = """
            {
                "id": "counter-uuid-1",
                "item": "Feed Sacks",
                "total_amount": "20000.00",
                "cash": "0.00",
                "bank_amount": "0.00",
                "contra_amount": "20000.00",
                "previous_payable": "50000.00",
                "updated_balance": "0.00"
            }
        """.trimIndent()

        val sale = json.decodeFromString<CounterSale>(payload)
        assertEquals("counter-uuid-1", sale.id)
        assertEquals("20000.00", sale.contraAmount)
        assertEquals("50000.00", sale.previousPayable)
    }

    @Test
    fun testUnifiedPartyCustomerAndBalanceSerialization() {
        val customerPayload = """
            {
                "id": "party-123",
                "customer_name": "Unified Enterprise",
                "role": "both",
                "is_customer": true,
                "is_supplier": true,
                "total_receivable": "25000.00",
                "total_payable": "15000.00",
                "net_balance": "10000.00",
                "net_status": "owes_us"
            }
        """.trimIndent()

        val customer = json.decodeFromString<Customer>(customerPayload)
        assertEquals("both", customer.role)
        assertTrue(customer.isCustomer)
        assertTrue(customer.isSupplier)
        assertEquals("25000.00", customer.totalReceivable)
        assertEquals("15000.00", customer.totalPayable)
        assertEquals("10000.00", customer.netBalance)
        assertEquals("owes_us", customer.netStatus)

        val balancePayload = """
            {
                "customer_id": "party-123",
                "current_balance": "25000.00",
                "advance_balance": "0.00",
                "outstanding_payable": "15000.00",
                "total_receivable": "25000.00",
                "total_payable": "15000.00",
                "net_balance": "10000.00",
                "net_status": "owes_us",
                "role": "both",
                "is_customer": true,
                "is_supplier": true
            }
        """.trimIndent()

        val balance = json.decodeFromString<CustomerBalance>(balancePayload)
        assertEquals("15000.00", balance.outstandingPayable)
        assertEquals("both", balance.role)
        assertTrue(balance.isSupplier)
    }

    @Test
    fun testInvoiceDataMapperFromLineSaleWithContraAdjustment() {
        val lineSale = LineSale(
            id = "sale-101",
            invoiceNumber = "INV-101",
            customerName = "Sri Agro Trader",
            totalAmount = "25000.00",
            totalCashIn = "0.00",
            bankAmount = "0.00",
            contraAmount = "15000.00",
            previousPayable = "15000.00",
            balance = "10000.00"
        )

        val printData = InvoiceDataMapper.fromLineSale(lineSale)
        assertEquals(15000.00, printData.contraAmount, 0.001)
        assertEquals(15000.00, printData.previousPayable, 0.001)
        assertEquals(0.00, printData.remainingPayable, 0.001)
        assertEquals(10000.00, printData.balanceDue, 0.001)
        assertEquals(15000.00, printData.totalSettledForSale, 0.001)
    }

    @Test
    fun testInvoiceDataMapperFromCounterSaleWithContraAdjustment() {
        val counterSale = CounterSale(
            id = "cnt-202",
            invoiceNumber = "POS-202",
            customerName = "Farm Producer",
            totalAmount = "20000.00",
            cash = "0.00",
            bankAmount = "0.00",
            contraAmount = "20000.00",
            previousPayable = "50000.00",
            amountAllocatedToSale = "0.00"
        )

        val printData = InvoiceDataMapper.fromCounterSale(counterSale)
        assertEquals(20000.00, printData.contraAmount, 0.001)
        assertEquals(50000.00, printData.previousPayable, 0.001)
        assertEquals(30000.00, printData.remainingPayable, 0.001)
        assertEquals(0.00, printData.balanceDue, 0.001)
        assertEquals(20000.00, printData.totalSettledForSale, 0.001)
    }

    @Test
    fun testConsecutiveSalesContraAdjustmentScenario() {
        // Initial state: Supplier Payable = ₹25,000, Customer Due = ₹0
        val payableInitial = "25000.00"

        // Sale 1: ₹10,000 sale, ₹0 payment
        val sale1Preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "10000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = payableInitial
        )
        assertEquals("10000.00", sale1Preview.contraAdjustmentString)
        assertEquals("15000.00", sale1Preview.remainingSupplierPayableString)
        assertEquals("0.00", sale1Preview.remainingSaleDueString)
        assertEquals("0.00", sale1Preview.updatedCustomerDueString)

        // Sale 2: ₹20,000 sale against remaining payable of ₹15,000
        val payableAfterSale1 = sale1Preview.remainingSupplierPayableString
        val sale2Preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "20000.00",
            customerCurrentBalanceStr = sale1Preview.updatedCustomerDueString,
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = payableAfterSale1
        )
        assertEquals("15000.00", sale2Preview.contraAdjustmentString)
        assertEquals("0.00", sale2Preview.remainingSupplierPayableString)
        assertEquals("5000.00", sale2Preview.remainingSaleDueString)
        assertEquals("5000.00", sale2Preview.updatedCustomerDueString)
    }

    @Test
    fun testContraAdjustmentWithExistingReceivableBalance() {
        // Party owes business ₹5,000 previous receivable
        // Business owes party ₹8,000 supplier payable
        // New sale: ₹10,000 with ₹0 direct payment
        // Contra offsets ₹8,000 against new sale
        // Remaining sale due: ₹2,000
        // Total customer outstanding due = ₹5,000 (prev) + ₹2,000 (sale) = ₹7,000
        // Remaining supplier payable = ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "10000.00",
            customerCurrentBalanceStr = "5000.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "8000.00"
        )
        assertEquals("8000.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSupplierPayableString)
        assertEquals("2000.00", preview.remainingSaleDueString)
        assertEquals("7000.00", preview.updatedCustomerDueString)
    }

    @Test
    fun testSaleConfirmationDataIntegrity() {
        val conf = SaleConfirmation(
            saleId = "sale-xyz",
            invoiceNumber = "INV-2026-0099",
            partyName = "Rao Fertilizers & Chemicals",
            saleAmount = 25000.0,
            contraAdjustment = 15000.0,
            previousSupplierPayable = 15000.0,
            remainingSupplierPayable = 0.0,
            remainingCustomerReceivable = 10000.0,
            actualAmountReceived = 0.0,
            advanceApplied = 0.0,
            isLineSale = true
        )
        assertEquals(25000.0, conf.saleAmount, 0.001)
        assertEquals(15000.0, conf.contraAdjustment, 0.001)
        assertEquals(0.0, conf.remainingSupplierPayable, 0.001)
        assertEquals(10000.0, conf.remainingCustomerReceivable, 0.001)
        assertEquals(0.0, conf.actualAmountReceived, 0.001)
        assertTrue(conf.isLineSale)
    }
}
