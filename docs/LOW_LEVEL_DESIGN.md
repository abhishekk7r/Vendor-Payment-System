# Low-Level Design: Offline-Ready Payment System

## 🔧 Detailed Component Design

### 1. **Transaction Processing Engine**

#### 1.1 TransactionController
```java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    // Dependencies
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ValidationService validationService;
    @Autowired private PaymentMetrics paymentMetrics;
    @Autowired private PaymentEventPublisher eventPublisher;
    
    // Core Methods
    public ResponseEntity<String> makePaymentOnline(PaymentRequestOnline request)
    public ResponseEntity<String> makePaymentOffline(PaymentRequestOffline request)
    public ResponseEntity<List<Transaction>> getFlaggedTransactions()
    public ResponseEntity<String> reviewTransaction(Long adminId, Long transactionId, Boolean approval)
}
```

#### 1.2 Payment Processing Flow
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Validation    │───▶│  Fraud Check    │───▶│ Balance Check   │
│                 │    │                 │    │                 │
│ • Input params  │    │ • Geolocation   │    │ • Sufficient    │
│ • User exists   │    │ • Rate limiting │    │   funds         │
│ • Vendor exists │    │ • Code validity │    │ • Account       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Transaction     │    │ Balance Update  │    │ Event           │
│ Creation        │    │                 │    │ Publishing      │
│                 │    │ • Debit user    │    │                 │
│ • Generate ID   │    │ • Credit vendor │    │ • Success/Fail  │
│ • Set status    │    │ • Update DB     │    │ • Metrics       │
│ • Timestamp     │    │ • Cache update  │    │ • Notifications │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 2. **Fraud Detection System**

#### 2.1 Geolocation Validation
```java
private boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radius) {
    // Haversine Formula Implementation
    double earthRadius = 6371; // kilometers
    
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = earthRadius * c;
    
    return distance <= radius;
}
```

#### 2.2 Fraud Detection Decision Tree
```
Transaction Request
│
├─ Distance Check
│  ├─ > 20km ──────────────────────────────────────────┐
│  └─ ≤ 20km ─┐                                        │
│              │                                        │
├─ Amount Check                                         │
│  ├─ > Daily Limit ────────────────────────────────┐  │
│  └─ ≤ Daily Limit ─┐                              │  │
│                     │                              │  │
├─ Frequency Check                                   │  │
│  ├─ > Rate Limit ──────────────────────────────┐  │  │
│  └─ ≤ Rate Limit ─┐                            │  │  │
│                    │                            │  │  │
├─ Code Validation (Offline only)                 │  │  │
│  ├─ Invalid Code ──────────────────────────┐   │  │  │
│  └─ Valid Code ─┐                          │   │  │  │
│                 │                          │   │  │  │
└─ Final Decision │                          │   │  │  │
   │              │                          │   │  │  │
   ▼              ▼                          ▼   ▼  ▼  ▼
SUCCESS      FLAGGED                      FAILED FAILED FAILED FLAGGED
```

### 3. **Offline Transaction Management**

#### 3.1 Code Generation Algorithm
```java
public class OfflineCodeGenerator {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODES_PER_BATCH = 5;
    
    public Set<String> generateCodes() {
        Set<String> codes = new HashSet<>();
        SecureRandom random = new SecureRandom();
        
        while (codes.size() < CODES_PER_BATCH) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            codes.add(code.toString());
        }
        
        return codes;
    }
}
```

#### 3.2 Offline Transaction State Machine
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   CREATED   │───▶│  VALIDATED  │───▶│  PENDING    │───▶│  COMPLETED  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       │                   │                   ▼                   │
       │                   │            ┌─────────────┐            │
       │                   │            │   FLAGGED   │            │
       │                   │            └─────────────┘            │
       │                   │                   │                   │
       │                   │                   ▼                   │
       │                   │            ┌─────────────┐            │
       │                   │            │ ADMIN_REVIEW│            │
       │                   │            └─────────────┘            │
       │                   │                   │                   │
       │                   │                   ├─ APPROVE ────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   FAILED    │    │   FAILED    │    │   FAILED    │
