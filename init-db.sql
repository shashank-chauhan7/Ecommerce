-- Initialize multiple databases for different services
CREATE DATABASE ecommerce_users;
CREATE DATABASE ecommerce_products;
CREATE DATABASE ecommerce_orders;
CREATE DATABASE ecommerce_payments;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ecommerce_auth TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_users TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_products TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_orders TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_payments TO ecommerce;
