# Nexus — Personal Productivity Hub

A modern, polished desktop productivity app for people juggling a full-time
job, side projects, and an active lifestyle (kickboxing, gym, etc.).

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | JavaFX 21 + AtlantaFX 2 (PrimerDark / PrimerLight) |
| Icons | Ikonli + Material Design 2 pack |
| Extra controls | ControlsFX 11 |
| Build | Maven 3.9+ |
| Database | H2 (embedded file, `~/.nexus/data/nexus.mv.db`) |
| SQL | JOOQ 3.19 (type-safe, code-generated) |
| Migrations | Flyway 10 |
| Connection pool | HikariCP 5 |
| Utility | Lombok, Jackson 2.17 |
| Logging | SLF4J 2 + Logback 1.5 |
| Charts (Phase 3) | JFreeChart 1.5 |
| Testing | JUnit 5, Mockito 5, TestFX 4 |

---

## Requirements

- **Java 21** (LTS) — [download](https://adoptium.net)
- **Maven 3.9+** — bundled with IntelliJ or [download](https://maven.apache.org)
- Internet access on first build (to download dependencies from Maven Central)

---

## First-Time Setup

### 1. Generate JOOQ sources (required before compiling)

JOOQ generates type-safe Java from the database schema.
This step runs Flyway against a throw-away H2 file in `target/`, then
JOOQ reads the schema and emits Java classes.

```bash
mvn generate-sources
```

### 2. (IntelliJ only) Mark generated sources

Right-click `target/generated-sources/jooq` → **Mark Directory as → Sources Root**

Now all red `com.nexus.db.*` errors disappear.

---

## Running the App

```bash
mvn javafx:run
```

On first launch Nexus will:
1. Create `~/.nexus/data/` and `~/.nexus/logs/`
2. Run Flyway migrations to set up the production database
3. Seed five life areas (Work, Side Projects, Kickboxing, Gym, Personal) plus sample tasks

---

## Running Tests

```bash
mvn test
```

Tests use Mockito mocks for the repository layer — no database required.

---

## Project Structure

```
src/main/java/com/nexus/
├── Main.java               ← entry point (plain launcher)
├── NexusApp.java           ← JavaFX Application
├── config/
│   ├── AppContext.java     ← manual DI container
│   ├── DatabaseConfig.java ← HikariCP + Flyway
│   └── JooqConfig.java     ← DSLContext factory
├── model/                  ← domain objects + enums
├── repository/             ← JOOQ-backed data access
├── service/                ← business logic
├── viewmodel/              ← JavaFX observable state (MVVM)
└── ui/
    ├── MainWindow.java
    ├── components/         ← Sidebar, TaskCard
    └── views/              ← TaskListView, TaskDetailPanel

src/main/resources/
├── db/migration/           ← Flyway SQL scripts
├── css/nexus.css           ← AtlantaFX overrides
└── logback.xml

target/generated-sources/jooq/com/nexus/db/   ← JOOQ generated (git-ignored)
```

---

## Data Location

| File | Purpose |
|---|---|
| `~/.nexus/data/nexus.mv.db` | Production H2 database |
| `~/.nexus/logs/nexus.log` | Rolling log (7-day history) |

---

## Keyboard Shortcuts (Phase 1)

| Key | Action |
|---|---|
| `Ctrl+N` | New task |
| `Ctrl+F` | Focus search |
| `Ctrl+D` | Mark selected task as done |
| `Delete` | Archive selected task |

---

## Build Pipeline Explained

```
mvn javafx:run
  └─ generate-sources
       ├─ flyway:migrate  → target/jooq-gen/nexusdb  (schema only, build-time)
       └─ jooq:generate   → target/generated-sources/jooq  (Java classes)
  └─ compile  → compiles your code + JOOQ generated code
  └─ javafx:run  → launches com.nexus.Main
```

At runtime Flyway runs again (fast, idempotent) against the real production
database in `~/.nexus/data/`, applying any new migrations automatically.

---

## Phase Roadmap

| Phase | Status | Features |
|---|---|---|
| **1 – Foundation** | ✅ Current | Tasks, projects, categories, CRUD, search/filter, archive |
| **2 – Time Control** | Planned | Today view, Week view, recurring tasks, Pomodoro timer, time blocks, reminders |
| **3 – Insight** | Planned | Goals, streaks, dashboard charts, JSON backup/export |
