# PayFlow Backend 🚀

### Secure Payment Management Platform | Java • Spring Boot • PostgreSQL • Razorpay

PayFlow is a full-stack payment management platform designed to simplify payment processing, transaction tracking, refund management, and merchant operations through secure and scalable REST APIs.

The backend is developed using Java and Spring Boot, with PostgreSQL for persistent data storage and Razorpay integration for payment processing. It provides authentication, transaction management, notifications, reporting, and password recovery functionality for the PayFlow frontend.

> **Project Status:** Development and deployment in progress. Razorpay integration should remain in Test Mode until end-to-end testing is complete.

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
- [Project Structure](#-project-structure)
- [API Modules](#-api-modules)
- [Security](#-security)
- [Database](#-database)
- [Getting Started](#-getting-started)
- [Environment Configuration](#-environment-configuration)
- [Running the Application](#-running-the-application)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Future Improvements](#-future-improvements)
- [Author](#-author)

---

## 🎯 Overview

PayFlow aims to provide a centralized backend for payment-related operations through RESTful APIs.

The application separates business logic, API controllers, data access, security, and database entities into dedicated modules. This structure makes the codebase easier to maintain, test, and extend.

### Main Objectives

- Provide secure user authentication and authorization.
- Manage payment orders and transaction records.
- Integrate with the Razorpay payment gateway.
- Support refund workflows and transaction reporting.
- Provide notification and account recovery functionality.
- Connect a React-based frontend with a Java Spring Boot backend.

---

## ✨ Key Features

### 🔐 Authentication and Account Management

- User registration and login APIs.
- JWT-based authentication.
- Protected endpoints using Spring Security.
- OTP verification during supported authentication workflows.
- Forgot-password and password-reset functionality.
- Centralized exception handling for API errors.

### 💳 Payment Management

- Payment order creation.
- Razorpay payment gateway integration.
- Payment verification workflow.
- Transaction status management.
- Payment details retrieval.
- Payment-related API responses for frontend integration.

### 💸 Refund Management

- Refund request handling.
- Refund-related status management.
- Refund records associated with payment transactions.
- Refund API integration with the payment workflow.

*Refund availability and final status depend on the implemented backend logic and the payment gateway response.*

### 📊 Transaction Reporting

- Transaction reporting endpoints.
- Transaction data retrieval.
- Backend services for generating transaction reports.

### 🔔 Notifications

- Notification API endpoints.
- Notification persistence and retrieval.
- Service and repository layers for notification management.

### 🛡️ Error Handling and Validation

- Centralized exception handling.
- Structured request and response DTOs.
- Separation of controller and service responsibilities.
- Database-backed entities and repository interfaces.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java | Core backend programming language |
| Spring Boot | Application framework |
| Spring Web | REST API development |
| Spring Security | API security and access control |
| JWT | Token-based authentication |
| Spring Data JPA | Database access |
| Hibernate | ORM and entity persistence |
| PostgreSQL | Relational database |
| Maven | Dependency and build management |
| Razorpay API | Payment gateway integration |
| Git & GitHub | Version control and collaboration |

*The exact dependencies and versions are defined in `pom.xml`.*

---

## 🏗️ System Architecture

```text
                 ┌──────────────────────┐
                 │   React + Vite UI    │
                 │   Vercel Deployment  │
                 └──────────┬───────────┘
                            │ HTTPS / REST
                            ▼
                 ┌──────────────────────┐
                 │   Spring Boot API    │
                 │   Authentication     │
                 │   Business Logic     │
                 └──────────┬───────────┘
                            │
             ┌──────────────┼──────────────┐
             ▼              ▼              ▼
      ┌────────────┐ ┌────────────┐ ┌─────────────┐
      │ PostgreSQL │ │  Razorpay  │ │ Email / OTP │
      │  Database  │ │  Gateway   │ │  Services   │
      └────────────┘ └────────────┘ └─────────────┘
```

The frontend communicates with the backend through HTTP APIs. The backend manages authentication, business operations, persistence, and supported third-party integrations.

**Deployment note:** The frontend is deployed on Vercel. Backend hosting and cloud database configuration are separate deployment steps.

---

## 📁 Project Structure

```text
PayFlow-backend/
├── .github/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/payflow/
│   │   │   ├── config/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── MerchantController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── PaymentOrderController.java
│   │   │   │   ├── PaymentWebhookController.java
│   │   │   │   ├── RefundController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── auth/
│   │   │   │   ├── payment/
│   │   │   │   └── refund/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── report/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── PayflowApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── .gitattributes
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

*This structure reflects the main files present in the project; package contents may evolve as development continues.*

---

## 🔌 API Modules

The backend organizes its APIs into modules for authentication, users, merchants, payments, refunds, notifications, reporting, and health checks.

| Module | Responsibility |
|---|---|
| Authentication | Registration, login, OTP, password recovery |
| Users | User account operations |
| Merchants | Merchant-related operations |
| Payments | Payment creation, verification, and retrieval |
| Payment Orders | Payment order workflows |
| Refunds | Refund requests and refund status |
| Notifications | Notification operations |
| Transaction Reports | Transaction reporting |
| Health | Application health endpoint |

The precise endpoint paths, request payloads, authentication requirements, and response formats should be verified against the corresponding controllers and DTOs before publishing API documentation.

---

## 🔐 Security

Security is an important part of the PayFlow backend.

The project includes JWT authentication components and Spring Security configuration for protected API access.

### Security Practices

- Keep database credentials outside committed source code.
- Store JWT signing secrets in environment variables.
- Keep Razorpay secret keys private.
- Never commit email passwords, API tokens, or production credentials.
- Validate incoming requests on the server.
- Use HTTPS for deployed environments.
- Verify payment results on the backend rather than trusting frontend-only status.
- Validate payment webhook signatures when processing gateway webhooks.
- Use test credentials during development and integration testing.

**Important:** JWT and payment-gateway secrets must never be exposed in frontend environment variables or public repositories.

---

## 🗄️ Database

PayFlow uses PostgreSQL as its relational database.

The backend includes persistence components for users, merchants, notifications, payment orders, payments, and refunds.

Spring Data JPA and Hibernate provide the object-relational mapping layer between Java entities and database tables.

### Database Configuration

Configure the following values for the environment in which the application runs:

- Database JDBC URL
- Database username
- Database password
- Database schema and migration strategy, as applicable

For production deployment, use a managed PostgreSQL database and securely configure the connection through environment variables.

Do not publish database credentials or production database URLs containing passwords.

---

## 🚀 Getting Started

### Prerequisites

Install or configure the following tools:

- Java Development Kit compatible with the project
- Git
- PostgreSQL
- A Razorpay test account for gateway integration testing

Maven can be run through the Maven Wrapper included in this repository.

### 1. Clone the Repository

```bash
git clone https://github.com/Pratyush-95/PayFlow-backend.git
cd PayFlow-backend
```

### 2. Configure PostgreSQL

Create a PostgreSQL database for local development and configure the connection settings through environment variables or an untracked local configuration file.

Example JDBC URL:

```text
jdbc:postgresql://localhost:5432/payflow
```

The database name and connection details must match your local PostgreSQL setup.

### 3. Configure Application Secrets

Set the required database, JWT, email, and Razorpay configuration values before starting the application.

Never commit real secrets to GitHub.

### 4. Build the Application

On Windows:

```bash
.\mvnw.cmd clean package
```

On macOS or Linux:

```bash
./mvnw clean package
```

### 5. Start the Application

On Windows:

```bash
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The server port is determined by the application configuration. Confirm the configured port before testing endpoints.

---

## ⚙️ Environment Configuration

The application may require configuration for the following categories:

| Category | Example variable name |
|---|---|
| Database URL | `DB_URL` |
| Database username | `DB_USERNAME` |
| Database password | `DB_PASSWORD` |
| JWT secret | `JWT_SECRET` |
| Razorpay key ID | `RAZORPAY_KEY_ID` |
| Razorpay secret | `RAZORPAY_KEY_SECRET` |
| Email username | `MAIL_USERNAME` |
| Email password | `MAIL_PASSWORD` |

These are suggested variable names for deployment documentation. The application properties must reference the matching names for them to work.

Do not paste actual secret values into this README.

---

## 🧪 Testing

The repository includes a Spring Boot test source directory.

Build and execute tests using:

```bash
.\mvnw.cmd test
```

Before a production release, test:

- User registration and login.
- JWT-protected endpoints.
- OTP verification and password reset.
- Payment order creation and payment verification.
- Refund workflows.
- Transaction retrieval and reporting.
- Database connectivity and error handling.
- Razorpay test-mode integration.
- Webhook signature validation.

Only mark a test as passing after it has actually been executed and verified.

---

## ☁️ Deployment

The PayFlow frontend and backend are deployed independently.

### Frontend

The React and Vite frontend is hosted on Vercel.

### Backend

The Spring Boot backend is intended for deployment on a Java-compatible hosting service, such as Render.

### Database

PostgreSQL can be hosted using a managed database service.

### Production Checklist

- Configure all required environment variables.
- Use a managed PostgreSQL instance.
- Configure CORS for the deployed frontend domain.
- Verify the production API base URL.
- Keep credentials and secret keys private.
- Test authentication and payment flows end-to-end.
- Check application logs and health endpoints.
- Keep Razorpay in Test Mode until the integration is fully validated.

---

## 🔮 Future Improvements

Potential improvements for future iterations include:

- Automated API documentation using OpenAPI/Swagger.
- Integration and unit test coverage.
- Database schema migration management.
- Continuous integration with GitHub Actions.
- Structured logging and monitoring.
- Rate limiting and additional API abuse protection.
- Idempotency and reliability improvements for payment operations.
- Deployment automation and production readiness checks.

These items are planned improvements, not claims that every feature is already implemented.

---

## 👨‍💻 Author

**Pratyush Kumar**

GitHub: [Pratyush-95](https://github.com/Pratyush-95)

### Project

**PayFlow — Full-Stack Payment Management Platform**

Built to practice Java backend development, REST API design, database integration, authentication, and payment-gateway workflows.

---

## 📄 License

A license has not yet been specified. Add an appropriate license before granting others permission to reuse, modify, or distribute the code.
