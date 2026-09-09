# PhotoShare — Event Photo Gallery Platform

A full-stack photo-sharing application for photography/event teams. Admins create events, assign team members, review uploads, and publish PIN-protected customer galleries.

## Live Demo

> Deploy using the instructions below, then add your live URL here.

| Resource | Value |
|----------|-------|
| **App URL** | `http://localhost:5173` (local) |
| **Admin** | `admin@demo.com` / `admin123` |
| **Team Member** | `member@demo.com` / `member123` |
| **Gallery** | Publish from admin UI after selecting photos |

## Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, JWT |
| Frontend | React 19, TypeScript, Vite, React Router |
| Database | PostgreSQL 16 |
| File Storage | Local filesystem (dev) or AWS S3 (production) |
| Deployment | Docker Compose |

## Architecture

```
┌─────────────┐     HTTPS/REST      ┌──────────────────┐
│  React SPA  │ ◄─────────────────► │  Spring Boot API │
│  (Vite)     │                     │  (Port 8080)     │
└─────────────┘                     └────────┬─────────┘
                                             │
                              ┌──────────────┼──────────────┐
                              ▼              ▼              ▼
                        PostgreSQL      Local/S3       JWT Auth
                        (metadata)     (photo files)  (users + gallery)
```

### Database Schema

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

### Admin / Lead
- Register and login
- Create events and add team members
- View all uploaded photos
- Select photos for the customer gallery
- Publish gallery with a 6-digit PIN
- Generate shareable gallery link

### Team Member
- Login and view assigned events
- Upload multiple photos
- View own uploads only
- Cannot publish galleries or manage other users' photos

### Customer (no account)
- Open gallery link (`/gallery/{slug}`)
- Enter PIN to access published photos
- Browse gallery with lightbox view

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

# Run (requires Maven or use the path below)
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

### Run Everything with Docker

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080

## Environment Variables

See [`.env.example`](.env.example) for all variables. Key settings:

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/photoshare` |
| `JWT_SECRET` | JWT signing secret (32+ chars) | — |
| `STORAGE_TYPE` | `local` or `s3` | `local` |
| `S3_BUCKET` | AWS S3 bucket name | — |
| `FRONTEND_URL` | Used for gallery link generation | `http://localhost:5173` |
| `CORS_ORIGINS` | Allowed frontend origins | `http://localhost:5173` |

**Never commit secrets to Git.**

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
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
| POST | `/api/public/gallery/{slug}/access` | Public | Verify PIN |
| GET | `/api/public/gallery/{slug}/photos` | Gallery token | List published photos |

## Testing

```bash
cd backend
mvn test
```

Tests cover:
- Authentication (invalid credentials)
- Authorization (team member cannot create events or publish galleries)
- Event access control (members cannot access unassigned events)
- Full gallery workflow (upload → select → publish → PIN access)
- Incorrect PIN rejection

## Deployment

### Recommended: Docker on a cloud VM (AWS EC2, DigitalOcean, etc.)

1. Clone the repository on your server
2. Set environment variables (use S3 for production storage)
3. Run `docker compose up -d --build`
4. Point a domain to the server and configure HTTPS (e.g. with Caddy or Nginx + Let's Encrypt)

### AWS S3 Storage (Production)

Set in your environment:
```
STORAGE_TYPE=s3
S3_BUCKET=your-bucket-name
S3_REGION=us-east-1
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

## Known Limitations

- Local storage does not scale for production — use S3
- No image thumbnails/resizing (bonus feature)
- No pagination on large galleries
- Gallery PIN is shown once at publish time — store it securely
- No email notifications for gallery links

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
├── docker-compose.yml
└── README.md
```

## Workflow

1. **Admin** creates an event and adds team members by email
2. **Team members** upload event photos
3. **Admin** reviews all photos and selects ones for the gallery
4. **Admin** publishes the gallery with a 6-digit PIN
5. **Customer** opens the shareable link, enters the PIN, and views photos

---

Built for the TrizenAI Full Stack Internship Challenge.
