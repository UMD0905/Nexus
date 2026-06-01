# Nexus — Personal Productivity Hub

A modern desktop productivity app for people juggling a full-time job, side
projects, and an active lifestyle. Built with a Java 21 backend and a React 19
frontend embedded in a JavaFX WebView, packaged as a native Windows exe via
jpackage.

---

## Features

### Task Management
- Create, edit, delete tasks with title, description, priority, due date, start time, and estimated duration
- Four priority levels: **Low**, **Medium**, **High**, **Critical** (color-coded throughout)
- Four status levels: **To Do**, **In Progress**, **Done**, **Cancelled**
- Assign tasks to multiple life areas (categories) and a project
- Add free-form tags with custom colors
- Subtasks with drag-to-reorder and completion tracking
- Markdown-formatted descriptions with live preview
- **Mass actions** — select multiple tasks with checkboxes (Shift+click range, Ctrl+A all), then bulk-set status, priority, life area, or delete

### Views
| View | Description |
|---|---|
| **Dashboard** | Stats cards (active tasks, due today, overdue, Pomodoro minutes), weekly completion chart, category breakdown, streak badges |
| **Today** | Tasks due today + optional time-block planner |
| **This Week** | Tasks grouped by day across the current ISO week |
| **Calendar** | Monthly calendar overlay with task dots |
| **Kanban** | Drag-and-drop board grouped by status |
| **All Tasks** | Full list with live filter bar (status, priority, category, search) |
| **Inbox** | GTD capture bucket — unprocessed tasks land here |
| **Anytime** | GTD anytime bucket — ready to do, no fixed date |
| **Someday** | GTD someday bucket — low-urgency ideas |
| **Scheduled** | Tasks with a future defer-until date (hidden from main views until then) |
| **Goals** | Long-term goals linked to tasks, progress bar computed from task completion |
| **Projects** | Project cards with task counts and completion percentage |
| **Streaks** | Full streak management — view current/best streaks, edit titles and counters, reset or delete individual streaks |
| **Pomodoro** | Configurable timer (work / short break / long break), session history, auto-logs actual minutes to task |
| **Eisenhower Matrix** | 2×2 urgent/important quadrant board |
| **Review** | Weekly review flow — completed this week, overdue, upcoming |
| **Archive** | Soft-deleted tasks, restorable in bulk |
| **Settings** | Preferences, backup/restore, iCal export, About & Diagnostics |

### Recurring Tasks
- Daily, weekday, weekly, monthly, yearly recurrence
- Fixed interval or "after completion" modes
- Generates upcoming instances 14 days ahead on startup
- Skip a single instance without breaking the series
- Automatically creates and updates a streak entry when completed

### Streaks
- Auto-created when a recurring task is marked done
- Tracks current streak, best streak, and last completed date
- Active (flame) / inactive (snowflake) visual state based on whether completed today or yesterday
- Dedicated **Streaks** page with always-visible Edit, Reset, and Delete buttons per streak
- Edit dialog: rename, change life area, manually adjust current/best counters

### Goals
- Link tasks to goals; progress 0–100% is computed from task completion ratio
- Status: Active, Completed, Abandoned
- Assign multiple life area categories

### Projects
- Color-coded project cards under a life area
- Track start date, due date, status (Active / Completed / Archived)
- Task count and completion percentage computed live
- Projects appear in the sidebar under their parent life area

### Life Areas (Categories)
- Custom name and color
- Drag-to-reorder in the sidebar (position persisted per session)
- Tasks and goals can belong to multiple life areas simultaneously

### Pomodoro Timer
- Configurable work (default 25 min), short break (5 min), long break (15 min)
- Auto-advance option
- Session history per task — actual focused minutes rolled up to the task
- Dashboard shows today's Pomodoro count and this week's total focus time

### Reminders & Notifications
- Per-task reminder: fire N minutes before the due date
- OS toast notification (ControlsFX) + in-app notification record
- Missed reminders caught on startup (up to 24 h back)
- Bell badge in the top bar shows unread count
- Task snooze: silence reminders until a chosen time

### Search & Quick Actions
- **Quick Add** (Ctrl+N): capture a task title fast without leaving context
- **Search palette** (Ctrl+K): full-text search across all tasks and goals
- **Mark done** (Ctrl+D): complete the currently focused task from anywhere

### Import / Export
- **JSON export / import** — full data backup and restore
- **iCalendar (.ics) export** — tasks with due dates become VTODO / VEVENT entries importable in any calendar app
- **Auto-backup** — scheduled backup to a configurable directory
- **Diagnostics export** — zip of recent logs + settings keys (values redacted) for support

### Settings (About & Diagnostics)
- App version, Java version, OS info
- Database file path and size
- Entity counts (tasks, goals, categories)
- Schema version (Flyway)
- One-click export of a diagnostics zip to `~/Downloads/`
- Theme toggle (dark / light)
- All preferences persisted to the H2 database

