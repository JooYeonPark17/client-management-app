-- Members
CREATE INDEX idx_members_phone_number ON members(phone_number);
CREATE INDEX idx_members_user_id ON members(user_id);

-- Products
CREATE INDEX idx_products_name_status ON products(name, status);

-- Orders
CREATE INDEX idx_orders_member_created ON orders(member_id, created_at DESC);
CREATE INDEX idx_orders_idempotency_key ON orders(idempotency_key);
CREATE INDEX idx_orders_status ON orders(status);

-- Order Items
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Payments
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_status_created ON payments(payment_status, created_at DESC);

-- Idempotency Records
CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);
