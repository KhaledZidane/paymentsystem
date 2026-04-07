# Cart, Checkout & Mock Payment System

A Spring Boot REST API covering cart management, order lifecycle, and mock payment processing. Built as a single in-memory service with no external dependencies.

---

## Requirements

- Java 17+
- Maven 4.0.5+

Or just Docker (see below).

---

## Starting the App

### With Maven

```bash
cd paymentsystem
mvn spring-boot:run
```

The server starts on **port 8080** by default. To use a different port:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments='-DPORT=9090'
```

### With Docker

```bash
docker build -t cart-checkout .
docker run -p 8080:8080 cart-checkout
```

### Running Tests

```bash
mvn test
```

37 tests across unit and integration suites. All should pass.

---

## API Reference

Base URL: `http://localhost:8080`

### Cart

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/carts` | Create a new cart |
| `GET` | `/carts/{cartId}` | Get cart details |
| `POST` | `/carts/{cartId}/items` | Add or merge an item |
| `DELETE` | `/carts/{cartId}/items/{productId}` | Remove an item |
| `POST` | `/carts/{cartId}/checkout` | Checkout → creates an Order |

**Add item body:**
```json
{
  "productId": "product-1",
  "quantity": 2,
  "price": 19.99
}
```

Adding the same `productId` twice merges the quantities.

---

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/orders/{orderId}` | Get order details and current status |
| `GET` | `/orders/{orderId}/history` | Get full state transition audit trail |
| `POST` | `/orders/{orderId}/payment/start` | Start a payment attempt |
| `POST` | `/orders/{orderId}/cancel` | Cancel the order |

---

### Mock Payment Provider

Simulates the external payment provider confirming or failing a payment.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/mock/payment/{paymentIntentId}/confirm` | Mark payment as confirmed |
| `POST` | `/mock/payment/{paymentIntentId}/fail` | Mark payment as failed |
| `POST` | `/mock-provider/webhook` | Send a raw webhook event |

**Webhook body:**
```json
{
  "paymentIntentId": "<id>",
  "result": "CONFIRMED"
}
```

`result` accepts `CONFIRMED` or `FAILED`.

---

## Order State Machine

```
CREATED ──────────────────────────────── CANCELLED (terminal)
   │                                         ▲
   ▼ (payment/start)                         │
PENDING_PAYMENT                              │
   │              │                          │
   ▼              ▼                          │
  PAID      PAYMENT_FAILED ─────────────────┘
(terminal)       │
                 └──► PENDING_PAYMENT (retry)
```

| From | To | Trigger |
|------|----|---------|
| `CREATED` | `PENDING_PAYMENT` | `POST /payment/start` |
| `CREATED` | `CANCELLED` | `POST /cancel` |
| `PENDING_PAYMENT` | `PAID` | Webhook `CONFIRMED` |
| `PENDING_PAYMENT` | `PAYMENT_FAILED` | Webhook `FAILED` |
| `PAYMENT_FAILED` | `PENDING_PAYMENT` | `POST /payment/start` (retry) |
| `PAYMENT_FAILED` | `CANCELLED` | `POST /cancel` |

`PAID` and `CANCELLED` are terminal — no further transitions allowed.

---

## Postman Collection

A ready-made Postman collection is included at `paymentsystem/CartCheckout.postman_collection.json`. It contains 25 pre-built requests across four folders:

- **Happy path** — full flow from cart to confirmed payment
- **Failure & retry** — payment fails then succeeds on retry
- **Duplicate webhook** — idempotency verification
- **Guard rails** — error cases (empty cart checkout, duplicate payment, invalid transitions)

Import it directly into Postman via **File → Import**.

---

## Example: Happy Path

```bash
# 1. Create a cart
curl -X POST http://localhost:8080/carts

# 2. Add an item
curl -X POST http://localhost:8080/carts/{cartId}/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"product-1","quantity":2,"price":19.99}'

# 3. Checkout
curl -X POST http://localhost:8080/carts/{cartId}/checkout

# 4. Start payment
curl -X POST http://localhost:8080/orders/{orderId}/payment/start

# 5. Confirm payment (mock provider)
curl -X POST http://localhost:8080/mock/payment/{paymentIntentId}/confirm

# 6. Verify order is PAID
curl http://localhost:8080/orders/{orderId}

# 7. View full history
curl http://localhost:8080/orders/{orderId}/history
```

---

## Error Responses

All errors return a JSON body:

```json
{
  "status": 409,
  "message": "Order already has an active payment in flight"
}
```

| Status | Meaning |
|--------|---------|
| `404` | Cart or order not found |
| `409` | Illegal state transition or duplicate active payment |
| `422` | Checkout attempted on an empty cart |
