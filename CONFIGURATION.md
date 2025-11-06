# Backend Configuration Guide

## 🔐 Security-First Configuration

This project uses environment variables for all sensitive configuration to prevent hardcoding secrets in the codebase.

## 📋 Quick Start

### 1. Copy Environment Template
```bash
cd backend-java
cp .env.example .env
```

### 2. Edit `.env` File
Open `.env` and update with your actual values:

```bash
# REQUIRED: Change these values
DATABASE_PASSWORD=your_actual_db_password
JWT_SECRET=your_actual_jwt_secret_min_32_chars

# Optional: Customize if needed
DATABASE_USERNAME=your_db_username
DATABASE_URL=jdbc:postgresql://localhost:5432/langapp
```

### 3. Generate Secure JWT Secret
```bash
# Option 1: Using OpenSSL
openssl rand -base64 32

# Option 2: Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Option 3: Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Copy the output and use it as your `JWT_SECRET`.

## 🗂️ Configuration Files Structure

```
backend-java/src/main/resources/
├── application.yml           # Base config (uses env vars)
├── application-dev.yml       # Development overrides
└── application-prod.yml      # Production overrides
```

### Base Configuration (`application.yml`)
- Uses environment variables with sensible defaults
- All sensitive data comes from env vars
- Profile-independent settings

### Development Profile (`application-dev.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update        # Auto-update schema (dev only!)
    show-sql: true            # Log SQL queries
```

**Active by default** when `SPRING_PROFILES_ACTIVE=dev` (or not set)

### Production Profile (`application-prod.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate      # Never auto-update in production
    show-sql: false           # Don't log queries
server:
  error:
    include-message: never    # Don't expose errors
    include-stacktrace: never # Don't expose stack traces
```

**Use in production** by setting `SPRING_PROFILES_ACTIVE=prod`

## 🚀 Running the Application

### Development Mode
```bash
# Default: uses dev profile
mvn spring-boot:run

# Or explicitly
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

### Production Mode
```bash
# Set environment variables first!
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_PASSWORD=your_prod_password
export JWT_SECRET=your_prod_jwt_secret

# Run
mvn spring-boot:run
```

### Using IDE (IntelliJ IDEA)
1. Edit Run Configuration
2. Add Environment Variables:
   ```
   SPRING_PROFILES_ACTIVE=dev
   DATABASE_PASSWORD=your_password
   JWT_SECRET=your_jwt_secret
   ```
3. Run

## 🔧 Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | `abc123...` |
| `DATABASE_PASSWORD` | PostgreSQL password | `secure_pwd` |

### Optional Variables (Have Defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/langapp` | Database connection URL |
| `DATABASE_USERNAME` | `kihoon` | Database username |
| `SERVER_PORT` | `3000` | Server port |
| `JWT_EXPIRATION` | `604800000` | Token expiration (7 days in ms) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed CORS origins |

## 🛡️ Security Best Practices

### ✅ DO
- ✅ Use `.env` for local development
- ✅ Generate unique JWT secrets for each environment
- ✅ Use strong database passwords (12+ chars, mixed case, numbers, symbols)
- ✅ Set `SPRING_PROFILES_ACTIVE=prod` in production
- ✅ Keep `.env` in `.gitignore` (already configured)
- ✅ Use environment variables in CI/CD pipelines
- ✅ Rotate JWT secrets periodically

### ❌ DON'T
- ❌ Never commit `.env` file to git
- ❌ Never hardcode secrets in code
- ❌ Never use dev profile in production
- ❌ Never expose stack traces in production
- ❌ Never use weak/default secrets

## 🐳 Docker Deployment

```dockerfile
# Example Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/*.jar app.jar

# Environment variables will be passed at runtime
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Run with environment variables:**
```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://db-host:5432/langapp \
  -e DATABASE_USERNAME=prod_user \
  -e DATABASE_PASSWORD=prod_password \
  -e JWT_SECRET=your_prod_jwt_secret \
  -p 3000:3000 \
  langapp-backend
```

## ☁️ Cloud Deployment

### AWS Elastic Beanstalk
Set environment variables in EB Console:
- Configuration → Software → Environment properties

### Heroku
```bash
heroku config:set JWT_SECRET=your_secret
heroku config:set DATABASE_PASSWORD=your_password
heroku config:set SPRING_PROFILES_ACTIVE=prod
```

### Kubernetes
Use ConfigMaps and Secrets:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: langapp-secrets
type: Opaque
data:
  jwt-secret: base64_encoded_secret
  db-password: base64_encoded_password
```

## 🔍 Troubleshooting

### "JWT_SECRET must be specified"
**Cause**: `JWT_SECRET` environment variable not set
**Fix**: Set `JWT_SECRET` in `.env` or export it

### "Could not connect to database"
**Cause**: Database credentials incorrect or DB not running
**Fix**:
1. Check PostgreSQL is running
2. Verify `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`

### "Profile 'prod' not found"
**Cause**: Trying to use production profile without configuration
**Fix**: Use `dev` profile or properly configure production environment

## 📚 Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

## 🆘 Support

For issues or questions:
1. Check this guide first
2. Review `.env.example` for required variables
3. Verify environment variables are set correctly
4. Check application logs for specific error messages