└─────────────┘    └─────────────┘    └─────────────┘
```

### 4. **Database Schema Design**

#### 4.1 Core Tables
```sql
-- Users Table
CREATE TABLE table_user (
    user_id VARCHAR(255) PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) UNIQUE NOT NULL,
    is_approved BOOLEAN DEFAULT FALSE,
    approval_timestamp BIGINT,
    user_status BOOLEAN DEFAULT TRUE,
    user_enrolled BOOLEAN DEFAULT FALSE,
    user_enrollapproved BOOLEAN DEFAULT FALSE,
    user_latitude DOUBLE,
    user_longitude DOUBLE,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Wallets Table
CREATE TABLE wallet_table (
    wallet_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255),
    balance DOUBLE DEFAULT 0.0,
    offline_balance DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES table_user(user_id)
);

-- Wallet Codes Table
CREATE TABLE wallet_codes (
    wallet_id INT,
    code VARCHAR(8),
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    PRIMARY KEY (wallet_id, code),
    FOREIGN KEY (wallet_id) REFERENCES wallet_table(wallet_id)
);

-- Transactions Table
CREATE TABLE transaction_table (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    vendor_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    status ENUM('PENDING', 'SUCCESSFUL', 'FAILED', 'FLAGGED') DEFAULT 'PENDING',
    payment_mode ENUM('ONLINE', 'OFFLINE') NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    fraud_score DOUBLE DEFAULT 0.0,
    admin_reviewed BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES table_user(user_id),
    FOREIGN KEY (vendor_id) REFERENCES vendor_table(vendor_id)
);

-- Vendors Table
CREATE TABLE vendor_table (
    vendor_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    status BOOLEAN DEFAULT FALSE,
    store_wallet_id INT,
    personal_wallet_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (store_wallet_id) REFERENCES wallet_table(wallet_id),
    FOREIGN KEY (personal_wallet_id) REFERENCES wallet_table(wallet_id)
);
```

#### 4.2 Indexing Strategy
```sql
-- Performance Indexes
CREATE INDEX idx_user_email ON table_user(user_email);
CREATE INDEX idx_user_status ON table_user(user_status, is_approved);
CREATE INDEX idx_transaction_user_date ON transaction_table(user_id, transaction_date);
CREATE INDEX idx_transaction_status ON transaction_table(status);
CREATE INDEX idx_transaction_vendor ON transaction_table(vendor_id, transaction_date);
CREATE INDEX idx_wallet_codes_lookup ON wallet_codes(code, is_used);

-- Composite Indexes for Complex Queries
CREATE INDEX idx_transaction_fraud_review ON transaction_table(status, admin_reviewed, transaction_date);
CREATE INDEX idx_user_location ON table_user(user_latitude, user_longitude);
```

### 5. **Caching Strategy**

#### 5.1 Redis Cache Structure
```
Cache Keys:
├── user_cache:{userId}           # User profile data (TTL: 30 min)
├── wallet_balance:{userId}       # Wallet balances (TTL: 5 min)
├── offline_codes:{userId}        # Valid offline codes (TTL: 24 hours)
├── vendor_location:{vendorId}    # Vendor coordinates (TTL: 1 hour)
├── fraud_score:{userId}          # User fraud score (TTL: 15 min)
├── rate_limit:{userId}           # Rate limiting counters (TTL: 1 min)
└── pending_transactions          # Set of pending offline transactions
```

#### 5.2 Cache Invalidation Strategy
```java
@Service
public class CacheInvalidationService {
    
    @EventListener
    public void handleWalletUpdate(WalletUpdateEvent event) {
        // Invalidate wallet balance cache
        redisTemplate.delete("wallet_balance:" + event.getUserId());
    }
    
    @EventListener
    public void handleUserUpdate(UserUpdateEvent event) {
        // Invalidate user cache
        redisTemplate.delete("user_cache:" + event.getUserId());
    }
    
