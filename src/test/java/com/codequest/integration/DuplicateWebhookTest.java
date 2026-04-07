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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests idempotency of the webhook endpoint.
 *
 * <p>The payment provider may send the same event more than once.
 * The system must handle this gracefully: the second (and any subsequent)
 * delivery of the same webhook must not corrupt the order state or throw an error.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DuplicateWebhookTest {

    @Autowired MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateConfirmedWebhook_isIdempotent() throws Exception {
        String paymentIntentId = setupPendingPayment();

        // First webhook delivery → order moves to PAID
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"CONFIRMED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));

        // Second webhook delivery (duplicate) → still PAID, no error
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"CONFIRMED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    void duplicateFailedWebhook_isIdempotent() throws Exception {
        String paymentIntentId = setupPendingPayment();

        // First webhook delivery → order moves to PAYMENT_FAILED
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"FAILED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAYMENT_FAILED")));

        // Duplicate delivery → still PAYMENT_FAILED, no error
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"FAILED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAYMENT_FAILED")));
    }

    @Test
    void contradictingWebhooks_secondIsIgnored() throws Exception {
        String paymentIntentId = setupPendingPayment();

        // First webhook confirms
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"CONFIRMED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));

        // Second webhook tries to fail (contradicting) — must be ignored, order stays PAID
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","result":"FAILED"}
                                """.formatted(paymentIntentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    private String setupPendingPayment() throws Exception {
        // Create cart
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andReturn();
        String cartId = jsonField(cartResult, "id");

        // Add item
        mockMvc.perform(post("/carts/{id}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":"product-1","quantity":1,"price":10.00}
                        """));

        // Checkout
        MvcResult checkoutResult = mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andReturn();
        String orderId = jsonField(checkoutResult, "id");

        // Start payment
        MvcResult paymentResult = mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andReturn();
        return jsonField(paymentResult, "id");
    }

    @SuppressWarnings("unchecked")
    private String jsonField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        return (String) map.get(field);
    }
}
