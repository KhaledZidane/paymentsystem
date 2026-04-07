package com.codequest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: full end-to-end happy path through the cart, checkout, and payment flows.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CartCheckoutFlowTest {

    @Autowired MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void happyPath_cartToPayment() throws Exception {
        // Step 1: Create cart
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        String cartId = jsonField(cartResult, "id");

        // Step 2: Add item
        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"product-1","quantity":2,"price":19.99}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value("product-1"));

        // Step 3: Checkout → Order CREATED
        MvcResult checkoutResult = mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.cartId").value(cartId))
                .andExpect(jsonPath("$.total").value(39.98))
                .andReturn();

        String orderId = jsonField(checkoutResult, "id");

        // Step 4: Start payment → Order PENDING_PAYMENT
        MvcResult paymentResult = mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andReturn();

        String paymentIntentId = jsonField(paymentResult, "id");

        // Verify order is now PENDING_PAYMENT
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // Step 5: Mock provider confirms payment → Order PAID
        mockMvc.perform(post("/mock/payment/{id}/confirm", paymentIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Final state check
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void paymentFailureAndRetry_eventuallyPaid() throws Exception {
        String cartId = createCartWithItem();

        MvcResult checkoutResult = mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = jsonField(checkoutResult, "id");

        // First payment attempt
        MvcResult firstPayment = mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        String firstIntentId = jsonField(firstPayment, "id");

        // Provider reports failure
        mockMvc.perform(post("/mock/payment/{id}/fail", firstIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        // Retry payment
        MvcResult retryPayment = mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isCreated())
                .andReturn();
        String retryIntentId = jsonField(retryPayment, "id");

        // Provider confirms
        mockMvc.perform(post("/mock/payment/{id}/confirm", retryIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void addSameProductTwice_mergesQuantity() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();
        String cartId = jsonField(cartResult, "id");

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"productId":"product-X","quantity":1,"price":5.00}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"productId":"product-X","quantity":3,"price":5.00}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void startPayment_whenAlreadyPending_returns409() throws Exception {
        String cartId = createCartWithItem();
        MvcResult checkout = mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andReturn();
        String orderId = jsonField(checkout, "id");

        // First payment starts successfully
        mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isCreated());

        // Second attempt while first is still pending → 409
        mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("active payment")));
    }

    @Test
    void addItem_afterCheckout_returns409() throws Exception {
        String cartId = createCartWithItem();
        mockMvc.perform(post("/carts/{id}/checkout", cartId));

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"product-2","quantity":1,"price":9.99}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void checkout_emptyCart_returns422() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();
        String cartId = jsonField(cartResult, "id");

        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isUnprocessableEntity());
    }

    private String createCartWithItem() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();
        String cartId = jsonField(cartResult, "id");

        mockMvc.perform(post("/carts/{id}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":"product-1","quantity":1,"price":25.00}
                        """));

        return cartId;
    }

    @SuppressWarnings("unchecked")
    private String jsonField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        return (String) map.get(field);
    }
}

