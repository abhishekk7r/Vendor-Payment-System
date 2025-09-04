# System Flow Diagrams: Offline-Ready Payment System

## 🔄 Complete Transaction Flows

### 1. **Online Payment Flow**

```mermaid
sequenceDiagram
    participant U as User/Client
    participant API as API Gateway
    participant TC as TransactionController
    participant VS as ValidationService
    participant FS as FraudService
    participant WS as WalletService
    participant DB as MySQL Database
    participant R as Redis Cache
    participant EP as EventPublisher
    participant M as Metrics

    U->>API: POST /api/transactions/make-payment-online
    API->>API: Rate Limiting Check
    API->>API: JWT Authentication
    API->>TC: Forward Request
    
    TC->>VS: Validate Input Parameters
    VS-->>TC: Validation Result
    
    TC->>R: Check User Cache
    alt Cache Miss
        TC->>DB: Fetch User Data
        DB-->>TC: User Details
        TC->>R: Cache User Data
    else Cache Hit
        R-->>TC: Cached User Data
    end
    
    TC->>FS: Perform Fraud Check
    FS->>FS: Geolocation Validation (20km radius)
    FS->>FS: Rate Limiting Check
    FS->>FS: Amount Validation
    FS-->>TC: Fraud Check Result
    
    alt Fraud Detected (Distance > 20km)
        TC->>DB: Create FLAGGED Transaction
        TC->>WS: Debit User Balance
        TC->>EP: Publish FRAUD_DETECTED Event
        TC->>M: Increment Fraud Metrics
        TC-->>U: "Payment flagged for review"
    else No Fraud Detected
        TC->>WS: Check Sufficient Balance
        alt Insufficient Balance
            TC->>M: Increment Failed Payment Metrics
            TC-->>U: "Insufficient Balance Error"
        else Sufficient Balance
            TC->>DB: Begin Transaction
            TC->>WS: Debit User Balance
            TC->>WS: Credit Vendor Balance
            TC->>DB: Create SUCCESSFUL Transaction
            TC->>DB: Commit Transaction
            TC->>R: Update Balance Cache
            TC->>EP: Publish PAYMENT_SUCCESS Event
            TC->>M: Increment Success Metrics
            TC-->>U: "Payment Successful"
        end
    end
```

### 2. **Offline Payment Flow**

```mermaid
sequenceDiagram
    participant U as User/Client
    participant API as API Gateway
    participant TC as TransactionController
    participant VS as ValidationService
    participant CS as CodeService
    participant FS as FraudService
    participant WS as WalletService
    participant DB as MySQL Database
    participant R as Redis Cache
    participant OTS as OfflineTransactionService
    participant EP as EventPublisher

    Note over U,EP: Phase 1: Code Generation (Online)
    U->>API: POST /api/wallets/transfer-to-offline/{userId}
    API->>TC: Transfer Request
    TC->>CS: Generate Offline Codes (5 codes)
    CS->>CS: Generate 8-char alphanumeric codes
    CS->>DB: Store Codes in wallet_codes table
    CS->>R: Cache Codes for quick access
    TC->>WS: Transfer balance (online → offline)
    TC-->>U: Return Generated Codes

    Note over U,EP: Phase 2: Offline Payment Processing
    U->>API: POST /api/transactions/make-payment-offline
    API->>TC: Offline Payment Request
    
    TC->>VS: Validate Input Parameters
    VS-->>TC: Validation Result
    
    TC->>R: Check Code in Cache
    alt Code in Cache
        R-->>TC: Code Valid
    else Code Not in Cache
        TC->>DB: Verify Code in Database
        DB-->>TC: Code Verification Result
    end
    
    alt Invalid Code
        TC->>EP: Publish INVALID_CODE Event
        TC-->>U: "Invalid Code Error"
    else Valid Code
        TC->>FS: Perform Fraud Check
        FS->>FS: Geolocation Validation
        FS-->>TC: Fraud Check Result
        
        alt Fraud Detected
            TC->>OTS: Store in Pending Queue
            TC->>WS: Debit Offline Balance
            TC->>DB: Create FLAGGED Transaction
            TC->>EP: Publish FRAUD_DETECTED Event
            TC-->>U: "Payment flagged for review"
        else No Fraud
            TC->>WS: Check Offline Balance
            alt Insufficient Offline Balance
                TC-->>U: "Insufficient Offline Balance"
            else Sufficient Balance
                TC->>DB: Begin Transaction
                TC->>WS: Debit User Offline Balance
                TC->>WS: Credit Vendor Balance
                TC->>CS: Mark Code as Used
                TC->>DB: Create SUCCESSFUL Transaction
                TC->>DB: Commit Transaction
                TC->>R: Update Caches
                TC->>EP: Publish OFFLINE_PAYMENT_SUCCESS Event
                TC-->>U: "Offline Payment Successful"
            end
        end
    end
```

