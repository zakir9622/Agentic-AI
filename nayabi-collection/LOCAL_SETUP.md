# Running Nayabi Collection on Your Machine

This guide gets the site running locally in about 5 minutes.

---

## Where the code lives

| | |
|---|---|
| **GitHub repo** | https://github.com/zakir9622/Agentic-AI |
| **Branch** | `claude/nayabi-collection-ecommerce-ui9x3n` |
| **Project folder** | `nayabi-collection/` (inside the repo) |

---

## Step 1 — Install prerequisites

| Tool | Why | Download |
|---|---|---|
| **Node.js 20+** (LTS) | Runs the app | https://nodejs.org |
| **Docker Desktop** | Runs the local database | https://docker.com/products/docker-desktop |
| **Git** | Clones the code | https://git-scm.com |

> **After installing Docker Desktop, make sure it is open and fully started** (look for the Docker icon in your taskbar — it should be solid, not spinning).

---

## Step 2 — Get the code

Open **PowerShell** (or Git Bash / Terminal) and run:

```powershell
git clone https://github.com/zakir9622/Agentic-AI.git
cd Agentic-AI\nayabi-collection
```

---

## Step 3 — Run the setup script

### Windows (PowerShell) — recommended
```powershell
.\setup.ps1
```

If you see *"cannot be loaded because running scripts is disabled"*, run this first (once):
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```
Then retry `.\setup.ps1`.

### Mac / Linux (Terminal)
```bash
bash setup.sh
```

The script will:
- Install all npm dependencies
- Create a `.env` file with auto-generated secure secrets
- Start a local PostgreSQL database via Docker
- Push the database schema
- Seed 16 sample products + an admin account

---

## Step 4 — Start the site

```powershell
npm run dev
```

Open **http://localhost:3000** in your browser. 🎉

---

## Admin panel

Visit **http://localhost:3000/admin/login**

| | |
|---|---|
| Email | `admin@nayabicollection.com` |
| Password | `Admin@1234!` |

---

## Useful commands

```powershell
npm run dev          # Start the site (http://localhost:3000)
npm run db:studio    # Visual database browser (http://localhost:5555)
npm run build        # Production build
npm run typecheck    # Check TypeScript
```

### Managing the local database (Docker)

```powershell
docker stop nayabi-db     # Stop the database
docker start nayabi-db    # Start it again
docker rm -f nayabi-db    # Delete it (run setup.ps1 again to recreate)
```

---

## Troubleshooting

**"Docker is not running"** — Open Docker Desktop from the Start Menu and wait until the icon in the taskbar is no longer animated (fully started). Then re-run `.\setup.ps1`.

**Port 5432 already in use** — Another PostgreSQL is running. Either stop it, or get a free cloud DB at https://neon.tech, paste the URL into `DATABASE_URL` in `.env`, then run:
```powershell
npm run db:push
npm run db:seed
```

**"npm is not recognised"** — Node.js isn't installed (or you need to restart your terminal after installing it). Download from https://nodejs.org.

**"git is not recognised"** — Install Git from https://git-scm.com, then restart your terminal.