    @EventListener
    public void handleCodeUsage(CodeUsageEvent event) {
        // Remove used code from cache
        redisTemplate.opsForSet().remove("offline_codes:" + event.getUserId(), event.getCode());
    }
}
```

### 6. **API Design Specifications**

#### 6.1 RESTful Endpoints
```
Payment APIs:
POST   /api/transactions/make-payment-online     # Process online payment
POST   /api/transactions/make-payment-offline    # Process offline payment
GET    /api/transactions/flagged-transactions    # Get flagged transactions
POST   /api/transactions/review-transaction/{adminId}/{transactionId}/{approval}

Wallet APIs:
POST   /api/wallets/add-money/{userId}           # Add money to wallet
GET    /api/wallets/check-balance/{userId}       # Check wallet balance
POST   /api/wallets/transfer-to-offline/{userId} # Transfer to offline balance
GET    /api/wallets/get-codes/{userId}           # Get offline codes

User APIs:
POST   /home/register                            # Register new user
POST   /home/approve/{userId}                    # Approve user registration
GET    /home/checkWaitingPeriod                  # Check waiting period

Authentication APIs:
POST   /auth/login                               # User login
POST   /auth/refresh                             # Refresh JWT token
POST   /auth/logout                              # User logout
```

#### 6.2 Request/Response Models
```java
// Online Payment Request
public class PaymentRequestOnline {
    @NotNull @Size(min=1) private String userId;
    @NotNull @Positive private Integer vendorId;
    @NotNull @Positive private Double amount;
    @NotNull @DecimalMin("-90") @DecimalMax("90") private Double latitude;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double longitude;
}

// Offline Payment Request
public class PaymentRequestOffline extends PaymentRequestOnline {
    @NotNull @Size(min=8, max=8) private String code;
}

// Standard API Response
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
    private String requestId;
}
```

### 7. **Error Handling Strategy**

#### 7.1 Exception Hierarchy
```java
// Base Exception
public abstract class PaymentException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
}

// Specific Exceptions
public class InsufficientBalanceException extends PaymentException {
    public InsufficientBalanceException(double available, double requested) {
        super("Insufficient balance. Available: " + available + ", Requested: " + requested);
        this.errorCode = "INSUFFICIENT_BALANCE";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}

public class InvalidCodeException extends PaymentException {
    public InvalidCodeException(String code) {
        super("Invalid offline code: " + code);
        this.errorCode = "INVALID_CODE";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}

public class FraudDetectedException extends PaymentException {
    public FraudDetectedException(String reason) {
        super("Transaction flagged for fraud: " + reason);
        this.errorCode = "FRAUD_DETECTED";
        this.httpStatus = HttpStatus.FORBIDDEN;
    }
}
```

#### 7.2 Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .timestamp(LocalDateTime.now().toString())
                .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
}
```

### 8. **Performance Optimization**

#### 8.1 Database Connection Pooling
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

#### 8.2 Async Processing Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "paymentExecutor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Payment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 9. **Monitoring & Metrics**

#### 9.1 Custom Metrics
```java
// Business Metrics
payment.transactions.total{type=online,status=success}
payment.transactions.total{type=offline,status=success}
payment.fraud.detected.total{reason=distance}
payment.processing.duration{percentile=95}

// Technical Metrics
jvm.memory.used{area=heap}
http.server.requests{uri=/api/transactions/make-payment-online}
database.connections.active
redis.connections.active
```

#### 9.2 Health Check Endpoints
```java
@Component
public class PaymentHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check database connectivity
        // Check Redis connectivity
        // Check external service availability
        
        return Health.up()
                .withDetail("database", "UP")
                .withDetail("redis", "UP")
                .withDetail("fraud-service", "UP")
                .build();
    }
}
```

This low-level design provides the detailed implementation blueprint for building a production-ready offline payment system with all the necessary components for scalability, security, and maintainability.