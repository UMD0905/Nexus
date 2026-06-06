# Nexus — Personal Productivity Hub

> A full-featured desktop productivity app built for people juggling a full-time job, side
> projects, and an active lifestyle. Java 21 backend + React 19 frontend, packaged as a
> self-contained native Windows application — no installer, no JRE required.

---

## Table of Contents

- [Features](#features)
  - [Task Management](#task-management)
  - [Views](#views)
  - [Recurring Tasks](#recurring-tasks)
  - [Streaks](#streaks)
  - [Goals](#goals)
  - [Projects](#projects)
  - [Life Areas (Categories)](#life-areas-categories)
  - [Finance Tracker](#finance-tracker)
  - [Pomodoro Timer](#pomodoro-timer)
  - [Reminders & Notifications](#reminders--notifications)
  - [Search & Quick Actions](#search--quick-actions)
  - [Kanban Board](#kanban-board)
  - [Eisenhower Matrix](#eisenhower-matrix)
  - [GTD Buckets](#gtd-buckets)
  - [Review Flow](#review-flow)
  - [Import / Export & Backup](#import--export--backup)
  - [Settings & Diagnostics](#settings--diagnostics)
  - [Window & System Integration](#window--system-integration)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Running in Development](#running-in-development)
- [Running Tests](#running-tests)
- [Building a Native Windows App](#building-a-native-windows-app)
- [Project Structure](#project-structure)
- [Database Migrations](#database-migrations)
- [Data Location](#data-location)
- [CI / CD](#ci--cd)

---

## Features

### Task Management

The core of Nexus is a rich, fully-featured task system designed to capture everything from a quick idea to a complex multi-step project.

**Creating & editing tasks:**
- Title, rich-text description (Markdown with live preview), priority, status, due date + time, start time, and estimated duration
- Assign a task to one or more **life areas** (categories) simultaneously and optionally to a **project**
- Add free-form **tags** with custom hex colors for ad-hoc labeling
- Attach **subtasks** — a drag-to-reorder checklist nested inside the parent task; completion percentage is tracked live
- Set a **recurrence rule** (daily, weekday, weekly, monthly, yearly — fixed or after-completion modes)
- Configure a **reminder** (N minutes before due date) that fires as an OS toast notification
- **Snooze** a reminder to silence it until a chosen date/time without deleting it

**Priority levels** (color-coded throughout the UI):
| Level | Color |
|---|---|
| Low | Blue-grey |
| Medium | Amber |
| High | Orange |
| Critical | Red |

**Status levels:**
| Status | Meaning |
|---|---|
| To Do | Not started |
| In Progress | Actively working |
| Done | Completed |
| Cancelled | Abandoned |

**Lifecycle / GTD buckets** (set via the defer field):
- **Inbox** — unprocessed capture
- **Anytime** — ready to work on, no date needed
- **Someday** — low urgency, not now
- **Scheduled** — defer until a future date (hidden from main views until then)

**Mass actions** — select multiple tasks at once for bulk operations:
- Tick individual checkboxes or use **Shift+click** for range selection
- **Ctrl+A** to select / deselect everything in the current list
- Bulk-set **status**, **priority**, or **life area** across the selection
- **Bulk delete** selected tasks in one click
- Selection count shown in a floating action bar above the list

---

### Views

Nexus has 19 purpose-built views, all reachable from the sidebar:

| View | Description |
|---|---|
| **Dashboard** | At-a-glance stats: active tasks, due today, overdue, Pomodoro minutes today. Weekly completion bar chart, life-area breakdown pie chart, top streak badges. |
| **Today** | All tasks due today, sorted by time. Optional time-block planner to schedule your day hour by hour. |
| **This Week** | Tasks grouped by day across the current ISO week (Mon–Sun). Helps you plan and review without switching between individual days. |
| **Calendar** | Monthly calendar grid. Each day shows colored dots for tasks due that day. Click a day to see its task list. |
| **Kanban** | Drag-and-drop board with one swimlane per status (To Do / In Progress / Done / Cancelled). Drag cards between columns to update status instantly. |
| **All Tasks** | Full flat list of all non-archived tasks. Live filter bar: filter by status, priority, life area, and free-text search simultaneously. |
| **Inbox** | GTD capture bucket — tasks with no life area or not yet processed land here for triage. |
| **Anytime** | GTD anytime bucket — tasks that are ready to work on whenever you have time, with no fixed date. |
| **Someday** | GTD someday/maybe bucket — low-urgency ideas you want to keep but not act on immediately. |
| **Scheduled** | Tasks deferred to a future date, hidden from other views until their defer date arrives. |
| **Goals** | Long-term goal cards with linked tasks, a progress bar computed from task completion ratio, and status (Active / Completed / Abandoned). |
| **Projects** | Color-coded project cards grouped under their parent life area. Each card shows start/due dates, task count, and live completion percentage. |
| **Streaks** | Full streak management page — every habit streak in one place, with edit, reset, and delete controls always visible. |
| **Pomodoro** | Focus timer with configurable work, short-break, and long-break intervals. Auto-advance option, session history per task, and today's focus minutes on the dashboard. |
| **Eisenhower Matrix** | 2×2 urgent/important quadrant board. Drag tasks between quadrants to reprioritize your workload visually. |
| **Review** | Structured weekly review: completed this week, tasks that slipped (overdue), and what's coming up next week. |
| **Archive** | Soft-deleted tasks. Browse, search, and restore individual tasks or bulk-restore all. |
| **Finance** | Personal finance tracker — income/expense transactions, monthly trend chart, category breakdown, and balance overrides in UZS and USD. |
| **Settings** | App preferences, backup/restore, iCal export, theme toggle, and About & Diagnostics panel. |

---

### Recurring Tasks

Build habits and repeating workflows with a first-class recurrence system:

- **Recurrence types:** Daily · Weekdays only · Weekly (pick which days) · Monthly · Yearly
- **Two scheduling modes:**
  - *Fixed* — next instance is always on the calendar schedule (e.g. every Monday)
  - *After completion* — next instance is calculated from when you completed the last one
- On startup Nexus automatically generates upcoming instances **14 days ahead** so your lists are always populated
- **Skip one instance** — mark a single occurrence as skipped without breaking the whole series
- Completing a recurring task automatically **creates or increments its streak entry** so habit tracking is zero-effort

---

### Streaks

Track any recurring habit as a streak — fully automatic or manually managed:

- **Auto-created** the first time you mark a recurring task done; updated every subsequent completion
- Tracks three counters per habit: **current streak**, **best streak ever**, and **last completed date**
- Visual state: active streak shows a flame icon; lapsed streak (no completion today or yesterday) shows a snowflake
- Dedicated **Streaks view** — every streak visible at once with inline action buttons:
  - **Edit** — rename the streak, change its life area, or manually adjust current/best counters (useful for retroactive imports)
  - **Reset** — zero out the current streak counter while keeping the best-streak record
  - **Delete** — permanently remove the streak entry (the underlying task is untouched)
- Streak badges also appear on the **Dashboard** for a quick motivational glance

---

### Goals

Keep your big-picture ambitions connected to your daily work:

- Create goals with a title, description, target date, and one or more life areas
- **Link tasks** to a goal — the goal's progress bar is automatically computed as `completed tasks / total linked tasks × 100`
- Three lifecycle statuses: **Active**, **Completed**, **Abandoned**
- When all linked tasks are done, the goal can be auto-marked as Completed
- Goals are visible in their own sidebar view and referenced in the weekly Review flow

---

### Projects

Organize multi-task efforts under named, color-coded projects:

- Each project belongs to a **life area** and appears nested under it in the sidebar
- Project card shows: name, color badge, start date, due date, status, task count, and **live completion %**
- Three statuses: **Active**, **Completed**, **Archived**
- Filter any task list by project using the task filter bar
- Projects are displayed as a card grid — scan progress across all active projects at a glance

---

### Life Areas (Categories)

Life areas are the top-level organizing buckets — think Work, Health, Side Projects, Personal:

- Create as many as you need, each with a **custom name and hex color**
- **Drag-to-reorder** in the sidebar; the order is persisted between sessions
- A task or goal can belong to **multiple life areas simultaneously** (M2M relationship)
- Projects are nested under their parent life area in the sidebar hierarchy
- Life area colors are used throughout the UI for color-coded dots, badges, and chart slices

---

### Finance Tracker

Track your personal income and expenses in two currencies with a clean, fully integrated finance dashboard:

**Transactions:**
- Log **Income** or **Expense** transactions with amount, currency (UZS / USD), category, description, and date
- Edit or delete any transaction inline without leaving the view
- Category suggestions auto-complete as you type (Salary, Freelance, Rent, Food, etc.)

**Dashboard stats (4 stat cards):**
| Card | Description |
|---|---|
| **Total Balance** | All-time net (income − expenses). Manually overridable. |
| **This Month Income** | Sum of all income transactions in the current calendar month. Manually overridable. |
| **This Month Expenses** | Sum of all expenses in the current calendar month. Manually overridable. |
| **This Month Net** | Derived: month income − month expenses (no override). |

- **Manual balance override** — hover any overridable card, click the pencil icon, type a new value, and press **Accept**. The override is stored persistently and shown with a small indicator dot. Click the reset icon to revert to the calculated value.
- Stat cards are currency-aware — toggle between **UZS** and **USD** in the top bar to see all figures in the selected currency simultaneously.

**Tabs:**
| Tab | Contents |
|---|---|
| **Overview** | Monthly trend bar chart (last 6 months), category breakdown by income and expense, 8 most recent transactions |
| **Income** | Full income history grouped by date, with a category breakdown |
| **Expenses** | Full expense history grouped by date, with a category breakdown |
| **All History** | Month navigator — step backward/forward through any month to see all transactions for that period |

**Charts & analytics:**
- **Monthly trend** — side-by-side income/expense bars for the last 6 months, filtered by the active currency
- **Category breakdown** — horizontal bar chart showing each category's share of total spending or income (currency-filtered)

**Data:**
- Stored in a dedicated `TRANSACTIONS` table (`DECIMAL(15,2)` amounts, VARCHAR category/description, indexed by date, currency, and type)
- Supports both **UZS** (Uzbekistani soʻm) and **USD** with fully independent totals
- Amounts are stored with 2 decimal places; the UI formats UZS without decimals and USD to 2 decimal places

---

### Pomodoro Timer

Stay focused with a built-in Pomodoro timer tightly integrated with your task list:

- **Three configurable intervals:** work session (default 25 min), short break (5 min), long break (15 min)
- Start a Pomodoro session linked to any task — actual focused minutes are logged to that task automatically
- **Auto-advance** mode transitions work → break → work without requiring you to click
- Session history is stored per task so you can see how many Pomodoros a task took
- Dashboard shows: **today's Pomodoro session count** and **this week's total focus minutes**
- Long break triggers automatically every 4 completed work sessions

---

### Reminders & Notifications

Never miss a deadline with multi-layer reminders:

- **Per-task reminder:** set N minutes before the due date/time (e.g. 15 minutes before)
- **OS toast notification** (Windows Action Center via ControlsFX) pops up at reminder time
- **In-app notification record** stored in the database — visible via the bell icon in the top bar
- **Bell badge** shows the unread notification count; click to open the notification panel
- **Missed reminder catchup:** on every startup, Nexus scans for any reminder whose trigger window fell in the last 24 hours while the app was closed and fires them retroactively
- **Snooze:** silence a reminder until a specific future time — the task stays on your list but notifications are suppressed until the snooze expires
- Background scanner runs every **60 seconds** using a dedicated daemon thread (minimal CPU)

---

### Search & Quick Actions

Speed up your workflow with keyboard-first capture and navigation:

- **Quick Add (Ctrl+N):** a minimal capture modal — type a task title and hit Enter. The task lands in your Inbox for later processing. Never lose an idea because switching context was too slow.
- **Search palette (Ctrl+K):** a full-text command palette that searches across all tasks and goals simultaneously. Results update as you type. Hit Enter to open the result.
- **Mark done (Ctrl+D):** instantly complete the currently focused task from any view
- **Natural language date parsing** via `chrono-node` — type "next Friday 3pm" in the date field and it parses it automatically

---

### Kanban Board

Visualize and manage your workflow with a drag-and-drop board:

- Four columns: **To Do**, **In Progress**, **Done**, **Cancelled**
- Each card shows: title, priority color strip, due date, life area badge, and subtask completion ratio
- Drag a card to a different column to instantly update its status in the database
- Cards within a column are sorted by priority (Critical → High → Medium → Low) then by due date
- Filter the board by life area or search term using the top filter bar

---

### Eisenhower Matrix

Prioritize ruthlessly with the classic urgent/important framework:

- Four quadrants: **Do First** (urgent + important), **Schedule** (not urgent + important), **Delegate** (urgent + not important), **Eliminate** (not urgent + not important)
- Drag tasks between quadrants — the underlying priority and labels are updated accordingly
- Quadrant counts shown in each header so you can see where your time is being pulled
- Color-coded cards match the global priority color scheme

---

### GTD Buckets

Nexus implements the core GTD capture-and-clarify workflow with dedicated views:

- **Inbox** — the default landing zone for quick-captured tasks. Process regularly by assigning life areas, due dates, and projects.
- **Anytime** — tasks that are actionable now but have no fixed date. Your ready-to-pull queue.
- **Someday** — ideas and aspirations to revisit when priorities shift. Kept out of your active views.
- **Scheduled** — tasks deferred to a future date using the "defer until" field. Invisible in all other views until the defer date arrives, then they surface automatically.

---

### Review Flow

Build a sustainable weekly review habit:

- Three sections in one view: **Completed this week**, **Overdue (slipped)**, **Coming up next week**
- Completed tasks show a checkmark with their completion timestamp
- Overdue tasks show how many days they are past due with a reschedule shortcut
- Upcoming tasks show due dates grouped by day
- Designed to be the last thing you do on Friday — close the week, plan the next

---

### Import / Export & Backup

Your data is always yours:

- **JSON full export** — exports the entire database (tasks, goals, categories, projects, streaks, settings) as a structured JSON file. Useful for migration or manual inspection.
- **JSON import** — restore from any previously exported JSON backup. Merges or replaces existing data.
- **iCalendar (.ics) export** — all tasks with due dates are exported as `VEVENT` entries, importable into Apple Calendar, Google Calendar, Outlook, or any standard calendar app.
- **Auto-backup** — scheduled automatic backup to a configurable directory (set in Settings). Runs in the background so you always have a recent copy.
- **Diagnostics export** — one-click export of a ZIP containing recent log files and a redacted settings dump (values masked). Attach to a bug report without exposing personal data.

---

### Settings & Diagnostics

- **Theme toggle** — switch between dark (default navy canvas) and light modes; preference persisted to the database
- **Backup directory** — configure where auto-backups are written
- **Pomodoro intervals** — customize work, short break, and long break durations
- **About panel** — displays app version, Java version, OS info, database file path and size, entity counts (tasks, goals, categories), and current Flyway schema version
- **Diagnostics export** — one-click ZIP of logs + redacted settings for support
- All preferences stored in an `app_settings` key-value table in H2 (not flat files)

---

### Window & System Integration

Nexus feels like a native app, not an embedded web page:

- **Custom undecorated window** — no OS title bar; the top bar is drawn entirely in React with pixel-perfect controls
- **Draggable title bar** — click and drag anywhere on the top bar to move the window
- **Double-click to maximize** — double-click the title bar to toggle maximize/restore
- **Window controls** — minimize (−), maximize/restore (□), and close (×) in the top-right corner, all wired through the Java bridge to the JavaFX Stage
- **Window state persistence** — position, size, and maximized state saved to `~/.nexus/data/window-state.json` and restored on next launch; pre-maximized bounds saved correctly so restoring from maximized returns to the right size
- **Single-instance lock** — a TCP port lock (47291) prevents a second process from opening if Nexus is already running
- **System tray icon** — Nexus minimizes to the Windows system tray; right-click the tray icon for Quick Add and Show/Hide actions
- **Minimize to tray** — closing the window minimizes to tray instead of quitting; use the tray menu to exit fully
- **Global hotkeys** — Ctrl+N, Ctrl+K, Ctrl+D registered system-wide so they work even when Nexus is in the background

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+N` | Open Quick Add task capture modal |
| `Ctrl+K` | Open full-text search / command palette |
| `Ctrl+D` | Mark the focused task as Done |
| `Ctrl+A` | Select / deselect all tasks in the current list |
| `Shift+Click` | Range-select tasks for mass actions |
| `Escape` | Close dialogs / clear current selection |
| `Enter` | Confirm dialog / save form |
| `Double-click title bar` | Toggle maximize / restore window |

---

## Tech Stack

### Backend

| Layer | Library / Version |
|---|---|
| Desktop shell | JavaFX 21.0.5 — undecorated `Stage` + `WebView` |
| Database | H2 2.2 — embedded file database (`~/.nexus/data/nexus.mv.db`) |
| SQL | JOOQ 3.19 — type-safe, code-generated query DSL |
| Migrations | Flyway 10 — versioned SQL migration scripts |
| Connection pool | HikariCP 5 |
| Serialization | Jackson 2.17 + `JavaTimeModule` |
| Utility | Lombok |
| Logging | SLF4J 2 + Logback 1.5 (rolling file, 7-day retention) |
| OS notifications | ControlsFX 11 (Windows toast via `Notifications.create()`) |
| Testing | JUnit 5, Mockito 5, AssertJ |
| Native packaging | jpackage (Java 14+, bundled with JDK 21) |

### Frontend

| Layer | Library / Version |
|---|---|
| Framework | React 19 + TypeScript |
| Bundler | Vite 8 — code-split (each view is a separate lazy chunk) |
| Styling | Tailwind CSS 3 |
| Charts | Recharts 3 |
| Drag & drop | @dnd-kit/core + @dnd-kit/sortable |
| Icons | lucide-react |
| Date parsing | date-fns 4, chrono-node 2 (natural language date input) |
| Markdown | react-markdown 9 + remark-gfm + rehype-sanitize |

---

## Architecture

```
JavaFX Application (NexusApp.java)
└── Stage  ·  undecorated, 1280×800 default
    └── WebView (WebKit engine)
        └── React SPA  (file:// from temp-extracted webui/ bundle)
            └── window.nexusBridge  ←→  NexusBridge.java (JSObject)
                                         ├── TaskBridge       tasks.*
                                         ├── GoalBridge       goals.*
                                         ├── DashboardBridge  dashboard.*
                                         ├── PlanningBridge   planning.*
                                         ├── ProjectBridge    projects.*
                                         ├── FinanceBridge    finance.*
                                         └── WindowBridge     win.*
```

### Bridge pattern

The React frontend communicates with the Java backend exclusively through a typed
bidirectional bridge object injected into the WebView's JavaScript context:

- **React → Java:** `window.nexusBridge.<subBridge>.<method>(jsonArgs)` — synchronous JSON string in / JSON string out. Every sub-bridge method accepts a single JSON argument and returns a JSON result.
- **Java → React:** `window.onBridgeEvent(eventJson)` — an async push mechanism. The Java side calls this via `Platform.runLater()` to deliver notifications, hotkey events, and reminder triggers to the frontend without polling.

**Window control proxies** — `minimizeWindow`, `maximizeWindow`, `closeWindow`, `startDrag`,
`dragWindow`, `toggleMaximize` are exposed directly on the top-level `NexusBridge` object rather
than the sub-bridge, because JavaFX WebKit's `JSObject` field traversal is unreliable for
accessing nested Java objects. Calling through the top-level registered object is always safe.

### Data flow

```
React component
  → bridge.ts (typed TS wrapper)
    → window.nexusBridge.tasks.getTask("{id:1}")
      → TaskBridge.getTask(String json)       ← Java
        → TaskService.findById(1)
          → TaskRepository (JOOQ SELECT)
            → H2 file database
          ← Task domain object
        ← Jackson JSON serialization
      ← "{id:1, title:...}"
    ← parsed TypeScript object
  ← React state update → re-render
```

---

## Requirements

| Tool | Version |
|---|---|
| Java (JDK) | 21 LTS — [download Temurin](https://adoptium.net) |
| Maven | 3.9+ — bundled with IntelliJ or [download](https://maven.apache.org) |
| Node.js | 20+ — only needed if you modify the frontend |

---

## Running in Development

### 1. Generate JOOQ sources (required before first compile)

```bash
mvn generate-sources
```

JOOQ runs Flyway against a throw-away H2 file in `target/`, reads the schema, and emits
type-safe Java classes into `target/generated-sources/jooq/`.

> **IntelliJ tip:** right-click `target/generated-sources/jooq` → **Mark Directory as → Sources Root**

### 2. Run the app

```bash
mvn javafx:run
```

### 3. Frontend hot-reload (optional)

The compiled React bundle is already committed to `src/main/resources/webui/` so you can run
the app without touching Node. If you want to modify the frontend:

```bash
cd src/main/webui
npm install
npm run dev       # Vite dev server — browser-only preview (no Java bridge)
npm run build     # compile → src/main/resources/webui/  (picked up by the app)
```

On first launch Nexus will:
1. Create `~/.nexus/data/` and `~/.nexus/logs/`
2. Apply all Flyway migrations (V1–V14) automatically
3. Seed default life-area categories and sample tasks
4. Acquire the single-instance TCP lock and open the window

---

## Running Tests

```bash
mvn test
```

**100 unit tests across 12 service classes — all pass.**

| Test class | Tests |
|---|---|
| `BackupServiceTest` | 5 |
| `CategoryServiceTest` | 10 |
| `GoalServiceTest` | 8 |
| `ICalExportServiceTest` | 5 |
| `PomodoroServiceTest` | 7 |
| `ProjectServiceTest` | 11 |
| `RecurrenceServiceTest` | 8 |
| `ReminderServiceTest` | 5 |
| `SettingsServiceTest` | 13 |
| `StreakServiceTest` | 7 |
| `TaskServiceTest` | 13 |
| `TimeBlockServiceTest` | 8 |
| **Total** | **100** |

Tests cover happy paths, edge cases, and error handling for all major service classes.
Service tests use Mockito mocks for repositories to remain fast and deterministic (no H2 instance needed).

---

## Building a Native Windows App

Run `package.bat` from the project root (double-click or run from a terminal):

```
package.bat
```

This script:
1. Sets `JAVA_HOME` to the bundled JDK
2. Runs `mvn package -Ppackage -DskipTests` to compile and assemble `target/dist-input/`
3. Runs `jpackage` to create a self-contained app-image at `dist/Nexus/`
4. Moves JavaFX platform JARs into `dist/Nexus/app/mods/` and removes the no-classifier stubs

The result is `dist/Nexus/Nexus.exe` — **no JRE or Java installation required** on the target machine; everything is bundled.

### Installing on your machine

1. The `dist/Nexus/` folder is already in place after the build
2. A **desktop shortcut** (`Nexus.lnk`) is created automatically — just double-click to launch
3. To pin to taskbar: right-click the desktop shortcut → **Pin to taskbar**
4. To move the app: copy the entire `dist/Nexus/` folder anywhere (e.g. `C:\Users\<you>\Apps\Nexus\`) and update the shortcut target path

---

## Project Structure

```
src/main/java/com/nexus/
├── NexusApp.java               ← JavaFX Application entry point (window, tray, hotkeys, lifecycle)
├── Main.java                   ← jpackage-compatible launcher (no JavaFX on bootstrap classpath)
├── config/
│   ├── AppContext.java         ← manual DI container — all singletons wired and started here
│   ├── DatabaseConfig.java     ← HikariCP datasource + Flyway migration runner
│   └── JooqConfig.java        ← JOOQ DSLContext factory
├── model/                      ← immutable domain objects and enums
│   ├── Task.java               ← core task with all fields (subtasks, tags, recurrence, snooze…)
│   ├── Subtask.java
│   ├── Goal.java
│   ├── Project.java
│   ├── Category.java           ← life area
│   ├── Tag.java
│   ├── RecurrenceRule.java
│   ├── TimeBlock.java
│   ├── PomodoroSession.java
│   ├── Streak.java
│   ├── AppNotification.java
│   ├── Transaction.java        ← finance transaction (income/expense)
│   └── enums/
│       ├── Priority.java       ← LOW, MEDIUM, HIGH, CRITICAL
│       ├── TaskStatus.java     ← TODO, IN_PROGRESS, DONE, CANCELLED
│       ├── RecurrenceType.java ← DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY
│       ├── Lifecycle.java      ← INBOX, ANYTIME, SOMEDAY, SCHEDULED
│       └── GoalStatus.java     ← ACTIVE, COMPLETED, ABANDONED
├── repository/                 ← JOOQ-backed data access with dynamic WHERE builders
│   ├── TaskRepository.java
│   ├── GoalRepository.java
│   ├── TagRepository.java
│   ├── RecurrenceRuleRepository.java
│   ├── NotificationRepository.java
│   ├── PomodoroSessionRepository.java
│   └── TransactionRepository.java  ← finance transaction CRUD
├── service/
│   ├── TaskService.java        ← CRUD, status transitions, recurrence hooks, mass actions
│   ├── GoalService.java        ← progress tracking via linked tasks, auto-completion
│   ├── RecurrenceService.java  ← 14-day lookahead generation, skip-instance logic
│   ├── StreakService.java      ← auto-create, update, expiry detection, reset, delete
│   ├── ReminderService.java    ← 60s background scan + startup catchup (last 24h)
│   ├── PomodoroService.java    ← session start / complete / abandon, minutes rollup
│   ├── TimeBlockService.java   ← calendar time block CRUD
│   ├── BackupService.java      ← scheduled auto-backup via ExportService
│   ├── ICalExportService.java  ← iCalendar VEVENT generation from tasks
│   ├── ExportService.java      ← JSON full export / import
│   ├── CategoryService.java    ← life area CRUD + drag-reorder position persistence
│   ├── ProjectService.java     ← project CRUD + live task stats computation
│   ├── SettingsService.java    ← key-value settings backed by H2 app_settings table
│   ├── TransactionService.java ← finance transaction CRUD + currency totals
│   ├── NotificationService.java ← in-app notification record CRUD
│   └── SystemTrayService.java  ← Windows system tray icon, context menu, minimize-to-tray
└── ui/
    ├── NexusBridge.java        ← top-level JS bridge + window control proxies
    ├── MainWindow.java         ← JavaFX root pane, WebView wiring, bridge injection
    └── bridge/
        ├── TaskBridge.java     ← tasks.* — CRUD, subtasks, tags, recurrence, mass actions
        ├── GoalBridge.java     ← goals.* — goal CRUD and status
        ├── DashboardBridge.java ← dashboard.* — stats, streaks, import/export
        ├── PlanningBridge.java ← planning.* — time blocks, categories, tags, Pomodoro
        ├── ProjectBridge.java  ← projects.* — project CRUD and task stats
        ├── FinanceBridge.java  ← finance.* — transactions CRUD, stats, balance overrides
        ├── WindowBridge.java   ← win.* — window controls, notifications, settings, file pickers
        └── BridgeDtos.java     ← lightweight JSON DTOs (no domain object leakage to JS layer)

src/main/webui/src/
├── App.tsx                     ← root component: view router, data loading, bridge event handler
├── bridge.ts                   ← fully typed TypeScript wrapper for window.nexusBridge
├── types.ts                    ← shared domain types (Task, Goal, Streak, Project…)
├── components/
│   ├── Sidebar.tsx             ← nav links, life areas (drag-to-reorder), project tree
│   ├── TopBar.tsx              ← window chrome: drag handle, quick add, search, bell, theme toggle
│   ├── QuickAdd.tsx            ← rapid task capture modal (Ctrl+N)
│   ├── SearchPalette.tsx       ← full-text command palette (Ctrl+K)
│   ├── TaskDialog.tsx          ← full task editor: subtasks, tags, recurrence, reminders, snooze
│   ├── SubtaskList.tsx         ← drag-to-reorder subtask checklist
│   ├── TagPicker.tsx           ← tag create / assign with hex color picker
│   ├── DatePicker.tsx          ← date + time picker with natural language input
│   ├── DurationPicker.tsx      ← hour/minute duration input
│   └── ToastStack.tsx          ← in-app toast notification stack
└── views/                      ← all lazy-loaded via React.lazy + Suspense
    ├── Dashboard.tsx           ← stats, charts, streak badges
    ├── Today.tsx               ← due today + time-block planner
    ├── Week.tsx                ← tasks grouped by ISO week day
    ├── Calendar.tsx            ← monthly calendar with task dots
    ├── Kanban.tsx              ← drag-and-drop status board
    ├── TaskList.tsx            ← full list with filter bar and mass actions
    ├── Inbox.tsx               ← GTD inbox bucket
    ├── Anytime.tsx             ← GTD anytime bucket
    ├── Someday.tsx             ← GTD someday bucket
    ├── Scheduled.tsx           ← deferred tasks
    ├── Projects.tsx            ← project cards with live stats
    ├── Goals.tsx               ← goal cards with progress bars
    ├── Streaks.tsx             ← streak management (edit / reset / delete)
    ├── Pomodoro.tsx            ← focus timer with session history
    ├── Eisenhower.tsx          ← 2×2 urgent/important quadrant board
    ├── Review.tsx              ← weekly review: done / overdue / upcoming
    ├── Finance.tsx             ← finance tracker: transactions, stats, charts, overrides
    ├── Settings.tsx            ← preferences, backup, export, about, diagnostics
    └── Archive.tsx             ← soft-deleted tasks with bulk restore

src/main/resources/
├── db/migration/               ← Flyway SQL migration scripts V1–V14
├── app.properties              ← build-time version info (injected by Maven)
└── webui/                      ← compiled React bundle (committed — no Node needed to run)

src/test/java/com/nexus/service/
└── *ServiceTest.java           ← 12 test files, 100 tests total
```

---

## Database Migrations

| Version | Contents |
|---|---|
| V1 | Initial schema: tasks, categories, subtasks, tags, task_tags join table |
| V2 | Seed data: default life areas (Work, Health, Side Projects, Personal, University) + sample tasks |
| V3 | Performance indexes on frequently queried columns |
| V4 | Category colour field + display order (position) column |
| V5 | Streaks table: current_streak, best_streak, last_completed_date, linked recurrence rule |
| V6 | Task enhancements: start_time, estimated_minutes, actual_minutes fields |
| V7 | Subtasks table, tags table, projects table, JSON import/export schema support |
| V8 | Multi-category support via task_categories join table (tasks can belong to many life areas) |
| V9 | Settings persistence: app_settings key-value table |
| V10 | Recurrence system v2: recurrence_rules table with mode (fixed / after-completion) and skip tracking |
| V11 | Task snooze: snoozed_until field; notifications table for in-app notification records |
| V12 | Defer/lifecycle buckets (defer_until, lifecycle enum), recurrence after-completion mode finalised |
| V13 | Task templates table + energy log table for future energy-level tracking |
| V14 | Finance schema: `TRANSACTIONS` table (id, type, amount `DECIMAL(15,2)`, currency, category, description, txn_date, created_at) with indexes on date, currency, and type |

---

## Data Location

| Path | Purpose |
|---|---|
| `~/.nexus/data/nexus.mv.db` | Production H2 database (all tasks, goals, settings) |
| `~/.nexus/data/window-state.json` | Saved window position, size, and maximized state |
| `~/.nexus/logs/nexus.log` | Rolling log file (7-day retention, ~5 MB max per file) |
| `~/.nexus/backups/` | Auto-backup destination (configurable in Settings → Backup) |

`~` is `%USERPROFILE%` on Windows (e.g. `C:\Users\YourName`).

---

## CI / CD

GitHub Actions runs on every push and pull request to `master`:

### `java` job
- Runs on Ubuntu with Xvfb (headless display for JavaFX)
- Java 21 Temurin
- `mvn verify` — compiles, generates JOOQ sources, and runs all 100 tests

### `frontend` job
- Node 20
- `npm ci && npm run lint && tsc --noEmit && npm run build`
- Verifies TypeScript types, linting, and that the bundle compiles cleanly

### `release` workflow (manual trigger)
- Builds the Maven package with the `package` profile
- Runs jpackage to produce a Windows app-image
- Creates a GitHub Release with the `dist/Nexus/` folder as a downloadable asset
