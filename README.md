# 클라이언트 관리 애플리케이션

### 🗣️ 주요 기능
- 회원 관리
  - JWT를 활용한 회원 인증
  - 전화번호, 성별, 생년월일을 AES-256-GCM을 사용하여 암호화
- 주문 처리
  - 중복 요청을 방지하기 위해 Idempotency 사용
  - 동시성 이슈로 인한 문제를 방지를 위하여 Redis 분산락에 기반한 custom 로컬 분산락 구현 (@Lock 사용)

    
### 📚 API 문서
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs


### 🗄️ Database Schema
- erd : [erd-diagram](/docs/ERD.png)


### 🧪 Testing
- http file : [full cycle test http](/src/test/http/full-cycle-test.http)