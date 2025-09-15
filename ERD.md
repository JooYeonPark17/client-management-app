# Client Management App - Entity Relationship Diagram (ERD)

## Database Structure

```mermaid
erDiagram
    MEMBERS {
        bigserial id PK
        varchar user_id UK "NOT NULL"
        varchar name "NOT NULL"
        varchar phone_number "NOT NULL"
        varchar gender "NOT NULL"
        varchar birth_date "NOT NULL"
        varchar member_status "NOT NULL DEFAULT 'ACTIVE'"
        timestamp withdrawn_at "NULL"
        bigint version "NOT NULL DEFAULT 0"
        timestamp created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    PRODUCTS {
        bigserial id PK
        varchar name "NOT NULL"
        decimal price "NOT NULL"
        varchar description
        varchar status "NOT NULL DEFAULT 'AVAILABLE'"
        timestamp created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    ORDERS {
        bigserial id PK
        bigint member_id FK "NOT NULL"
        decimal total_amount "NOT NULL"
        varchar status "NOT NULL DEFAULT 'PENDING'"
        varchar idempotency_key UK "NOT NULL"
        bigint payment_id FK "NULL"
        bigint version "NOT NULL DEFAULT 0"
        timestamp created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    ORDER_ITEMS {
        bigserial id PK
        bigint order_id FK "NOT NULL"
        bigint product_id FK "NOT NULL"
        integer quantity "NOT NULL CHECK > 0"
        decimal unit_price "NOT NULL"
        decimal total_price "NOT NULL"
    }

    PAYMENTS {
        bigserial id PK
        bigint order_id FK UK "NOT NULL"
        decimal amount "NOT NULL"
        varchar payment_status "NOT NULL DEFAULT 'PENDING'"
        varchar payment_method
        varchar transaction_id
        timestamp created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    IDEMPOTENCY_RECORDS {
        bigserial id PK
        varchar idempotency_key UK "NOT NULL"
        varchar resource_id "NOT NULL"
        varchar resource_type "NOT NULL"
        timestamp created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp expires_at "NOT NULL"
    }

    %% Relationships
    MEMBERS ||--o{ ORDERS : "1:N (member_id)"
    ORDERS ||--o{ ORDER_ITEMS : "1:N (order_id)"
    PRODUCTS ||--o{ ORDER_ITEMS : "1:N (product_id)"
    ORDERS ||--o| PAYMENTS : "1:1 (order_id)"
    ORDERS ||--o| PAYMENTS : "1:1 (payment_id)"
```

## Entity Descriptions

### MEMBERS
- **Primary Key**: `id` (BIGSERIAL)
- **Unique Key**: `user_id` (VARCHAR)
- **Business Logic**:
  - Stores user account information
  - Supports withdrawal process with `withdrawn_at` timestamp
  - Member status: ACTIVE, WITHDRAWN, PENDING_WITHDRAWAL
  - Optimistic locking with `version` field

### PRODUCTS
- **Primary Key**: `id` (BIGSERIAL)
- **Business Logic**:
  - Product catalog with name, price, description
  - Status: AVAILABLE, UNAVAILABLE
  - Pricing stored as DECIMAL(10,2)

### ORDERS
- **Primary Key**: `id` (BIGSERIAL)
- **Foreign Keys**:
  - `member_id` → MEMBERS.id
  - `payment_id` → PAYMENTS.id (nullable)
- **Unique Key**: `idempotency_key` (prevents duplicate orders)
- **Business Logic**:
  - Order status: PENDING, PAID, CANCELLED
  - Bidirectional relationship with payments
  - Optimistic locking with `version` field

### ORDER_ITEMS
- **Primary Key**: `id` (BIGSERIAL)
- **Foreign Keys**:
  - `order_id` → ORDERS.id (CASCADE DELETE)
  - `product_id` → PRODUCTS.id
- **Business Logic**:
  - Line items for each order
  - Stores quantity, unit price, and total price
  - Quantity must be > 0

### PAYMENTS
- **Primary Key**: `id` (BIGSERIAL)
- **Foreign Key**: `order_id` → ORDERS.id (UNIQUE - 1:1 relationship)
- **Business Logic**:
  - Payment status: PENDING, SUCCESS, FAILED, CANCELLED
  - Stores payment method and transaction ID
  - One-to-one relationship with orders

### IDEMPOTENCY_RECORDS
- **Primary Key**: `id` (BIGSERIAL)
- **Unique Key**: `idempotency_key`
- **Business Logic**:
  - Tracks idempotent operations
  - Prevents duplicate resource creation
  - Auto-expires based on `expires_at`

## Key Relationships

1. **MEMBERS → ORDERS**: One-to-Many
   - A member can have multiple orders
   - `orders.member_id` references `members.id`

2. **ORDERS → ORDER_ITEMS**: One-to-Many (Cascade Delete)
   - An order contains multiple line items
   - `order_items.order_id` references `orders.id`
   - Items are deleted when order is deleted

3. **PRODUCTS → ORDER_ITEMS**: One-to-Many
   - A product can appear in multiple order items
   - `order_items.product_id` references `products.id`

4. **ORDERS ↔ PAYMENTS**: Bidirectional One-to-One
   - Each order can have one payment
   - Each payment belongs to one order
   - `payments.order_id` references `orders.id` (unique)
   - `orders.payment_id` references `payments.id` (nullable)

## Constraints Summary

- **Check Constraints**:
  - `members.member_status` ∈ {ACTIVE, WITHDRAWN, PENDING_WITHDRAWAL}
  - `products.status` ∈ {AVAILABLE, UNAVAILABLE}
  - `orders.status` ∈ {PENDING, PAID, CANCELLED}
  - `payments.payment_status` ∈ {PENDING, SUCCESS, FAILED, CANCELLED}
  - `order_items.quantity` > 0

- **Unique Constraints**:
  - `members.user_id`
  - `orders.idempotency_key`
  - `payments.order_id`
  - `idempotency_records.idempotency_key`

- **Versioning**:
  - `members.version` and `orders.version` for optimistic locking