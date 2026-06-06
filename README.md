# Nexus — Personal Productivity Hub

A full-featured desktop productivity app built with Java 21 + React 19, packaged as a self-contained native Windows executable — no installer, no JRE required.

---

## Features

### 19 Built-in Views

| View | Description |
|---|---|
| **Dashboard** | At-a-glance stats, weekly completion chart, life-area breakdown, streak badges |
| **Today** | Tasks due today + optional hour-by-hour time-block planner |
| **This Week** | Tasks grouped by day across the current ISO week |
| **Calendar** | Monthly grid with colored task dots; click a day to see its tasks |
| **Kanban** | Drag-and-drop board: To Do / In Progress / Done / Cancelled |
| **All Tasks** | Full task list with live filter bar (status, priority, life area, free text) |
| **Inbox / Anytime / Someday / Scheduled** | GTD capture and clarify buckets |
| **Goals** | Goal cards with linked tasks and auto-computed progress bars |
| **Projects** | Color-coded project cards with live completion percentages |
| **Streaks** | Habit streak management — edit, reset, or delete any streak |
| **Pomodoro** | Focus timer with configurable intervals, auto-advance, and session history |
| **Eisenhower Matrix** | Drag tasks between urgent/important quadrants |
| **Review** | Weekly review: completed, overdue, and upcoming |
| **Finance** | Income/expense tracker with charts and manual balance overrides |
| **Archive** | Soft-deleted tasks with bulk restore |
| **Settings** | Preferences, backup/restore, iCal export, diagnostics |

### Task System

- Title, description (Markdown), priority, status, due date/time, start time, estimated duration
- Assign to multiple **life areas** and a **project**; add color-coded **tags**
- **Subtasks** with drag-to-reorder and live completion percentage
- **Recurrence** — Daily, Weekdays, Weekly, Monthly, Yearly; fixed or after-completion modes
- **Reminders** — OS toast + in-app notification; snooze to a specific future time
- **Mass actions** — Shift+click or Ctrl+A to select; bulk set status/priority/life area or delete

### Finance Tracker

- Log Income/Expense transactions with amount, currency (UZS/USD), category, description, and date
- 4 stat cards: Total Balance, Month Income, Month Expenses, Month Net — all currency-aware
- **Manual override** — click the pencil icon on any card to set a custom value; reset anytime
- Charts: 6-month trend bars, category breakdown; tabs for Overview / Income / Expenses / All History

### Other Highlights

