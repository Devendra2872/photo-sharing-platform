# PhotoShare — Event Photo Gallery Platform

A full-stack photo-sharing application for photography/event teams. Admins create events, assign team members, review uploads, and publish PIN-protected customer galleries.

## Live Demo

| Resource | Value |
|---|---|
| App URL | http://56.228.33.153 |
| Admin | admin@demo.com / admin123 |
| Team Member | member@demo.com / member123 |
| Gallery | Publish from admin UI after selecting photos |

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security, JWT |
| Frontend | React 19, TypeScript, Vite, React Router |
| Database | PostgreSQL 16 (AWS RDS in production) |
| File Storage | Local filesystem (dev) or AWS S3 (production) |
| Deployment | Docker Compose on AWS EC2 |

## Architecture

```
                            Browser (customer / admin / team member)
                                          │
                                          ▼
                          ┌───────────────────────────┐
                          │   EC2 instance (Ubuntu)    │
                          │                             │
                          │  ┌───────────────────────┐ │
                          │  │ nginx (frontend:80)    │ │
                          │  │  - serves React build  │ │
                          │  │  - proxies /api/* ────┼─┼──┐
                          │  └───────────────────────┘ │  │
                          │                             │  ▼
                          │  ┌───────────────────────┐ │
                          │  │ Spring Boot (8080)     │◄┘
                          │  └──────────┬────────────┘ │
                          └─────────────┼───────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
            ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
            │  RDS Postgres  │   │  S3 (private)  │   │  JWT (signed)  │
            │  (metadata)    │   │  (photo files)  │   │  auth + gallery│
            └───────────────┘   └───────────────┘   │  session tokens│
                                                      └───────────────┘
```

Both frontend and backend run as Docker containers on a single EC2 instance, orchestrated with Docker Compose. nginx serves the built React app and reverse-proxies `/api/*` to the backend container internally — so the browser only ever talks to one origin, avoiding CORS for the app's own API calls.

### Security architecture: PIN → session token → file access

Gallery access uses three layered controls, each with its own scope and lifetime:

1. **PIN verification** — customer submits the 6-digit PIN (bcrypt-hashed, never stored in plaintext) against a specific gallery slug.
2. **Gallery session token** — on success, a JWT is issued with the gallery slug embedded as the subject claim and a 1-hour expiry. Every subsequent request to view photos must present this token, and the backend verifies the token's slug matches the gallery being requested (preventing a token issued for one gallery from being reused on another).
3. **Presigned S3 URLs** — the S3 bucket is fully private (no public access). Photo URLs returned to the browser are presigned by the backend with a 15-minute expiry, generated fresh on every gallery view. This means a raw photo URL, even if copied and shared outside the app, becomes useless shortly after.

This layered design means the PIN alone doesn't grant permanent access, and no photo URL works indefinitely if leaked.

## Database Schema

```
users ──────────────┐
  id, email,        │
  password_hash,    │
  name, role        │
                    ▼
events ◄──── event_members ──── users (team members)
  id, name,
  description,
  created_by

photos
  id, event_id, uploaded_by,
  filename, storage_key,
  file_size, content_type,
  selected_for_gallery

galleries
  id, event_id, slug,
  pin_hash, published, published_at
```

## Features

**Admin / Lead**
- Register and login
- Create events and add team members
- View all uploaded photos
- Select photos for the customer gallery
- Publish gallery with a 6-digit PIN
- Generate shareable gallery link

**Team Member**
- Login and view assigned events
- Upload multiple photos
- View own uploads only
- Cannot publish galleries or manage other users' photos

**Customer (no account)**
- Open gallery link (`/gallery/{slug}`)
- Enter PIN to access published photos
- Browse gallery with lightbox view
- Download individual photos

## Local Setup

### Prerequisites
- Java 21+
- Node.js 20+
- Docker (for PostgreSQL) or a local PostgreSQL instance

### 1. Start PostgreSQL
```bash
docker compose up postgres -d
```

### 2. Backend
```bash
cd backend
cp ../.env.example ../.env   # optional — defaults work for local dev
mvn spring-boot:run
```
The API starts at `http://localhost:8080`. Demo users are seeded on first run.

### 3. Frontend
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```
Open `http://localhost:5173`.

### Run everything with Docker (local dev)
```bash
docker compose up --build
```
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`

## Environment Variables

See `.env.example` for all variables. Key settings:

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/photoshare` |
| `JWT_SECRET` | JWT signing secret (32+ chars) | — |
| `STORAGE_TYPE` | `local` or `s3` | `local` |
| `S3_BUCKET` | AWS S3 bucket name | — |
| `S3_REGION` | AWS region for the bucket | `us-east-1` |
| `FRONTEND_URL` | Used for gallery link generation | `http://localhost:5173` |
| `CORS_ORIGINS` | Allowed frontend origins | `http://localhost:5173` |

