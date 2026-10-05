# Carepoint Clinic

Carepoint is an original sample clinic-management application, using the Petclinic project only as architectural inspiration. It is a Maven multi-module Java 21 / Spring Boot application with an API gateway, a JWT identity service, a clinic service, and a small browser-based sign-in/dashboard UI.

## Project layout

```text
.
├── api-gateway/       # Gateway and static browser UI
│   └── Dockerfile
├── clinic-service/    # Patient and appointment APIs; Flyway-managed schema
│   └── Dockerfile
├── identity-service/  # Demo login and short-lived JWT issuance
│   └── Dockerfile
├── k8s/
│   ├── base/
│   │   ├── api-gateway/
│   │   ├── clinic-service/
│   │   ├── common/
│   │   ├── identity-service/
│   │   └── postgres/
│   └── overlays/eks/  # ECR image names/tags for EKS
├── .github/workflows/ # CI, image publishing, and EKS deployment
├── .dockerignore
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

## Container architecture

Build three separate images from the repository root. Each Dockerfile uses a Maven build stage and a smaller Java 21 runtime stage; the app containers run as a non-root user. Keep the images separate so each service can be released, scaled, rolled back, and granted configuration independently. The gateway serves the browser UI; it does not contain either backend service.

```powershell
docker build -f .\identity-service\Dockerfile -t carepoint-identity-service:0.1.0 .
docker build -f .\clinic-service\Dockerfile -t carepoint-clinic-service:0.1.0 .
docker build -f .\api-gateway\Dockerfile -t carepoint-api-gateway:0.1.0 .
```

The clinic service owns the patient/appointment data and schema migrations. The identity service is currently stateless and uses environment-configured demo credentials; it does not need a database. PostgreSQL is its own StatefulSet and persistent volume, not bundled into any application image.

## EKS deployment outline

Kubernetes resources are split by ownership: `k8s/base/api-gateway/`, `k8s/base/clinic-service/`, `k8s/base/identity-service/`, and `k8s/base/postgres/` each contain their own Kustomize configuration, Deployment/StatefulSet and Services. Shared settings are in `k8s/base/common/`. `k8s/overlays/eks/` changes the application image names and tags to ECR without editing base manifests. PostgreSQL initializes a non-superuser `clinic_app` role for the clinic service; the separate `clinic_admin` account is only used by the database container during initialization.

Provision EKS and its EBS CSI-backed default StorageClass first. For a local/dev cluster, create the namespace, copy `k8s/base/common/clinic-secrets.example.yaml`, replace every placeholder with real development values, and apply that Secret before the platform. Never commit the populated Secret. For shared environments, source the Secret from AWS Secrets Manager using an External Secrets Operator or another managed-secret integration. The manifest includes a single-replica PostgreSQL StatefulSet with a 20 GiB persistent volume claim; this is for development/learning, not highly available production storage.

```sh
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f /path/to/your/clinic-secrets.yaml
```

For manual deployments, push the three images, set the image names and tags under `k8s/overlays/eks/kustomization.yaml`, render/apply the overlay, and wait for rollouts:

```sh
kubectl apply -k k8s/overlays/eks
kubectl -n clinic rollout status statefulset/postgres --timeout=10m
kubectl -n clinic rollout status deployment/identity-service --timeout=5m
kubectl -n clinic rollout status deployment/clinic-service --timeout=5m
kubectl -n clinic rollout status deployment/api-gateway --timeout=5m
kubectl -n clinic get pods,pvc,services
```

The PostgreSQL initialization script runs only when the persistent volume is first initialized. Rotating either database password later requires a coordinated PostgreSQL role update and Secret rollout; changing a Kubernetes Secret alone does not change credentials already stored in PostgreSQL. The PVC is retained when the StatefulSet is deleted or scaled down, but that is not a backup. For production, prefer Amazon RDS/Aurora with Multi-AZ, automated backups and restore testing, encryption, monitoring, and managed credential rotation. Configure the AWS Load Balancer Controller, DNS, TLS termination, network policies, and cluster access for your environment. These manifests are a starting point, not a complete production security, networking, observability, backup, or disaster-recovery configuration.

## GitHub Actions CI/CD

`.github/workflows/ci-cd.yml` runs `mvn verify` (including Google Java Style Checkstyle and unit tests), renders the EKS Kustomize overlay, validates the Kubernetes schemas with kubeconform, and lints the workflow on pull requests and pushes to `main`. ESLint is not used because the UI is plain JavaScript without a Node build; Java Checkstyle is the relevant source-style check. A push to `main` then builds three independent service images, tags each with the full Git commit SHA plus the GitHub run ID/attempt (unique for safe retries), publishes SBOM/provenance to ECR, and blocks deployment when Trivy reports a fixable HIGH or CRITICAL image vulnerability. After all image scans pass (and SonarCloud passes when enabled), the workflow updates the rendered EKS overlay to those exact image tags and waits for database/application rollouts. Pull requests never receive AWS credentials or publish images.

Configure these **repository variables** before enabling delivery:

| Variable | Purpose |
|---|---|
| `AWS_REGION` | AWS region containing ECR and EKS |
| `EKS_CLUSTER_NAME` | Target EKS cluster |
| `ECR_PUBLISH_ROLE_ARN` | OIDC role limited to pushing to the three ECR repositories |
| `EKS_DEPLOY_ROLE_ARN` | Separate OIDC role allowed to update this EKS cluster |
| `SONAR_ENABLED` | Set to `true` to require SonarCloud analysis; omit/leave unset to skip |
| `SONAR_ORGANIZATION` | SonarCloud organization, when enabled |
| `SONAR_PROJECT_KEY` | SonarCloud project key, when enabled |

When SonarCloud is enabled, add `SONAR_TOKEN` as a **repository secret**. Create the ECR repositories `carepoint-api-gateway`, `carepoint-clinic-service`, and `carepoint-identity-service` in advance; configure immutable tags and ECR vulnerability scanning. Configure each AWS role's GitHub OIDC trust policy to this repository and its required branch/environment subject, and grant only the ECR push or EKS deployment permissions it needs. Enable the EKS access entry/RBAC mapping for the deployment role. The workflow targets the GitHub `production` environment; configure required reviewers there if deployments should require approval.

The workflow expects the Kubernetes `clinic-secrets` Secret and storage prerequisites to be provisioned **before** the first deployment. It does not put credentials in GitHub Actions or create sample secrets in the cluster. Protect `main` with required pull-request checks. Third-party GitHub Actions are pinned to full commit SHAs; periodically review and update those pins, preferably using Dependabot or Renovate pull requests. Keep the kubeconform Kubernetes version aligned with the EKS cluster version you select.

For an application rollback, use the deployment's previous image revision (or `kubectl -n clinic rollout undo deployment/<service>` for a quick operational rollback). Database schema changes need backward-compatible migrations; rolling back an application image does not roll back a Flyway schema migration.

## Tests

`.\mvnw.cmd test` runs the identity login tests and clinic API tests, including authentication, patient creation/listing, and appointment validation. Tests use embedded H2 and do not require AWS, PostgreSQL, Docker, or an EKS cluster.
