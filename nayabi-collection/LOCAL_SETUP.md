# Running Nayabi Collection on Your Machine

This guide gets the site running locally in about 5 minutes.

---

## Where the code lives

| | |
|---|---|
| **GitHub repo** | https://github.com/zakir9622/Agentic-AI |
| **Branch** | `claude/nayabi-collection-ecommerce-ui9x3n` |
| **Project folder** | `nayabi-collection/` (inside the repo) |

The full ecommerce app is in the `nayabi-collection/` subfolder. Everything you
need — code, database schema, sample data — is committed and safe on GitHub.

---

## Quick start (one command)

### 1. Get the code onto your machine

```bash
git clone https://github.com/zakir9622/Agentic-AI.git
cd Agentic-AI/nayabi-collection
git checkout claude/nayabi-collection-ecommerce-ui9x3n
```

### 2. Run the setup script

```bash
bash setup.sh
```

This automatically:
- Installs all dependencies
- Generates `.env.local` with secure secrets (no manual setup)
- Spins up a local PostgreSQL database via Docker
- Loads 16 sample products and an admin account

### 3. Start the site

```bash
npm run dev
```

Open **http://localhost:3000** in your browser. 🎉

---

## What you need installed first

| Tool | Why | Get it |
|---|---|---|
| **Node.js 20+** | Runs the app | https://nodejs.org |
| **Docker Desktop** | Runs the local database | https://docker.com/products/docker-desktop |
| **Git** | Clones the code | https://git-scm.com |

> **No Docker?** No problem. Get a free database at https://neon.tech, paste its
> connection string into `DATABASE_URL` in `.env.local`, then run
> `npm run db:push && npm run db:seed`.

---

## Admin panel

After setup, visit **http://localhost:3000/admin/login**

| | |
|---|---|
| Email | `admin@nayabicollection.com` |
| Password | `Admin@1234!` |

---

## Useful commands

```bash
npm run dev          # Start the site (http://localhost:3000)
npm run db:studio    # Visual database browser (http://localhost:5555)
npm run build        # Production build
npm run typecheck    # Check TypeScript
npm run lint         # Check code style
```

### Managing the local database (Docker)

```bash
docker stop nayabi-db     # Stop the database
docker start nayabi-db    # Start it again
docker rm -f nayabi-db    # Delete it (run setup.sh again to recreate)
```

---

## The site works without any paid accounts

Payments, email, SMS, and image upload all **degrade gracefully** — the store
runs perfectly for browsing and testing without them. To enable a service, just
fill in its keys in `.env.local`. See the main [README](./README.md) for the
full list and where to get each key.
