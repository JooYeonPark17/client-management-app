# 클라이언트 관리 애플리케이션
JWT 인증, 주문 처리, 결제 연동, 실시간 재고 관리를 포함한 종합적인 Spring Boot 애플리케이션.

## 주요 기능

### 핵심 기능
- 회원 관리: 회원가입, 인증, 탈퇴/재활성화
- 상품 카탈로그: 실시간 재고 추적이 포함된 재고 관리
- 주문 처리: 결제 연동이 포함된 주문 전체 라이프사이클
- 결제 게이트웨이: 현실적인 실패 시나리오가 포함된 모의 결제 서비스
- 보안: JWT 기반 인증과 역할 기반 접근 제어
- API 문서화: 인터랙티브 Swagger/OpenAPI 문서 제공

### 주요 기술
- Idempotency: 주요 작업의 중복 요청 방지
- 분산 락: 재고 관리 시 동시 접근 보호
- 서킷 브레이커: 자동 장애 조치를 통한 결제 게이트웨이 복원력 확보
- 재시도 메커니즘: 일시적 오류에 대한 지수 백오프
- Rate Limiting: API 남용 방지


## 구조
```
src/main/resources/
├── application.yml              # Base configuration
├── application-dev.yml          # Development settings
├── application-test.yml         # Test environment
├── application-staging.yml      # Staging environment
├── application-prod.yml         # Production settings
├── messages/
│   ├── messages_en.properties   # English messages
│   └── messages_ko.properties   # Korean messages
└── db/
    ├── migration/
    │   ├── V1__Create_tables.sql
    │   ├── V2__Add_indexes.sql
    │   └── V3__Insert_sample_data.sql
    └── testdata/
        └── sample_data.sql
```

### 3. Build and Run

```bash
# Build the application
./gradlew clean build

# Run with development profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Or run the JAR directly
java -jar build/libs/client-management-app-1.0.0.jar --spring.profiles.active=dev
```

### 4. Verify Installation

```bash
# Check application health
curl http://localhost:8080/api/health

# Access Swagger UI
open http://localhost:8080/swagger-ui.html

# Access H2 Console (dev profile only)
open http://localhost:8080/h2-console
```

## 📚 API 문서

### Interactive Documentation
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs


### Key Endpoints

#### Authentication
```bash
# Register new member
POST /api/members/register
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}

# Login
POST /api/auth/login
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123"
}

# Refresh token
POST /api/auth/refresh
Content-Type: application/json
{
  "refreshToken": "your-refresh-token"
}
```

#### Product Management
```bash
# Get available products
GET /api/products?page=0&size=20
Authorization: Bearer your-access-token

# Search products
GET /api/products/search?name=product&page=0&size=10
Authorization: Bearer your-access-token

# Get product by ID
GET /api/products/1
Authorization: Bearer your-access-token
```

#### Order Processing
```bash
# Create order
POST /api/orders
Authorization: Bearer your-access-token
Content-Type: application/json
{
  "idempotencyKey": "order-2024-001",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}

# Get order history
GET /api/orders?page=0&size=10
Authorization: Bearer your-access-token

# Cancel order
DELETE /api/orders/1
Authorization: Bearer your-access-token
```

#### Member Management
```bash
# Get member profile
GET /api/members/me
Authorization: Bearer your-access-token

# Request withdrawal
DELETE /api/members/withdraw
Authorization: Bearer your-access-token

# Cancel withdrawal
POST /api/members/cancel-withdrawal
Authorization: Bearer your-access-token
```

## 🔧 Configuration

### Environment Profiles

The application supports multiple environment profiles:

- **dev**: Development with H2 database and debug logging
- **test**: Testing with in-memory H2 and test data
- **staging**: Staging environment with PostgreSQL
- **prod**: Production environment with optimized settings

### Configuration Files


### Key Configuration Properties

```yaml
# JWT Configuration
jwt:
  secret: your-jwt-secret-key
  access-token-expiration: 3600000    # 1 hour
  refresh-token-expiration: 86400000  # 24 hours

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/client_management
    username: your-username
    password: your-password

# Logging Configuration
logging:
  level:
    com.jooyeon.app: INFO
    org.springframework.security: DEBUG
```

## 🗄️ Database Schema

### Entity Relationship Diagram
See [docs/erd-diagram.md](docs/erd-diagram.md) for detailed database schema and relationships.

### Core Tables
- **members**: User accounts and authentication
- **products**: Product catalog and inventory
- **orders**: Order management and status tracking
- **order_items**: Order line items with pricing
- **payments**: Payment processing records
- **idempotency_records**: Duplicate request prevention

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*UnitTest"
./gradlew test --tests "*IntegrationTest"

# Generate test coverage report
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Test Categories

- **Unit Tests**: Service layer with Mockito
- **Integration Tests**: Full application with database
- **Repository Tests**: JPA queries with @DataJpaTest
- **Security Tests**: Authentication and authorization
- **Concurrency Tests**: Distributed locking and race conditions
- **Performance Tests**: Load testing and benchmarks

### Test Data

```bash
# Load sample data for testing
curl -X POST http://localhost:8080/api/admin/load-sample-data \
  -H "Authorization: Bearer admin-token"
```

## 📦 Deployment

### Building for Production

```bash
# Build production JAR
./gradlew clean bootJar -Pprod

# JAR will be created at: build/libs/client-management-app-1.0.0.jar
```

### Running in Production

```bash
# Direct execution
java -jar client-management-app-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080

# With custom configuration
java -jar client-management-app-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.location=classpath:/application.yml,/path/to/external/config.yml
```