### 3. **User Registration & Approval Flow**

```mermaid
sequenceDiagram
    participant U as User
    participant API as API Gateway
    participant UC as UserController
    participant US as UserService
    participant VS as ValidationService
    participant DB as MySQL Database
    participant WS as WalletService
    participant A as Admin
    participant EP as EventPublisher

    Note over U,EP: Phase 1: User Registration
    U->>API: POST /home/register
    API->>UC: Registration Request
    UC->>VS: Validate User Data
    VS->>VS: Email Format Validation
    VS->>VS: Name Length Validation
    VS-->>UC: Validation Result
    
    alt Validation Failed
        UC-->>U: "Validation Error"
    else Validation Passed
        UC->>US: Register User
        US->>DB: Check Email Uniqueness
        alt Email Exists
            US-->>UC: "Email Already Exists"
            UC-->>U: "Email Already Registered"
        else Email Unique
            US->>DB: Create User (isApproved=false)
            US->>WS: Create Empty Wallet
            US->>EP: Publish USER_REGISTERED Event
            US-->>UC: User ID
            UC-->>U: "Registration Successful, Awaiting Approval"
        end
    end

    Note over U,EP: Phase 2: Admin Approval
    A->>API: POST /home/approve/{userId}
    API->>UC: Approval Request
    UC->>US: Approve User
    US->>DB: Update User (isApproved=true, approvalTimestamp=now)
    US->>EP: Publish USER_APPROVED Event
    US-->>UC: "User Approved"
    UC-->>A: "User Approved Successfully"

    Note over U,EP: Phase 3: Waiting Period Check
    U->>API: GET /home/checkWaitingPeriod?userId={userId}
    API->>UC: Check Waiting Period
    UC->>US: Check Waiting Period (15 minutes)
    US->>US: Calculate Time Difference
    alt Waiting Period Over
        US-->>UC: "Waiting Period Complete"
        UC-->>U: "You can now use all features"
    else Still Waiting
        US-->>UC: "Still in Waiting Period"
        UC-->>U: "Please wait before using features"
    end
```

### 4. **Admin Transaction Review Flow**

```mermaid
sequenceDiagram
    participant A as Admin
    participant API as API Gateway
    participant TC as TransactionController
    participant DB as MySQL Database
    participant WS as WalletService
    participant EP as EventPublisher
    participant U as User
    participant V as Vendor

    Note over A,V: Phase 1: Get Flagged Transactions
    A->>API: GET /api/transactions/flagged-transactions
    API->>TC: Get Flagged Transactions
    TC->>DB: Query FLAGGED Transactions
    DB-->>TC: List of Flagged Transactions
    TC-->>A: Flagged Transactions List

    Note over A,V: Phase 2: Admin Review Decision
    A->>API: POST /api/transactions/review-transaction/{adminId}/{transactionId}/{approval}
    API->>TC: Review Transaction Request
    TC->>DB: Fetch Transaction Details
    TC->>DB: Verify Admin Permissions
    
    alt Transaction Not Found
        TC-->>A: "Transaction Not Found"
    else Admin Not Authorized
        TC-->>A: "Unauthorized"
    else Transaction Not Flagged
        TC-->>A: "Transaction Not Pending Review"
    else Valid Review Request
        alt Approval = true (Approve Transaction)
            TC->>WS: Transfer Amount to Vendor
            TC->>DB: Update Transaction Status to SUCCESSFUL
            TC->>DB: Mark as admin_reviewed=true
            TC->>EP: Publish TRANSACTION_APPROVED Event
            TC-->>A: "Transaction Approved Successfully"
            Note over V: Vendor receives payment
        else Approval = false (Reject Transaction)
            TC->>WS: Return Amount to User Balance
            TC->>DB: Update Transaction Status to FAILED
            TC->>DB: Mark as admin_reviewed=true
            TC->>EP: Publish TRANSACTION_REJECTED Event
            TC-->>A: "Transaction Rejected Successfully"
            Note over U: User gets refund
        end
    end
```

### 5. **Batch Processing Flow (Background)**

```mermaid
sequenceDiagram
    participant S as Scheduler
    participant OTS as OfflineTransactionService
    participant R as Redis Cache
    participant DB as MySQL Database
    participant WS as WalletService
    participant EP as EventPublisher

    Note over S,EP: Scheduled Every 30 Seconds
    S->>OTS: @Scheduled processPendingTransactions()
    OTS->>R: Get Pending Transaction Keys
    R-->>OTS: Set of Transaction Keys
    
    loop For Each Pending Transaction
        OTS->>R: Get Transaction Details
        R-->>OTS: Transaction Object
        
        alt Transaction Exists
            OTS->>OTS: Validate Transaction
            alt Transaction Valid
                OTS->>DB: Update Transaction Status to SUCCESSFUL
                OTS->>WS: Complete Balance Transfer
                OTS->>EP: Publish BATCH_PROCESSED Event
                OTS->>R: Remove from Pending Queue
            else Transaction Invalid
                OTS->>DB: Update Transaction Status to FAILED
                OTS->>WS: Refund User Balance
                OTS->>EP: Publish BATCH_FAILED Event
                OTS->>R: Remove from Pending Queue
            end
        else Transaction Not Found
            OTS->>R: Remove Stale Key from Queue
        end
    end
    
    OTS->>EP: Publish BATCH_COMPLETE Event
```

