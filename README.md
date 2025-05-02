# Jobs Search Portal

A comprehensive job search and application management system built with Spring Boot, providing a secure platform for job seekers and employers.

## Overview

Jobs Search Portal is a RESTful web application that allows users to search for jobs, apply to positions, and manage their applications. Employers can post job listings, manage their company profiles, and track applicants. The system features secure authentication using JWT tokens and role-based access control.

## Features

- **User Management**

  - User registration and authentication
  - Role-based access control (Job Seekers, Employers, Admins)
  - Secure password handling
  - JWT token-based authentication with refresh token support

- **Job Management**

  - Create, read, update, and delete job listings
  - Search jobs by title, location, or company
  - Pagination support for job listings
  - Job expiration date tracking

- **Company Profiles**

  - Company registration and profile management
  - Company job listings

- **Application Processing**

  - Apply for jobs
  - Track application status (APPLIED, REVIEWING, REJECTED, ACCEPTED)
  - Application history for users

- **Security**
  - JWT authentication
  - Redis-based token management
  - Password encryption
  - Role-based authorization

## Tech Stack

- **Backend Framework**: Spring Boot 3.4.4
- **Java Version**: Java 17
- **Database**: PostgreSQL
- **Cache**: Redis
- **Security**: Spring Security with JWT
- **Build Tool**: Maven
- **Data Validation**: Spring Validation
- **Object Mapping**: ModelMapper

## Project Structure

```
src/main/java/com/ak/
├── config/                  # Configuration classes
│   ├── ApplicationConfig.java
│   ├── JwtFilter.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/              # REST API controllers
│   ├── ApplicationController.java
│   ├── AuthController.java
│   ├── CompanyController.java
│   ├── JobController.java
│   └── UserController.java
├── dto/                     # Data Transfer Objects
│   ├── ApiResponse.java
│   ├── ApplicationDto.java
│   ├── CompanyDto.java
│   └── ...
├── entity/                  # JPA entities
│   ├── Application.java
│   ├── Company.java
│   ├── Job.java
│   ├── Role.java
│   ├── Status.java
│   └── Users.java
├── exception/               # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedAccessException.java
├── repository/              # Spring Data JPA repositories
│   ├── ApplicationRepository.java
│   ├── CompanyRepository.java
│   ├── JobRepository.java
│   └── UserRepository.java
├── service/                 # Business logic services
│   ├── ApplicationService.java
│   ├── AuthService.java
│   ├── CompanyService.java
│   ├── JobService.java
│   ├── RedisService.java
│   └── UserService.java
└── utils/                   # Utility classes
    ├── ApplicationMapper.java
    ├── CompanyMapper.java
    ├── JobMapper.java
    └── JwtUtils.java
```

## API Endpoints

### Authentication

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/refresh` - Refresh JWT token

### Users

- `GET /api/users` - Get all users (Admin only)
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Jobs

- `GET /api/jobs` - Get all jobs (paginated)
- `GET /api/jobs/{id}` - Get job by ID
- `POST /api/jobs` - Create new job (Employer only)
- `PUT /api/jobs/{id}` - Update job (Employer only)
- `DELETE /api/jobs/{id}` - Delete job (Employer only)
- `GET /api/jobs/search` - Search jobs by title, location, or company

### Companies

- `GET /api/companies` - Get all companies
- `GET /api/companies/{id}` - Get company by ID
- `POST /api/companies` - Create new company (Employer only)
- `PUT /api/companies/{id}` - Update company (Employer only)
- `DELETE /api/companies/{id}` - Delete company (Employer only)

### Applications

- `GET /api/applications` - Get all applications (Admin only)
- `GET /api/applications/{id}` - Get application by ID
- `POST /api/applications` - Create new application (Job Seeker only)
- `PUT /api/applications/{id}` - Update application status (Employer only)
- `GET /api/applications/user/{userId}` - Get applications by user ID
- `GET /api/applications/job/{jobId}` - Get applications by job ID

## Setup and Installation

### Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Redis

### Database Configuration

Configure your PostgreSQL database in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/learning
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

### Redis Configuration

Configure Redis for token management:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### JWT Configuration

Configure JWT secrets and expiration times:

```properties
jwt.secret=your-jwt-secret-key
jwt.expiration.ms=86400000
refresh.jwt.secret=your-refresh-token-secret
refresh.jwt.expiration.ms=259200000
```

### Building and Running

1. Clone the repository
2. Navigate to the project directory
3. Build the project: `mvn clean install`
4. Run the application: `mvn spring-boot:run`

The application will start on port 8081 by default.

## Security Implementation

The application uses Spring Security with JWT for authentication:

- JWT tokens are issued upon successful login
- Refresh tokens are stored in Redis for efficient token management
- Role-based access control restricts endpoints based on user roles
- Password encryption ensures secure storage of credentials

## Future Enhancements

- Email notifications for application status changes
- Resume upload and management
- Advanced job search filters
- Interview scheduling
- Analytics dashboard for employers
- Mobile application support

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

[Your Name]

## Acknowledgments

- Spring Boot and Spring Security teams
- PostgreSQL and Redis communities
- All contributors to the project
