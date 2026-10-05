# Carepoint Clinic

Carepoint is an original sample clinic-management application, using the Petclinic project only as architectural inspiration. It is a Maven multi-module Java 21 / Spring Boot application with an API gateway, a JWT identity service, a clinic service, and a small browser-based sign-in/dashboard UI.

## Project layout

```text
.
├── api-gateway/       # Gateway and static browser UI
├── clinic-service/    # Patient and appointment APIs; Flyway-managed schema
├── identity-service/  # Demo login and short-lived JWT issuance
├── k8s/               # EKS-oriented Kubernetes resources
├── .mvn/wrapper/      # Maven Wrapper distribution configuration
├── mvnw.cmd
└── pom.xml            # Parent build and dependency version management
```

## Prerequisites and local run

- Java 21 or newer. The project compiles against Java 21.
- Internet access on the first Maven Wrapper run, to download Maven and dependencies.

From PowerShell in the project root:

```powershell
.\mvnw.cmd test
```

Start the three services in separate PowerShell windows:

```powershell
.\mvnw.cmd -pl identity-service spring-boot:run
.\mvnw.cmd -pl clinic-service spring-boot:run
.\mvnw.cmd -pl api-gateway spring-boot:run
```

Open <http://localhost:8080>. The local-only demo credentials are `demo` / `change-me-local-only`. The clinic service uses an in-memory H2 database by default, so its data is reset when the service restarts.

To override the local demo credentials and signing key, set `APP_AUTH_USERNAME`, `APP_AUTH_PASSWORD`, and `APP_SECURITY_JWT_SECRET` in the environment of **both** the identity and clinic services. The signing key must be a Base64-encoded random value of at least 32 bytes. For example, generate one in PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## API overview

All requests go through the gateway on port 8080.

| Method | Path | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/login` | Exchange configured demo credentials for a one-hour bearer token | Public |
| `GET` | `/api/clinic/patients` | List patients | Bearer token |
| `POST` | `/api/clinic/patients` | Create a patient | Bearer token |
| `GET` | `/api/clinic/appointments` | List appointments | Bearer token |
| `POST` | `/api/clinic/appointments` | Schedule an appointment for an existing patient | Bearer token |

Example:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' `
  -Body '{"username":"demo","password":"change-me-local-only"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/clinic/patients `
  -Headers $headers -ContentType 'application/json' `
  -Body '{"fullName":"Alex Morgan","email":"alex@example.com","phone":"555-0100"}'
```

The identity service currently authenticates one configured demo user. This intentionally keeps the sample self-contained; it is not a substitute for a production identity provider, persisted user management, password reset, MFA, or key rotation.

## EKS deployment outline

The manifest in `k8s/clinic-platform.yaml` provides namespace-scoped Deployments and Services, health probes, resource requests/limits, an externally exposed gateway, and placeholders for images and sensitive settings. It expects a PostgreSQL database managed outside the cluster (for example, Amazon RDS); it does not provision a database.

Before applying the manifest:

1. Build and push the three service images to ECR (containerization is a follow-up task).
2. Replace the three `REPLACE_WITH_ECR_URI/...` image references.
3. Set the ConfigMap's RDS JDBC URL and configure the RDS database, network access, and TLS policy.
4. Replace the placeholder values in `clinic-secrets`. Use AWS Secrets Manager with the Secrets Store CSI Driver or External Secrets Operator for a real deployment instead of committing credentials to Git.
5. Configure the cluster's AWS Load Balancer Controller / load-balancer policy, DNS, and TLS termination as required by your environment.

Apply and inspect:

```powershell
kubectl apply -f .\k8s\clinic-platform.yaml
kubectl -n clinic get pods,services
kubectl -n clinic logs deployment/clinic-service
```

Do not use the checked-in demo credentials or local signing secret in a shared or production environment. The manifest is a deployment starting point, not a complete production security, networking, observability, backup, or disaster-recovery configuration.

## Tests

`.\mvnw.cmd test` runs the identity login tests and clinic API tests, including authentication, patient creation/listing, and appointment validation. Tests use embedded H2 and do not require AWS, PostgreSQL, Docker, or an EKS cluster.