### 6. **Fraud Detection Decision Flow**

```mermaid
flowchart TD
    A[Transaction Request] --> B{User Exists?}
    B -->|No| Z[FAILED: User Not Found]
    B -->|Yes| C{Vendor Exists?}
    C -->|No| Y[FAILED: Vendor Not Found]
    C -->|Yes| D{Valid Amount?}
    D -->|No| X[FAILED: Invalid Amount]
    D -->|Yes| E{Rate Limit Check}
    E -->|Exceeded| W[FAILED: Rate Limit Exceeded]
    E -->|OK| F{Sufficient Balance?}
    F -->|No| V[FAILED: Insufficient Balance]
    F -->|Yes| G{Distance Check}
    G -->|> 20km| H[FLAGGED: Distance Fraud]
    G -->|≤ 20km| I{Offline Payment?}
    I -->|Yes| J{Valid Code?}
    J -->|No| U[FAILED: Invalid Code]
    J -->|Yes| K{Code Already Used?}
    K -->|Yes| T[FAILED: Code Already Used]
    K -->|No| L[SUCCESS: Process Payment]
    I -->|No| L[SUCCESS: Process Payment]
    
    H --> M[Debit Balance & Queue for Admin Review]
    L --> N[Complete Transaction & Update Balances]
    
    style A fill:#e1f5fe
    style L fill:#c8e6c9
    style H fill:#fff3e0
    style Z fill:#ffcdd2
    style Y fill:#ffcdd2
    style X fill:#ffcdd2
    style W fill:#ffcdd2
    style V fill:#ffcdd2
    style U fill:#ffcdd2
    style T fill:#ffcdd2
```

### 7. **Caching Strategy Flow**

```mermaid
flowchart TD
    A[API Request] --> B{Check Redis Cache}
    B -->|Cache Hit| C[Return Cached Data]
    B -->|Cache Miss| D[Query Database]
    D --> E[Store in Cache with TTL]
    E --> F[Return Data to Client]
    
    G[Data Update Event] --> H{Cache Invalidation}
    H --> I[Remove from Cache]
    H --> J[Update Database]
    
    K[Scheduled Cache Cleanup] --> L{Check TTL}
    L -->|Expired| M[Remove Expired Keys]
    L -->|Valid| N[Keep in Cache]
    
    style C fill:#c8e6c9
    style F fill:#c8e6c9
    style I fill:#fff3e0
    style M fill:#fff3e0
```

## 📊 System Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        A[Mobile App]
        B[Web Dashboard]
        C[POS Terminal]
        D[Admin Panel]
    end
    
    subgraph "API Gateway Layer"
        E[Load Balancer]
        F[Rate Limiter]
        G[JWT Auth]
        H[Request Logger]
    end
    
    subgraph "Application Layer"
        I[Transaction Controller]
        J[User Controller]
        K[Wallet Controller]
        L[Admin Controller]
    end
    
    subgraph "Service Layer"
        M[Payment Service]
        N[Fraud Detection Service]
        O[Offline Transaction Service]
        P[Event Publisher]
        Q[Validation Service]
    end
    
    subgraph "Data Layer"
        R[(MySQL Database)]
        S[(Redis Cache)]
        T[Event Bus]
    end
    
    subgraph "Monitoring Layer"
        U[Metrics Collector]
        V[Health Checks]
        W[Log Aggregator]
    end
    
    A --> E
    B --> E
    C --> E
    D --> E
    
    E --> F
    F --> G
    G --> H
    H --> I
    H --> J
    H --> K
    H --> L
    
    I --> M
    I --> N
    I --> O
    J --> M
    K --> M
    L --> M
    
    M --> P
    N --> P
    O --> P
    
    M --> Q
    N --> Q
    O --> Q
    
    M --> R
    M --> S
    N --> R
    N --> S
    O --> R
    O --> S
    P --> T
    
    M --> U
    N --> U
    O --> U
    
    style A fill:#e3f2fd
    style B fill:#e3f2fd
    style C fill:#e3f2fd
    style D fill:#e3f2fd
    style R fill:#f3e5f5
    style S fill:#fff3e0
    style T fill:#e8f5e8
```

These diagrams provide a comprehensive visual understanding of how the Offline-Ready Payment System works, from high-level architecture to detailed transaction flows. Each diagram shows the interaction between different components and the decision-making process at various stages of the system.