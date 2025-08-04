# Offline Payment System

A Spring Boot application that enables secure offline payments with geolocation-based fraud detection and admin approval workflows.

## Overview

This system allows users to make payments both online and offline, with built-in security features including:
- Geolocation-based transaction validation
- Offline payment codes for secure transactions without internet connectivity
- Admin review system for flagged transactions
- JWT-based authentication and authorization
- Wallet management with separate online and offline balances

## Features

### Core Functionality
- **User Registration & Approval**: Multi-step user onboarding with admin approval
- **Dual Payment Modes**: Support for both online and offline transactions
- **Geolocation Validation**: Automatic flagging of transactions outside 20km radius
- **Offline Payment Codes**: Secure alphanumeric codes for offline transactions
- **Wallet Management**: Separate online and offline balance tracking
- **Admin Dashboard**: Review and approve/reject flagged transactions

### Security Features
- JWT authentication with Spring Security
- Role-based access control (User, Vendor, Admin)
- Transaction fraud detection based on location
- Secure offline payment code generation
- Optimistic locking for transaction integrity

## Tech Stack

- **Framework**: Spring Boot 3.2.1
- **Java Version**: 21
- **Database**: MySQL 8.0
- **Security**: Spring Security 6 with JWT
- **ORM**: Spring Data JPA with Hibernate
- **Build Tool**: Maven
- **Additional**: Lombok, Apache Commons Lang3

## Project Structure

```
src/main/java/com/example/offlinePayment/
├── config/           # Security and application configuration
├── controller/       # REST API endpoints
├── exceptions/       # Custom exception classes
├── model/           # JPA entities and DTOs
├── repository/      # Data access layer
├── security/        # JWT and authentication components
└── service/         # Business logic layer
```

## Getting Started

### Prerequisites
- Java 21
- Maven 3.6+
- MySQL 8.0+

### Database Setup
1. Create a MySQL database named `au_session`
2. Update database credentials in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/au_session?autoreconnect=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Installation
1. Clone the repository
2. Navigate to the project directory
3. Install dependencies:
```bash
mvn clean install
```
4. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8081`

### Default Admin Credentials
- **Username**: admin@example.com
- **Password**: admin123

### Testing the Application
1. **Login**: POST `/auth/login` with admin credentials to get JWT token
2. **Register User**: POST `/home/register` with user details
3. **Approve User**: POST `/home/approve/{userId}`
4. **Add Money**: POST `/api/wallets/add-money/{userId}?amount=100`
5. **Make Payment**: POST `/api/transactions/make-payment-online`

## API Endpoints

### User Management
- `POST /home/register` - Register a new user
- `POST /home/approve/{userId}` - Approve user registration
- `GET /home/checkWaitingPeriod?userId={userId}` - Check user waiting period

### Wallet Operations
- `POST /api/wallets/add-money/{userId}?amount={amount}` - Add money to wallet
- `GET /api/wallets/check-balance/{userId}` - Check wallet balance
- `POST /api/wallets/transfer-to-offline/{userId}?amount={amount}` - Transfer to offline balance
- `GET /api/wallets/get-codes/{userId}` - Get offline payment codes

### Transactions
- `POST /api/transactions/make-payment-online` - Process online payment
- `POST /api/transactions/make-payment-offline` - Process offline payment
- `GET /api/transactions/flagged-transactions` - Get flagged transactions (Admin)
- `POST /api/transactions/review-transaction/{adminId}/{transactionId}/{approval}` - Review transaction (Admin)

## Payment Flow

### Online Payment
1. User initiates payment with location data
2. System validates user and vendor existence
3. Geolocation check (20km radius)
4. If within radius: immediate processing
5. If outside radius: transaction flagged for admin review

### Offline Payment
1. User transfers money to offline balance
2. System generates secure payment codes
3. User provides code and location for payment
4. Same geolocation validation as online payments
5. Code validation ensures transaction authenticity

## Security Considerations

- All transactions include geolocation validation
- Offline payments require pre-generated secure codes
- Flagged transactions require admin approval
- JWT tokens for API authentication
- Role-based access control for different user types

## Database Schema

### Key Entities
- **User**: User information with approval status and location
- **Vendor**: Merchant information with store and personal wallets
- **Wallet**: Balance management with online/offline separation
- **Transaction**: Payment records with status tracking
- **Admin**: Administrative users for transaction review

## Configuration

### Application Properties
```properties
server.port=8081
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
java -jar target/payment-0.0.1-SNAPSHOT.jar
```

## Recent Improvements

### ✅ Fixed Issues
- **Data Persistence**: UserService now uses database instead of in-memory storage
- **Input Validation**: Added comprehensive validation for all endpoints
- **Exception Handling**: Global exception handler for consistent error responses
- **Data Type Consistency**: Fixed casting issues between double and long
- **Security Configuration**: Proper endpoint access control
- **Code Quality**: Added proper logging and error messages

### ✅ New Features
- **Validation Service**: Centralized input validation
- **Global Exception Handler**: Consistent error handling across all endpoints
- **Enhanced API Responses**: More informative success/error messages
- **Wallet Balance API**: Returns both online and offline balances
- **Unit Tests**: Basic test coverage for core services

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is developed as a demo application for Spring Boot offline payment systems.