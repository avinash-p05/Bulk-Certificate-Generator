Bulk Certificate Generator
A Spring Boot application that automates the generation and distribution of certificates using custom templates and MongoDB for data storage.
Features

Custom certificate template support
Bulk user data import via CSV
Custom font support
Automated email distribution
Failed delivery tracking and retry mechanism
Email delivery statistics

Prerequisites

Java 17 or higher
Maven
MongoDB
SMTP server configuration for email sending

Project Structure
Copysrc/
├── main/
│   ├── java/
│   │   └── com/avalanche/Certificates_Distribution/
│   │       ├── controllers/
│   │       ├── models/
│   │       ├── services/
│   │       └── CertificatesDistributionApplication.java
│   └── resources/
│       ├── templates/    # Place certificate templates here
│       ├── fonts/        # Place custom fonts here
│       └── application.properties
Setup

Clone the repository:

bashCopygit clone <repository-url>
cd bulk-certificate-generator

Configure application.properties:

propertiesCopy# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/certificates

# Email Configuration
spring.mail.host=smtp.your-email-server.com
spring.mail.port=587
spring.mail.username=your-username
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

Add certificate templates:

Place your certificate template files in src/main/resources/templates/
Supported format: PDF (with text placeholders for dynamic content)


Add custom fonts (optional):

Place your font files in src/main/resources/fonts/
Supported formats: TTF, OTF



Database Schema
The application uses MongoDB with the following user document structure:
jsonCopy{
  "_id": ObjectId,
  "username": "String",
  "email": "String",
  "usn": "String",
  "emailSent": Boolean,
  "_class": "com.avalanche.Certificates_Distribution.models.User"
}
API Endpoints
Import Users
httpCopyPOST /api/certificates/import
Content-Type: multipart/form-data

Accepts CSV file with user data
Required CSV columns: username, email, usn

Generate and Send Certificates
httpCopyPOST /api/certificates/send

Generates certificates for all users
Sends certificates via email
Returns count of successfully sent certificates

Get Statistics
httpCopyGET /api/certificates/stats

Returns email delivery statistics

Get Failed Deliveries
httpCopyGET /api/certificates/failed

Returns list of users where email delivery failed

Retry Failed Emails
httpCopyPOST /api/certificates/retry-failed

Retries sending certificates to failed email addresses

Error Handling
The application includes comprehensive error handling for:

Invalid CSV files
Email sending failures
Database connectivity issues
Template processing errors

Building and Running

Build the project:

bashCopymvn clean install

Run the application:

bashCopyjava -jar target/certificates-distribution-1.0.0.jar
Contributing

Fork the repository
Create a feature branch
Commit your changes
Push to the branch
Create a Pull Request