- **Recurring tasks** auto-generate 14 days ahead; completions auto-update habit streaks
- **GTD buckets** — Inbox, Anytime, Someday, Scheduled with defer-until logic
- **Pomodoro** — configurable intervals, auto-advance, per-task session history
- **Search palette (Ctrl+K)** — full-text search across tasks and goals
- **Quick Add (Ctrl+N)** — minimal capture modal that lands in Inbox
- **Natural language dates** via chrono-node ("next Friday 3pm")
- **Import/Export** — JSON full backup, iCalendar (.ics) export, auto-backup to configurable directory
- **System tray** — minimize to tray, right-click for Quick Add and Show/Hide
- **Custom undecorated window** — draggable title bar, persisted position/size, single-instance lock

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+N` | Quick Add task |
| `Ctrl+K` | Search / command palette |
| `Ctrl+D` | Mark focused task Done |
| `Ctrl+A` | Select / deselect all |
| `Shift+Click` | Range-select tasks |
| `Escape` | Close dialog / clear selection |
| `Double-click title bar` | Toggle maximize |

---

## Tech Stack

### Backend

| Layer | Technology |
|---|---|
| Desktop shell | JavaFX 21 — undecorated Stage + WebView |
| Database | H2 2.2 (embedded file DB) |
| SQL | JOOQ 3.19 + Flyway 10 migrations |
| Connection pool | HikariCP 5 |
| Serialization | Jackson 2.17 + JavaTimeModule |
| Logging | SLF4J 2 + Logback 1.5 (rolling, 7-day) |
| OS notifications | ControlsFX 11 |
| Utilities | Lombok, JUnit 5, Mockito 5 |

### Frontend

| Layer | Technology |
|---|---|
| Framework | React 19 + TypeScript |
| Bundler | Vite 8 (code-split per view) |
| Styling | Tailwind CSS 3 |
| Charts | Recharts 3 |
| Drag & drop | @dnd-kit/core + sortable |
| Icons | lucide-react |
| Date parsing | date-fns 4 + chrono-node 2 |
| Markdown | react-markdown 9 + remark-gfm |

---

## Architecture

```
JavaFX Application
└── Stage (undecorated, 1280×800)
    └── WebView (WebKit)
        └── React SPA (file:// from bundled webui/)
            └── window.nexusBridge  ←→  NexusBridge.java (JSObject)
                    ├── TaskBridge        tasks.*
                    ├── GoalBridge        goals.*
                    ├── DashboardBridge   dashboard.*
                    ├── PlanningBridge    planning.*
                    ├── ProjectBridge     projects.*
                    ├── FinanceBridge     finance.*
                    └── WindowBridge      win.*
```

**React → Java:** `window.nexusBridge.<sub>.<method>(jsonArgs)` — synchronous, JSON in / JSON out.  
**Java → React:** `window.onBridgeEvent(eventJson)` — async push via `Platform.runLater()` for notifications and hotkeys.

---

## Requirements

| Tool | Version |
|---|---|
| Java (JDK) | 21 LTS |
| Maven | 3.9+ |
| Node.js | 20+ (only for frontend changes) |

---

## Running in Development

```bash
# 1. Generate JOOQ sources (required before first compile)
mvn generate-sources

# 2. Run the app
mvn javafx:run
```

**Frontend changes (optional):**
```bash
cd src/main/webui
npm install
npm run dev      # browser-only preview (no Java bridge)
npm run build    # compile → src/main/resources/webui/
```

On first launch Nexus creates `~/.nexus/data/`, applies all Flyway migrations, seeds default life areas, and acquires the single-instance lock.

> **IntelliJ tip:** mark `target/generated-sources/jooq` as **Sources Root** after `mvn generate-sources`.

---

## Running Tests

```bash
mvn test
```

100 unit tests across 12 service classes — all pass. Tests use Mockito mocks; no H2 instance needed.

---

## Building a Native Windows App

```
package.bat
```

This sets `JAVA_HOME`, runs `mvn package -Ppackage -DskipTests`, and runs `jpackage` to produce `dist/Nexus/Nexus.exe` — fully self-contained, no JRE required on the target machine.

A desktop shortcut `Nexus.lnk` is created automatically after the build.

---

## Project Structure

```
src/main/java/com/nexus/
├── NexusApp.java               # entry point — window, tray, hotkeys, lifecycle
├── config/                     # AppContext (DI), DatabaseConfig, JooqConfig
├── model/                      # domain objects: Task, Goal, Project, Transaction, enums…
├── repository/                 # JOOQ-backed data access
├── service/                    # business logic (TaskService, FinanceService, etc.)
└── ui/
    ├── NexusBridge.java        # top-level JS bridge + window control proxies
    ├── MainWindow.java         # WebView wiring, bridge injection
    └── bridge/                 # TaskBridge, GoalBridge, FinanceBridge, WindowBridge…

src/main/webui/src/
├── App.tsx                     # root router, data loading, bridge event handler
├── bridge.ts                   # typed TypeScript wrapper for window.nexusBridge
├── types.ts                    # shared domain types
├── components/                 # Sidebar, TopBar, TaskDialog, DatePicker, etc.
└── views/                      # 19 lazy-loaded view components

src/main/resources/
├── db/migration/               # Flyway SQL scripts V1–V14
└── webui/                      # compiled React bundle (committed — no Node needed to run)
```

---

## Database Migrations

| Version | Contents |
|---|---|
| V1–V3 | Core schema (tasks, categories, subtasks, tags), seed data, indexes |
| V4 | Category color + display order |
| V5 | Streaks table |
| V6 | Task time fields (start_time, estimated/actual minutes) |
| V7 | Projects table, JSON import/export schema |
| V8 | Multi-category support (task_categories join table) |
| V9 | app_settings key-value table |
| V10 | Recurrence rules v2 (fixed / after-completion, skip tracking) |
| V11 | Task snooze field + notifications table |
| V12 | Defer/lifecycle buckets (defer_until, lifecycle enum) |
| V13 | Task templates + energy log tables |
| V14 | Finance: `TRANSACTIONS` table (DECIMAL(15,2) amounts, UZS/USD, indexed) |

---

## Data Location

| Path | Purpose |
|---|---|
| `~/.nexus/data/nexus.mv.db` | H2 database |
| `~/.nexus/data/window-state.json` | Saved window position and size |
| `~/.nexus/logs/nexus.log` | Rolling log (7-day retention) |
| `~/.nexus/backups/` | Auto-backup destination |

`~` = `%USERPROFILE%` on Windows.

---

## CI / CD

GitHub Actions runs on every push to `master`:

- **java** — Ubuntu + Xvfb, Java 21, `mvn verify` (compiles + 100 tests)
- **frontend** — Node 20, `npm ci && npm run lint && tsc --noEmit && npm run build`
- **release** (manual) — builds native Windows app-image and creates a GitHub Release
