# Notification Management Service

A robust notification service built with Spring Boot that handles multiple notification channels including email, SMS (via Twilio), and push notifications (via Firebase).

## Overview

This project provides a centralized notification management system that:
- Processes notification requests from Kafka
- Supports multiple notification channels (email, SMS, push)
- Uses PostgreSQL for data persistence
- Implements circuit breaking with Resilience4j
- Includes scheduled tasks with ShedLock for distributed lock management

## Tech Stack

- **Java 24**
- **Spring Boot 3.4.4**
- **Spring Cloud 2024.0.1**
- **Kafka** for event-driven architecture
- **PostgreSQL** for data storage
- **Firebase Admin** for push notifications
- **Twilio** for SMS delivery
- **Thymeleaf** for email templating
- **Docker** for containerization
- **Resilience4j** for circuit breaking
- **ShedLock** for distributed task scheduling

## Getting Started

### Prerequisites

- JDK 24
- Docker and Docker Compose
- Firebase service account (for push notifications)
- Twilio account for SMS functionality
- SMTP server access for email notifications

### Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/notification-management.git
   cd notification-management
   ```

2. Configure environment variables:
   Create a `.env` file in the `notification-service` directory with the following variables:
   ```
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/notification_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=password
   
   # Kafka
   SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
   SPRING_KAFKA_CONSUMER_GROUP_ID=notification-group
   SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET=earliest
   
   # Email
   SPRING_MAIL_HOST=your-smtp-host
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=your-email
   SPRING_MAIL_PASSWORD=your-password
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
   
   # Twilio
   TWILIO_ACCOUNT_SID=your-twilio-sid
   TWILIO_AUTH_TOKEN=your-twilio-token
   TWILIO_PHONE_NUMBER=your-twilio-phone
   
   # Firebase
   FIREBASE_CONFIG_PATH=/app/firebase-service-account.json
   
   # Application
   SERVER_PORT=8081
   NOTIFICATION_RETRY_MAX_ATTEMPTS=3
   NOTIFICATION_RETRY_INITIAL_INTERVAL=5000
   ```

3. Place your Firebase service account JSON file in the project root as `firebase-service-account.json`

4. Start the services:
   ```
   docker-compose up -d
   ```

### Running Locally for Development

1. Start infrastructure services:
   ```
   docker-compose up -d postgres pgadmin zookeeper kafka
   ```

2. Run the notification service:
   ```
   cd notification-service
   ./mvnw spring-boot:run
   ```

## Architecture

### System Components

The notification service architecture consists of the following components:

1. **API Layer**: REST endpoints for direct notification requests
2. **Kafka Consumers**: Event-driven notification processing
3. **Channel Providers**: Implementation for each notification channel
4. **Persistence Layer**: Database storage for notification tracking
5. **Scheduler**: Retry mechanism for failed notifications

### Data Flow

1. Notification requests arrive via REST API or Kafka topics
2. Requests are validated and transformed into internal notification models
3. The notification dispatcher routes to the appropriate channel provider
4. Providers attempt delivery and report success/failure
5. Results are persisted to the database
6. Failed notifications are scheduled for retry

### Notification Channels

#### Email
- Uses Spring Mail with Thymeleaf templates
- Supports HTML formatting and attachments
- Tracks delivery status

#### SMS (Twilio)
- Integrates with Twilio API for SMS delivery
- Supports international phone numbers
- Provides delivery status tracking

#### Push Notifications (Firebase)
- Uses Firebase Cloud Messaging
- Supports tokens and topics
- Handles rich notifications with actions

## API Documentation

### Notification Endpoints

#### Create Notification

```
POST /api/v1/notifications
```

Request body:
```json
{
  "recipient": "user@example.com",
  "channel": "EMAIL",
  "subject": "Important Notification",
  "content": "This is an important message",
  "metadata": {
    "priority": "HIGH",
    "category": "ALERT"
  },
  "templateId": "welcome-template",
  "templateData": {
    "userName": "John Doe",
    "activationLink": "https://example.com/activate"
  }
}
```

#### Get Notification Status

```
GET /api/v1/notifications/{id}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DELIVERED",
  "sentAt": "2025-04-17T14:32:22Z",
  "deliveredAt": "2025-04-17T14:32:25Z",
  "failureReason": null,
  "retryCount": 0
}
```

## Database Schema

The service uses the following main tables:

1. **notifications**: Stores all notification records
2. **notification_templates**: Email and push notification templates
3. **notification_preferences**: User channel preferences
4. **notification_delivery_attempts**: Tracks delivery attempts and failures
5. **shedlock**: Used by ShedLock for distributed locking

## Kafka Topics

The service consumes from the following Kafka topics:

1. `notification.email.requests` - For email notifications
2. `notification.sms.requests` - For SMS notifications
3. `notification.push.requests` - For push notifications

And produces to:

1. `notification.status.updates` - For notification status changes

## Database Access

PgAdmin is available at http://localhost:8080 with:
- Email: admin@example.com
- Password: admin

## Monitoring and Observability

The service provides the following endpoints for monitoring:

1. `/actuator/health` - Health check endpoint
2. `/actuator/metrics` - Metrics endpoint
3. `/actuator/prometheus` - Prometheus metrics

## Deployment

### Docker Deployment

The included Docker Compose file sets up the complete environment. For production, consider:

1. Using separate database credentials
2. Setting up proper Kafka security
3. Configuring SSL for all services
4. Using Docker Swarm or Kubernetes for orchestration

### Kubernetes Deployment

Sample Kubernetes manifests are available in the `k8s/` directory, including:
- Deployments
- Services
- ConfigMaps
- Secrets
- Ingress rules

## Configuration Options

The service is highly configurable through application properties:

```properties
# Notification settings
notification.default-expiry=24h
notification.batch-size=100
notification.channels.email.enabled=true
notification.channels.sms.enabled=true
notification.channels.push.enabled=true

# Circuit breaker settings
resilience4j.circuitbreaker.instances.emailService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.emailService.waitDurationInOpenState=10000

# ShedLock configuration
shedlock.defaultLockAtMostFor=10m
```

### Development Guidelines

- Follow the Google Java Style Guide
- Write unit tests for all new features
- Update documentation when changing functionality
- Create issues for bugs and feature requests

## License

This project is licensed under the MIT - see the LICENSE file for details.

## Acknowledgments

- Spring Boot team for the excellent framework
- Confluent for Kafka images
- Twilio for SMS capabilities
- Firebase for push notification support
- The open source community for various libraries used in this project
