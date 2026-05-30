# Nexus — Personal Productivity Hub

A modern desktop productivity app for people juggling a full-time job, side
projects, and an active lifestyle. Built with a Java 21 backend and a React 19
frontend embedded in a JavaFX WebView.

---

## Tech Stack

### Backend

| Layer | Library |
|---|---|
| Desktop shell | JavaFX 21.0.5 (undecorated window + WebView) |
| Database | H2 2.2 (embedded file, `~/.nexus/data/nexus.mv.db`) |
| SQL | JOOQ 3.19 (type-safe, code-generated) |
| Migrations | Flyway 10 |
| Connection pool | HikariCP 5 |
| Serialization | Jackson 2.17 + JavaTimeModule |
| Utility | Lombok |
| Logging | SLF4J 2 + Logback 1.5 |
| Testing | JUnit 5, Mockito 5 |

### Frontend

| Layer | Library |
|---|---|
| Framework | React 19 + TypeScript |
| Bundler | Vite 8 |
| Styling | Tailwind CSS 3 |
| Charts | Recharts 3 |
| Drag & drop | @dnd-kit/core + @dnd-kit/sortable |
| Icons | lucide-react |
| Date parsing | date-fns 4, chrono-node 2 |
| Markdown | react-markdown 9 + remark-gfm + rehype-sanitize |

---

## Architecture

```
JavaFX Application
└── Stage (undecorated, 1280×800)
    └── WebView (Chromium-based)
        └── React SPA
            └── window.nexusBridge  ←→  NexusBridge.java
                                         ├── TaskBridge
                                         ├── GoalBridge
                                         ├── DashboardBridge
                                         ├── PlanningBridge
                                         ├── ProjectBridge
                                         └── WindowBridge
```

The React frontend communicates with the Java backend through a typed
bidirectional bridge. Java pushes events to React via `window.onBridgeEvent()`;
React calls Java via `window.nexusBridge.*` methods (all pass/return JSON).

---

## Requirements

- **Java 21** (LTS) — [download](https://adoptium.net)
- **Maven 3.9+** — bundled with IntelliJ or [download](https://maven.apache.org)
- **Node 20+** — only needed if you modify the frontend
- Internet access on first build (Maven Central + npm registry)

---

## First-Time Setup

### 1. Generate JOOQ sources (required before compiling)

```bash
mvn generate-sources
```

JOOQ runs Flyway against a throw-away H2 file in `target/`, reads the schema,
and emits type-safe Java classes.

### 2. (IntelliJ only) Mark generated sources

Right-click `target/generated-sources/jooq` → **Mark Directory as → Sources Root**

---

## Running the App

```bash
mvn javafx:run
```

On first launch Nexus will:
1. Create `~/.nexus/data/` and `~/.nexus/logs/`
2. Apply all Flyway migrations (V1–V13) to set up the production database
3. Seed life-area categories and sample tasks
4. Open a single-instance-locked window (port 47291 lock prevents duplicates)

---

## Running Tests

```bash
mvn test
```

---

## Building the Frontend (dev)

```bash
cd src/main/webui
npm install
npm run dev       # hot-reload dev server
npm run build     # build to src/main/resources/webui/
```

---

## Project Structure

```
src/main/java/com/nexus/
├── NexusApp.java               ← JavaFX Application (window, tray, lifecycle)
├── config/
│   ├── AppContext.java         ← manual DI container
│   ├── DatabaseConfig.java     ← HikariCP + Flyway
│   └── JooqConfig.java
├── model/                      ← domain objects + enums
│   ├── Task, Subtask, Goal, Project, Category, Tag
│   ├── RecurrenceRule, TimeBlock, PomodoroSession, Streak
│   └── enums/  (Priority, TaskStatus, RecurrenceType, …)
├── repository/                 ← JOOQ-backed data access
├── service/
│   ├── TaskService             ← CRUD, filtering, status transitions
│   ├── GoalService             ← progress tracking
│   ├── RecurrenceService       ← recurring task generation & skipping
│   ├── ReminderService         ← reminder scheduling
│   ├── StreakService           ← streak calculation
│   ├── PomodoroService         ← session management
│   ├── TimeBlockService        ← calendar time blocks
│   ├── BackupService           ← backup/restore
│   ├── ICalExportService       ← iCalendar export
│   ├── ExportService           ← CSV/JSON export
│   ├── SettingsService         ← user preferences
│   └── SystemTrayService       ← Windows system tray
└── ui/
    ├── NexusBridge.java        ← top-level bridge (composes sub-bridges)
    ├── MainWindow.java         ← JavaFX root + WebView wiring
    └── bridge/
        ├── TaskBridge.java
        ├── GoalBridge.java
        ├── DashboardBridge.java
        ├── PlanningBridge.java
        ├── ProjectBridge.java
        ├── WindowBridge.java
        └── *Input.java / BridgeDtos.java  ← JSON DTOs

src/main/webui/src/
├── App.tsx                     ← root component + view router
├── bridge.ts                   ← TypeScript types for window.nexusBridge
├── types.ts                    ← shared domain types
├── components/
│   ├── Sidebar, TopBar         ← navigation & window chrome
│   ├── QuickAdd                ← rapid task capture (keyboard shortcut)
│   ├── SearchPalette           ← command palette
│   ├── TaskDialog              ← full task editor
│   ├── SubtaskList, TagPicker, DatePicker
│   └── ToastStack, useToast    ← notification toasts
└── views/
    ├── Dashboard               ← stats, charts, quick actions
    ├── Today, Week             ← time-focused views
    ├── Inbox                   ← quick capture
    ├── Scheduled, Anytime, Someday  ← GTD-style buckets
    ├── TaskList                ← master filtered list
    ├── Projects                ← project-grouped tasks
    ├── Kanban                  ← status-based board
    ├── Calendar                ← calendar with tasks
    ├── Goals                   ← goal tracking & progress
    ├── Pomodoro                ← timer + session history
    ├── Eisenhower              ← urgency/importance matrix
    ├── Review                  ← weekly review
    └── Settings                ← preferences, backup, export

src/main/resources/
├── db/migration/               ← Flyway SQL (V1–V13)
└── webui/                      ← compiled React bundle (committed)
```

---

## Database Migrations

| Version | Contents |
|---|---|
| V1 | Initial schema |
| V2 | Seed data |
| V3 | Indexes |
| V4 | Category additions |
| V5 | Streak tracking |
| V6 | Schema enhancements |
| V7 | Subtasks, tags, projects, import |
| V8 | Multi-category support |
| V9 | Settings persistence |
| V10 | Recurrence system v2 |
| V11 | Task snooze |
| V12 | Defer / lifecycle / recurrence modes |
| V13 | Task templates + energy log |

---

## Data Location

| File | Purpose |
|---|---|
| `~/.nexus/data/nexus.mv.db` | Production H2 database |
| `~/.nexus/data/window-state.json` | Saved window position/size |
| `~/.nexus/logs/nexus.log` | Rolling log (7-day history) |

---

## Keyboard Shortcuts

| Key | Action |
|---|---|
| `Ctrl+N` | Quick-add task |
| `Ctrl+K` | Open search palette |
| `Ctrl+D` | Mark selected task done |

---

## CI

GitHub Actions runs two jobs on every push/PR to `master`:

- **java** — `mvn verify` under Xvfb on Ubuntu (Java 21 Temurin)
- **frontend** — `npm ci && lint && tsc --noEmit && npm run build` (Node 20)