Never commit secrets to Git. `backend/.env` and `frontend/.env` are gitignored; only `.env.example` files (placeholder values) are tracked.

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register user |
| POST | `/api/auth/login` | Public | Login |
| GET | `/api/auth/me` | JWT | Current user |
| GET/POST | `/api/events` | JWT | List/create events |
| GET | `/api/events/{id}` | JWT | Event details |
| POST | `/api/events/{id}/members` | Admin | Add team member |
| POST | `/api/events/{id}/photos` | JWT | Upload photos |
| GET | `/api/events/{id}/photos` | JWT | List photos |
| PUT | `/api/events/{id}/photos/select` | Admin | Select photos |
| POST | `/api/events/{id}/gallery/publish` | Admin | Publish gallery |
| POST | `/api/public/gallery/{slug}/access` | Public | Verify PIN, issue gallery token |
| GET | `/api/public/gallery/{slug}/photos` | Gallery token | List published photos (presigned URLs) |

## Testing

```bash
cd backend
mvn test
```

7 integration tests, all passing (`BUILD SUCCESS`). Tests run against an isolated H2 in-memory database and local file storage — no dependency on RDS, S3, or any AWS resource.

Coverage:
- Authentication (invalid credentials rejected)
- Authorization (team member cannot create events or publish galleries)
- Event access control (members cannot access unassigned events)
- Full gallery workflow (upload → select → publish → PIN access → view photos)
- Incorrect PIN rejection

## Deployment (AWS)

This app is deployed on AWS using RDS, S3, and a single EC2 instance running both containers via Docker Compose.

### 1. RDS (PostgreSQL)
- Created a `db.t3.micro` PostgreSQL instance (free tier), not publicly accessible
- Security group allows inbound port 5432 only from the EC2 instance's security group

### 2. S3 (photo storage)
- Created a private bucket (all public access blocked)
- IAM user with an inline policy scoped to `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on that bucket only
- CORS configured on the bucket to allow `GET` from the app's origin, so browser-side photo downloads work
- Backend generates presigned URLs (15-minute expiry) for all photo access — the bucket itself is never public

### 3. EC2 (application server)
- Ubuntu 24.04 LTS, `t3.micro`
- Docker + Docker Compose installed
- Security group: SSH (22, restricted), HTTP (80, public), HTTPS (443, public, for future TLS)
- Repository cloned via `git clone`; `backend/.env` created directly on the server with production values (RDS endpoint, S3 credentials, JWT secret) — never committed to Git

### 4. Running the stack
```bash
docker compose -f docker-compose.prod.yml up -d --build
```
`docker-compose.prod.yml` runs only the backend and frontend containers (no local Postgres — the backend connects to RDS). The frontend build takes `VITE_API_URL=/api` so it calls the backend through nginx's internal reverse proxy rather than an absolute address, meaning the app doesn't need rebuilding if the server's IP changes.

### Steps to reproduce
1. Provision RDS, note the endpoint
2. Create S3 bucket + IAM user, note bucket name and credentials
3. Launch EC2, install Docker
4. `git clone` this repository onto EC2
5. Create `backend/.env` on the server with real RDS/S3/JWT values (see Environment Variables above)
6. `docker compose -f docker-compose.prod.yml up -d --build`
7. Lock RDS's security group to accept traffic only from EC2's security group

## Known Limitations

- Single EC2 instance — no load balancing or auto-scaling
- Served over HTTP, not HTTPS (no domain/TLS certificate configured yet)
- No image thumbnails/resizing (bonus feature, not implemented)
- No pagination on large galleries
- No photo delete functionality yet (upload-only; storage layer supports delete, but no endpoint/UI wired up)
- Gallery PIN is shown once at publish time — store it securely
- No email notifications for gallery links
- No gallery-level expiration (bonus feature, not implemented)
- No CI/CD yet — deployment is currently manual (`git pull` + `docker compose up --build` on the server)

## Project Structure

```
Photo_Project/
├── backend/          # Spring Boot API
│   └── src/main/java/com/trizen/photoshare/
│       ├── controller/
│       ├── service/
│       ├── model/
│       ├── security/
│       └── repository/
├── frontend/         # React SPA
│   └── src/
│       ├── pages/
│       ├── components/
│       └── api/
├── docker-compose.yml       # local dev (includes Postgres container)
├── docker-compose.prod.yml  # production (EC2 + RDS, no local Postgres)
└── README.md
```

## Workflow

1. Admin creates an event and adds team members by email
2. Team members upload event photos
3. Admin reviews all photos and selects ones for the gallery
4. Admin publishes the gallery with a 6-digit PIN
5. Customer opens the shareable link, enters the PIN, and views/downloads photos

---

Built for the TrizenAI Full Stack Internship Challenge.

<--version CI/CD pipeline test -->
