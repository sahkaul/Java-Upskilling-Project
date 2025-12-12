## FinBankX - Secure Digital Banking Platform

A comprehensive implementation of a secure digital banking microservice using Spring Boot 3.2.3, MySQL, and JWT authentication.

### Project Overview

FinBankX is a multi-microservice banking platform with the following core components:

1. **User Microservice** - User registration, authentication, and role management
2. **Accounts Microservice** - Account management, transfers, and ledger operations

### Technology Stack

- **Framework**: Spring Boot 3.2.3
- **Database**: MySQL 8.0+
- **ORM**: Hibernate/JPA
- **Security**: Spring Security with JWT
- **Build Tool**: Maven
- **Java Version**: 17

### Key Features Implemented

#### 1. Customer & Account Management
- Customer profiles with PII encryption (AES-256-GCM)
- Multiple account types (SAVINGS, CURRENT)
- Account status management (ACTIVE, FROZEN, CLOSED)
- PII field masking in API responses

#### 2. Transfer System with Lifecycle
- Transfer states: REQUESTED → AUTHORIZED → POSTED / CANCELLED
- Transfer holds on source account during authorization
- Double-entry ledger posting on settlement
- Version control for transfer modifications (up to 10 versions)
- Support for transfer versioning and reversion

#### 3. Double-Entry Ledger
- Immutable ledger entries (DEBIT/CREDIT)
- Ledger invariant enforcement: sum(amount with sign) == 0
- Balance computation from ledger
- Support for interest and fee postings

#### 4. Security & Authorization
- Role-based access control (CUSTOMER, BANKER, OPS)
- Fine-grained ACL per account/resource
- Access denial tracking with correlationId
- PII field encryption at rest
- Secure password handling (from User microservice)

#### 5. Audit & Compliance
- Comprehensive audit trail for all operations
- Audit log filtering by actor, action, entity, date range
- Request/response context logging (sanitized)
- Correlation ID tracking for distributed tracing

#### 6. Idempotency & Safety
- Idempotency key validation (24-hour TTL window)
- Request hash verification
- Conflict detection on hash mismatch (409 Conflict)
- Concurrency controls with account-level locking

#### 7. Rate Limiting
- Per-user, per-endpoint rate limiting
- Configurable thresholds by role:
  - CUSTOMER: 30 requests/min
  - BANKER: 60 requests/min
  - OPS: 120 requests/min
- Retry-After header support

#### 8. Scheduled Operations
- Daily interest accrual (configurable annual rate)
- Monthly statement generation
- Encryption key rotation triggers
- Expired idempotency key cleanup

#### 9. Transfer Limits & Validations
- Per-transaction limits
- Daily aggregate limits
- Overdraft prevention
- Currency matching validation

### API Endpoints

#### Customer Management (`/api/customers`)
- `POST /` - Create customer
- `GET /{customerId}` - Get customer by ID
- `GET /user/{userId}` - Get customer by user ID
- `GET` - List all customers (paginated)
- `GET /search/{name}` - Search customers
- `PUT /{customerId}` - Update customer
- `DELETE /{customerId}` - Delete customer

#### Account Management (`/api/accounts`)
- `POST /` - Create account
- `GET /{accountId}` - Get account by ID
- `GET /number/{accountNumber}` - Get account by number
- `GET /customer/{customerId}` - List customer accounts
- `GET` - List all accounts (paginated)
- `GET /{accountId}/balance` - Get account balance
- `PUT /{accountId}` - Update account
- `POST /{accountId}/freeze` - Freeze account
- `POST /{accountId}/unfreeze` - Unfreeze account
- `POST /{accountId}/close` - Close account

#### Transfer Management (`/api/transfers`)
- `POST /` - Initiate transfer
- `POST /{transferId}/authorize` - Authorize transfer
- `POST /{transferId}/post` - Post transfer (settle)
- `POST /{transferId}/cancel` - Cancel transfer
- `GET /{transferId}` - Get transfer status
- `GET /account/{accountId}` - Get account transfers
- `GET /status/{status}` - Get transfers by status

#### Audit Logs (`/api/audit`)
- `GET` - List audit logs (paginated)
- `GET /actor/{actorId}` - Get logs by actor
- `GET /entity/{entityType}/{entityId}` - Get logs by entity
- `GET /date-range` - Get logs by date range

### Database Schema

The application uses MySQL with the following main tables:

- `customers` - Customer profiles with encrypted PII
- `accounts` - Bank accounts with status and balance
- `transfers` - Transfer records with lifecycle status
- `transfer_versions` - Historical versions of transfers
- `transfer_holds` - Temporary holds on accounts
- `ledger_entries` - Double-entry ledger
- `audit_logs` - Comprehensive audit trail
- `access_control_lists` - Fine-grained access control
- `idempotency_keys` - Idempotent request deduplication
- `rate_limit_entries` - Rate limiting tracking

### Configuration

Key configuration properties in `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finbankx
    username: root
    password: ''

app:
  jwt:
    secret: <JWT_SECRET>
    expiration: 900000  # 15 minutes
  encryption:
    key: <ENCRYPTION_KEY>
    algorithm: AES/GCM/NoPadding
  limits:
    customer:
      per-transaction: 100000.00
      daily-aggregate: 500000.00
  interest:
    default-annual-rate: 3.5
```

