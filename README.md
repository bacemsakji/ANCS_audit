# ANCS Audit System

A comprehensive audit management platform for the Tunisian National Cybersecurity Agency (ANCS) that manages technical security audits for organizations.

## 🏗️ Architecture

### Backend
- **Framework:** Spring Boot 3.3.1 (Java 21)
- **Port:** 8081 (configurable)
- **Database:** PostgreSQL 16
- **Storage:** MinIO (object storage for reports and evidence)
- **AI:** Ollama (local AI for executive summary generation)

### Frontend
- **Framework:** Flutter (mobile/web)
- **Navigation:** GoRouter
- **HTTP Client:** Dio
- **State Management:** BLoC pattern

### Infrastructure
- **Docker Compose:** PostgreSQL, MinIO, pgAdmin
- **Development:** Backend and Ollama run locally for easier debugging

## ✨ Features

### Mission Management
- Create and schedule audit missions
- Assign auditors to organizations
- Track mission status (planned, in progress, completed, cancelled)
- View mission details and assigned auditors

### Audit Checklists
- Technical control checklists based on security standards
- Record findings (conforme/non-conforme/observation)
- Add comments and upload evidence images
- Offline support with automatic synchronization
- Real-time progress tracking

### Report Generation
- **AI-Powered Executive Summary:** Automatic generation using Ollama/Mistral
- **Export Formats:** DOCX and PDF
- **Secure Download:** Pre-signed URLs via MinIO
- **Data Sovereignty:** Local AI ensures data stays in Tunisia

### Role-Based Access Control
- **ADMIN_ANCS:** Full access, create missions, view reports
- **AUDITEUR:** Perform audits, generate reports, complete missions
- **RSSI:** View dashboard for their organization only

### Dashboards
- **Admin Dashboard:** Overall statistics, mission tracking, certification alerts
- **RSSI Dashboard:** Organization-specific audit results and corrective actions

## 🛠️ Tech Stack

### Backend
- Spring Boot 3.3.1
- Spring Data JPA
- Spring Security with JWT
- Flyway for database migrations
- PostgreSQL 16
- MinIO for object storage
- Ollama for AI services
- MapStruct for object mapping
- Lombok for boilerplate reduction

### Frontend
- Flutter 3.x
- GoRouter for navigation
- Dio for HTTP requests
- BLoC for state management
- SQLite for offline storage
- url_launcher for file downloads

### Development Tools
- Docker Compose
- Maven
- Git
- pgAdmin (database administration)

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- Flutter 3.x
- Docker Desktop
- Node.js (for some tools)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Audit
   ```

2. **Start Docker services**
   ```bash
   cd ancs-audit-backend
   docker-compose up -d
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Start the backend**
   ```bash
   cd ancs-audit-backend
   mvn spring-boot:run
   ```

5. **Install Ollama and download model**
   ```bash
   # Install Ollama from https://ollama.ai
   ollama pull mistral
   ```

6. **Start the Flutter app**
   ```bash
   cd ancs-audit-mobile
   flutter pub get
   flutter run
   ```

### Default Credentials

**Admin:**
- Email: `admin.demo@ancs.gov.tn`
- Password: `Admin@ANCS2024!`

**Auditeur:**
- Email: `auditeur.demo@ancs.gov.tn`
- Password: `Auditeur@ANCS2024!`

**RSSI:**
- Email: `rssi.demo@bnt.com.tn`
- Password: `Rssi@ANCS2024!`

**MinIO:**
- Username: `ancs_minio_admin`
- Password: `ancs_minio_secret_dev`
- Console: http://localhost:9001

**pgAdmin:**
- Email: `admin@ancs.gov.tn`
- Password: `ancs_pgadmin_dev`
- URL: http://localhost:5050

## 📖 Usage

### Creating a Mission (Admin)
1. Login as admin
2. Navigate to "Missions"
3. Click "Créer une mission"
4. Fill in mission details (organization, auditor, dates, scope)
5. Save the mission

### Performing an Audit (Auditeur)
1. Login as auditeur
2. Select a mission from the list
3. Click "Commencer l'audit"
4. Go through each control item
5. Select result (conforme/non-conforme/observation)
6. Add comments and upload evidence
7. Click "Enregistrer" for each item
8. Click checkmark icon to complete the mission

### Generating Reports (Auditeur)
1. Complete the audit checklist
2. Click "Générer le rapport" button
3. Optionally use AI to generate executive summary
4. Compile the final report
5. Download the generated document

### Viewing Dashboard (RSSI)
1. Login as RSSI
2. View organization-specific audit results
3. Check corrective actions status
4. Monitor compliance scores

## 🔧 Development

### Backend Development
```bash
cd ancs-audit-backend
mvn spring-boot:run
```

### Frontend Development
```bash
cd ancs-audit-mobile
flutter run
```

### Database Migrations
```bash
# Flyway automatically runs migrations on startup
# To add new migration:
# Create file in src/main/resources/db/migration/V{version}__description.sql
```

### Running Tests
```bash
# Backend
cd ancs-audit-backend
mvn test

# Frontend
cd ancs-audit-mobile
flutter test
```

## 📁 Project Structure

```
Audit/
├── ancs-audit-backend/          # Spring Boot backend
│   ├── src/main/java/
│   │   └── tn/gov/ancs/audit/
│   │       ├── controller/      # REST controllers
│   │       ├── service/         # Business logic
│   │       ├── repository/      # Data access
│   │       ├── domain/         # Entities
│   │       ├── dto/            # Data transfer objects
│   │       └── config/         # Configuration
│   ├── src/main/resources/
│   │   ├── db/migration/       # Flyway migrations
│   │   └── application.yml     # Configuration
│   └── docker-compose.yml      # Docker services
├── ancs-audit-mobile/           # Flutter frontend
│   ├── lib/
│   │   ├── features/            # Feature modules
│   │   │   ├── auth/           # Authentication
│   │   │   ├── missions/       # Mission management
│   │   │   ├── checklist/      # Audit checklist
│   │   │   ├── rapports/       # Report generation
│   │   │   └── dashboard/      # Dashboards
│   │   ├── core/               # Shared utilities
│   │   └── main.dart           # App entry point
│   └── pubspec.yaml             # Flutter dependencies
└── README.md                    # This file
```

## 🔐 Security

- **Authentication:** JWT tokens with 2FA for admins
- **Authorization:** Role-based access control (RBAC)
- **Data Sovereignty:** Local AI (Ollama) ensures data stays in Tunisia
- **Secure Storage:** MinIO with private bucket policies
- **Audit Logging:** All sensitive operations are logged
- **CORS:** Configured for specific origins only

## 🌐 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/verify-2fa` - 2FA verification

### Missions
- `GET /api/missions` - List missions
- `GET /api/missions/{id}` - Get mission details
- `POST /api/missions` - Create mission (admin only)
- `PATCH /api/missions/{id}/statut` - Update mission status

### Reports
- `POST /api/rapports/missions/{missionId}/synthese-ia` - Generate AI summary
- `POST /api/rapports/generate` - Generate report document
- `GET /api/rapports/{id}/download` - Download report

### Dashboard
- `GET /api/dashboard/admin` - Admin dashboard
- `GET /api/dashboard/rssi` - RSSI dashboard

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is proprietary software owned by the Tunisian National Cybersecurity Agency (ANCS).

## 👥 Team

- ANCS Development Team

## 📞 Support

For support, please contact the ANCS IT department.

## 🔄 Version History

- **1.0.0** - Initial release
  - Mission management
  - Audit checklists
  - Report generation
  - AI-powered summaries
  - Role-based dashboards