### Window & System
- Custom undecorated window with drag-to-move title bar and double-click-to-maximize
- Minimize (−), maximize/restore (□), and close (×) buttons in the top bar
- Window position and size persisted across restarts; pre-maximized bounds saved correctly
- Single-instance lock (TCP port 47291) prevents duplicate processes
- System tray icon with quick-add action and show/hide
- Minimize-to-tray support

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+N` | Open Quick Add task dialog |
| `Ctrl+K` | Open search palette |
| `Ctrl+D` | Mark the focused task done |
| `Ctrl+A` | Select / deselect all tasks in the current list |
| `Shift+Click` | Range-select tasks (mass actions) |
| `Escape` | Close dialogs / clear selection |
| `Enter` | Confirm dialog / save form |
| Double-click title bar | Toggle maximize |

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
| Testing | JUnit 5, Mockito 5, AssertJ |
| Native packaging | jpackage (Java 14+, bundled with JDK 21) |

### Frontend

| Layer | Library |
|---|---|
| Framework | React 19 + TypeScript |
| Bundler | Vite 8 (code-split — each view is a separate lazy chunk) |
| Styling | Tailwind CSS 3 |
| Charts | Recharts 3 |
| Drag & drop | @dnd-kit/core + @dnd-kit/sortable |
| Icons | lucide-react |
| Date parsing | date-fns 4, chrono-node 2 |
| Markdown | react-markdown 9 + remark-gfm + rehype-sanitize |

---

## Architecture

```
JavaFX Application (NexusApp.java)
└── Stage  ·  undecorated, 1280×800 default
    └── WebView (WebKit)
        └── React SPA  (file:// from temp-extracted webui/)
            └── window.nexusBridge  ←→  NexusBridge.java
                                         ├── TaskBridge       tasks.*
                                         ├── GoalBridge       goals.*
                                         ├── DashboardBridge  dashboard.*
                                         ├── PlanningBridge   planning.*
                                         ├── ProjectBridge    projects.*
                                         └── WindowBridge     win.*
```

**Bridge pattern** — the React frontend communicates with the Java backend through
a typed bidirectional bridge:

- **React → Java**: `window.nexusBridge.<subBridge>.<method>(jsonArgs)` — synchronous JSON in/out
- **Java → React**: `window.onBridgeEvent(eventJson)` — pushed via `Platform.runLater` for notifications and hotkey events

Window control methods (`minimizeWindow`, `maximizeWindow`, `closeWindow`,
`startDrag`, `dragWindow`, `toggleMaximize`) are proxied directly on the
top-level `NexusBridge` object rather than the sub-bridge, since JavaFX WebKit's
JSObject field traversal is unreliable for DOM-reserved property names.

---

## Requirements

- **Java 21** (LTS) — [download Temurin](https://adoptium.net)
- **Maven 3.9+** — bundled with IntelliJ or [download](https://maven.apache.org)
- **Node 20+** — only needed if you modify the frontend

---

## Running in Development

### 1. Generate JOOQ sources (required before compiling)

```bash
mvn generate-sources
```

JOOQ runs Flyway against a throw-away H2 file in `target/`, reads the schema,
and emits type-safe Java classes into `target/generated-sources/jooq/`.

> **IntelliJ**: right-click `target/generated-sources/jooq` → **Mark Directory as → Sources Root**

### 2. Run the app

```bash
mvn javafx:run
```

### 3. Frontend hot-reload (optional)

```bash
cd src/main/webui
npm install
npm run dev       # Vite dev server — not embedded, browser-only preview
npm run build     # compile → src/main/resources/webui/ (picked up by the app)
```

On first launch Nexus will:
1. Create `~/.nexus/data/` and `~/.nexus/logs/`
2. Apply all Flyway migrations (V1–V13)
3. Seed default life-area categories and sample tasks
4. Open a single-instance-locked window

---

## Running Tests

```bash
mvn test
```

100 unit tests across 9 service classes — all pass. Tests cover happy paths for
`TaskService`, `GoalService`, `CategoryService`, `ProjectService`,
`TimeBlockService`, `PomodoroService`, `StreakService`, `ReminderService`,
`BackupService`, `ICalExportService`, and `SettingsService`.

---

## Building a Native Windows Desktop App

Run `package.bat` from the project root:

```
package.bat
```

This will:
1. Run `mvn package -Ppackage -DskipTests` to assemble `target/dist-input/`
2. Run `jpackage` to create a self-contained Windows app-image at `dist/Nexus/`

The result is a standalone `dist/Nexus/Nexus.exe` — no JRE installation needed,
everything is bundled. To install:

1. Copy the `dist/Nexus/` folder anywhere (e.g. `C:\Users\<you>\Apps\Nexus\`)
2. Right-click `Nexus.exe` → **Send to → Desktop (create shortcut)**
3. Right-click the desktop shortcut → **Pin to taskbar**

---

## Project Structure

```
src/main/java/com/nexus/
├── NexusApp.java               ← JavaFX Application (window, tray, hotkeys, lifecycle)
├── config/
│   ├── AppContext.java         ← manual DI container (all singletons wired here)
│   ├── DatabaseConfig.java     ← HikariCP datasource + Flyway runner
│   └── JooqConfig.java
├── model/                      ← domain objects + enums
│   ├── Task, Subtask, Goal, Project, Category, Tag
│   ├── RecurrenceRule, TimeBlock, PomodoroSession, Streak
│   └── enums/  (Priority, TaskStatus, RecurrenceType, Lifecycle …)
├── repository/                 ← JOOQ-backed data access (dynamic WHERE builders)
├── service/
│   ├── TaskService             ← CRUD, status transitions, recurrence hooks
│   ├── GoalService             ← progress tracking via linked tasks
│   ├── RecurrenceService       ← upcoming instance generation and skip logic
│   ├── StreakService           ← streak auto-create, update, expiry, delete
│   ├── ReminderService         ← 60 s background scan (only tasks with reminders)
│   ├── PomodoroService         ← session start/complete/abandon
│   ├── TimeBlockService        ← calendar time blocks
│   ├── BackupService           ← scheduled auto-backup via ExportService
│   ├── ICalExportService       ← iCalendar (.ics) file generation
│   ├── ExportService           ← JSON export/import
│   ├── CategoryService         ← life area CRUD
│   ├── ProjectService          ← project CRUD + task stats
│   ├── SettingsService         ← key-value settings (H2-backed)
│   ├── NotificationService     ← in-app notification records
│   └── SystemTrayService       ← Windows tray icon + context menu
└── ui/
    ├── NexusBridge.java        ← top-level bridge + window control proxies
    ├── MainWindow.java         ← JavaFX root pane + WebView wiring + bridge injection
    └── bridge/
        ├── TaskBridge.java
        ├── GoalBridge.java
        ├── DashboardBridge.java
        ├── PlanningBridge.java
        ├── ProjectBridge.java
        ├── WindowBridge.java
        └── BridgeDtos.java     ← lightweight JSON DTOs (no domain object leakage)

src/main/webui/src/
├── App.tsx                     ← root component, view router, data refresh
├── bridge.ts                   ← fully typed TS wrapper for window.nexusBridge
├── types.ts                    ← shared domain types (Task, Goal, Streak …)
├── components/
│   ├── Sidebar.tsx             ← nav + life areas (drag-to-reorder) + projects
│   ├── TopBar.tsx              ← window chrome, quick add, bell, theme toggle
│   ├── QuickAdd.tsx            ← rapid capture modal (Ctrl+N)
│   ├── SearchPalette.tsx       ← command palette (Ctrl+K)
│   ├── TaskDialog.tsx          ← full task editor (subtasks, tags, recurrence …)
│   ├── SubtaskList.tsx
│   ├── TagPicker.tsx
│   ├── DatePicker.tsx
│   └── ToastStack.tsx
└── views/                      ← all lazy-loaded (React.lazy + Suspense)
    ├── Dashboard.tsx
    ├── Today.tsx, Week.tsx, Calendar.tsx
    ├── Inbox.tsx, Anytime.tsx, Someday.tsx, Scheduled.tsx
    ├── TaskList.tsx            ← shared list component (mass actions, bulk ops)
    ├── Projects.tsx
    ├── Kanban.tsx
    ├── Goals.tsx
    ├── Streaks.tsx             ← streak management (edit / reset / delete)
    ├── Pomodoro.tsx
    ├── Eisenhower.tsx
    ├── Review.tsx
    └── Settings.tsx

src/main/resources/
├── db/migration/               ← Flyway SQL scripts V1–V13
├── app.properties              ← build-time version info
└── webui/                      ← compiled React bundle (committed, split into chunks)

src/test/java/com/nexus/service/
├── TaskServiceTest.java
├── GoalServiceTest.java (+ CategoryServiceTest, ProjectServiceTest …)
└── … (9 test files, 100 tests total)
```

---

## Database Migrations

| Version | Contents |
|---|---|
| V1 | Initial schema (tasks, categories, subtasks, tags) |
| V2 | Seed data (default life areas, sample tasks) |
| V3 | Indexes |
| V4 | Category colour and ordering |
| V5 | Streak tracking (current streak, best streak, last completed) |
| V6 | Start time, estimated/actual minutes on tasks |
| V7 | Subtasks, tags, projects, JSON import/export support |
| V8 | Multi-category support (task_categories join table) |
| V9 | Settings persistence (app_settings key-value table) |
| V10 | Recurrence system v2 (rules, modes, skip tracking) |
| V11 | Task snooze (snoozed_until field + notifications table) |
| V12 | Defer / lifecycle buckets / recurrence-after-completion mode |
| V13 | Task templates + energy log |

---

## Data Location

| Path | Purpose |
|---|---|
| `~/.nexus/data/nexus.mv.db` | Production H2 database |
| `~/.nexus/data/window-state.json` | Saved window position, size, and maximized state |
| `~/.nexus/logs/nexus.log` | Rolling log (7-day retention) |
| `~/.nexus/backups/` | Auto-backup destination (configurable in Settings) |

---

## CI

GitHub Actions runs two jobs on every push / PR to `master`:

- **java** — `mvn verify` under Xvfb on Ubuntu (Java 21 Temurin)
- **frontend** — `npm ci && lint && tsc --noEmit && npm run build` (Node 20)