### Error Codes

- `RESOURCE_NOT_FOUND` (404) - Resource does not exist
- `ACCESS_DENIED` (403) - Insufficient permissions
- `INSUFFICIENT_FUNDS` (422) - Insufficient balance for transfer
- `LIMIT_DAILY_EXCEEDED` (422) - Daily transfer limit exceeded
- `LIMIT_PER_TX_EXCEEDED` (422) - Per-transaction limit exceeded
- `INVALID_TRANSFER` (422) - Transfer validation failed
- `IDEMPOTENCY_CONFLICT` (409) - Idempotency key mismatch
- `RATE_LIMIT_EXCEEDED` (429) - Rate limit exceeded
- `VALIDATION_ERROR` (400) - Request validation failed

### Building & Running

#### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+

#### Build
```bash
cd Accounts-Microservice
./mvnw clean package
```

#### Run
```bash
./mvnw spring-boot:run
```

#### Database Setup
The application uses Hibernate's `ddl-auto: update` to automatically create/update schema. SQL scripts are also provided in `src/main/resources/schema.sql`.

### Security Considerations

1. **PII Encryption**: All sensitive customer data (email, phone, address) is encrypted using AES-256-GCM
2. **JWT Authentication**: Stateless JWT tokens with 15-minute expiration
3. **Access Control**: Role-based and ACL-based authorization on all endpoints
4. **Audit Logging**: All operations logged with actor ID, action, and correlation ID
5. **Rate Limiting**: Per-user, per-endpoint rate limiting to prevent abuse
6. **Idempotency**: Duplicate request detection and handling

### Testing

Comprehensive test coverage includes:
- Unit tests for services
- Integration tests for repositories
- API endpoint tests
- Security and authorization tests
- Idempotency tests
- Rate limiting tests

### Project Structure

```
Accounts-Microservice/
├── src/main/java/com/example/accounts/
│   ├── AccountsApplication.java
│   ├── audit/
│   ├── config/
│   ├── constants/
│   ├── controller/
│   │   ├── AccountController.java
│   │   ├── AuditController.java
│   │   ├── CustomerController.java
│   │   └── TransferController.java
│   ├── crypto/
│   │   └── EncryptionService.java
│   ├── dto/
│   │   ├── AccountsDto.java
│   │   ├── AuditLogDto.java
│   │   ├── CustomerDto.java
│   │   ├── TransferRequestDto.java
│   │   └── TransferResponseDto.java
│   ├── entity/
│   │   ├── AccessControlList.java
│   │   ├── Account.java
│   │   ├── AuditLog.java
│   │   ├── Customer.java
│   │   ├── IdempotencyKey.java
│   │   ├── LedgerEntry.java
│   │   ├── RateLimitEntry.java
│   │   ├── Transfer.java
│   │   ├── TransferHold.java
│   │   └── TransferVersion.java
│   ├── exception/
│   │   ├── AccessDeniedException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── IdempotencyConflictException.java
│   │   ├── InsufficientFundsException.java
│   │   ├── InvalidTransferException.java
│   │   ├── RateLimitExceededException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── TransferLimitExceededException.java
│   ├── reository/
│   │   ├── AccessControlListRepository.java
│   │   ├── AccountRepository.java
│   │   ├── AuditLogRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── IdempotencyKeyRepository.java
│   │   ├── LedgerRepository.java
│   │   ├── RateLimitRepository.java
│   │   ├── TransferHoldRepository.java
│   │   ├── TransferRepository.java
│   │   └── TransferVersionRepository.java
│   ├── service/
│   │   ├── AccessControlService.java
│   │   ├── AccountService.java
│   │   ├── AuditService.java
│   │   ├── CustomerService.java
│   │   ├── IdempotencyService.java
│   │   ├── LedgerService.java
│   │   ├── RateLimitService.java
│   │   └── TransferService.java
│   ├── service/impl/
│   │   ├── AccessControlServiceImpl.java
│   │   ├── AccountServiceImpl.java
│   │   ├── AuditServiceImpl.java
│   │   ├── CustomerServiceImpl.java
│   │   ├── IdempotencyServiceImpl.java
│   │   ├── LedgerServiceImpl.java
│   │   ├── RateLimitServiceImpl.java
│   │   ├── ScheduledTaskService.java
│   │   └── TransferServiceImpl.java
│   └── util/
│       ├── GeneratorUtil.java
│       ├── MaskingUtil.java
│       └── SecurityUtil.java
├── src/main/resources/
│   ├── application.yaml
│   └── schema.sql
└── pom.xml
```

### Future Enhancements

1. **Multi-Currency Support**: FX conversion with rates
2. **Statement Export**: PDF/CSV statement generation
3. **Mobile Banking**: Mobile app integration
4. **Advanced Analytics**: Transaction analytics and reporting
5. **Fraud Detection**: ML-based fraud detection
6. **Microservice Communication**: Service-to-service communication using gRPC
7. **Message Queue**: Event-driven architecture with Kafka/RabbitMQ
8. **API Gateway**: Central API gateway for routing and rate limiting

### Support & Documentation

For API documentation, access the Swagger UI:
- URL: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

### License

Apache 2.0

