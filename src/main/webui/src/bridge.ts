/**
 * Typed bridge to the JavaFX backend.
 * In production, window.nexusBridge is injected by NexusBridge.java.
 * In dev (browser), mock data is returned.
 */

import type { Task, Category, Goal, Project, TimeBlock, DashboardStats, MonthlyStats, Notification, Subtask, Tag } from './types'

declare global {
  interface Window {
    nexusBridge?: {
      tasks: {
        getTasks(filterJson: string): string
        getArchivedTasks(): string
        createTask(json: string): string
        updateTask(json: string): string
        deleteTask(id: number): void
        archiveTask(id: number): void
        restoreTask(id: number): void
        markDone(id: number): string
        markInProgress(id: number): void
        snoozeTask(taskId: number, minutes: number): void
        getSubtasks(taskId: number): string
        createSubtask(json: string): string
        toggleSubtask(subtaskId: number): string
        deleteSubtask(subtaskId: number): void
        skipRecurringInstance(taskId: number): void
        setTaskCategories(taskId: number, categoryIdsJson: string): void
        setTaskTags(taskId: number, tagIdsJson: string): string
        reorderSubtasks(taskId: number, orderedIdsJson: string): void
      }
      goals: {
        getGoals(): string
        createGoal(json: string): string
        updateGoal(json: string): string
        updateGoalStatus(id: number, status: string): void
        deleteGoal(id: number): void
        setGoalCategories(goalId: number, categoryIdsJson: string): void
      }
      dashboard: {
        getDashboardStats(): string
        getMonthlyStats(): string
        adjustStat(key: string, delta: number): void
        resetStatAdjustments(): void
        importData(filePath: string): string
        exportData(path: string): string
        exportIcal(path: string): string
        backupNow(): void
        getStreaks(): string
        updateStreak(json: string): string
        deleteStreak(id: number): void
      }
      planning: {
        getTimeBlocks(date: string): string
        getTodayBlocks(date: string): string
        createTimeBlock(json: string): string
        deleteTimeBlock(id: number): void
        getCategories(): string
        createCategory(json: string): string
        updateCategory(json: string): string
        deleteCategory(id: number): void
        getTags(): string
        createTag(json: string): string
        deleteTag(tagId: number): void
        updateRecurrenceRule(json: string): string
        logPomodoro(): void
        startPomodoroSession(taskId: number, minutes: number): string
        completePomodoroSession(sessionId: number): void
        abandonPomodoroSession(sessionId: number): void
        getPomodoroCount(taskId: number): number
        playAlarm(type: string): void
      }
      projects: {
        getProjects(): string
        getProjectsByCategory(categoryId: number): string
        createProject(json: string): string
        updateProject(json: string): string
        deleteProject(id: number): void
        getProjectTaskCount(projectId: number): number
        getProjectProgress(projectId: number): number
      }
      win: {
        minimizeWindow(): void
        maximizeWindow(): void
        closeWindow(): void
        startDrag(screenX: number, screenY: number): void
        dragWindow(screenX: number, screenY: number): void
        toggleMaximize(): void
        getNotifications(): string
        markNotificationRead(id: number): void
        getSettings(): string
        setSetting(key: string, value: string): void
        chooseFolder(title: string): string
        chooseFile(title: string, ext: string): string
        reorderCategories(orderedIdsJson: string): void
        getAppInfo(): string
        exportDiagnostics(): string
      }
      // Window control proxies (called directly to avoid sub-bridge field traversal)
      minimizeWindow(): void
      maximizeWindow(): void
      toggleMaximize(): void
      closeWindow(): void
      startDrag(screenX: number, screenY: number): void
      dragWindow(screenX: number, screenY: number): void
      init(jsWindow: unknown): void
      pushEvent(type: string, payload: unknown): void
    }
    onBridgeEvent?: (eventJson: string) => void
  }
}

function call<T>(fn: () => string): T {
  try { return JSON.parse(fn()) as T }
  catch { return [] as unknown as T }
}

const b = () => window.nexusBridge

// ── Window controls ───────────────────────────────────────────────────────
// Called directly on the top-level bridge (not via sub-bridge field) for
// reliable JSObject dispatch in JavaFX WebKit.

export function minimizeWindow()  { b()?.minimizeWindow()  }
export function maximizeWindow()  { b()?.maximizeWindow()  }
export function toggleMaximize()  { b()?.toggleMaximize()  }
export function closeWindow()     { b()?.closeWindow()     }
export function startDrag(screenX: number, screenY: number)  { b()?.startDrag(screenX, screenY)  }
export function dragWindow(screenX: number, screenY: number) { b()?.dragWindow(screenX, screenY) }

// ── About / Diagnostics ───────────────────────────────────────────────────

export interface AppInfo {
  version:       string
  java:          string
  os:            string
  dbPath:        string
  dbSize:        string
  taskCount:     number
  goalCount:     number
  categoryCount: number
  schemaVersion: string
}

export function getAppInfo(): AppInfo | null {
  if (!b()) return null
  return call(() => b()!.win.getAppInfo())
}

export function exportDiagnostics(): string | null {
  if (!b()) return null
  return call(() => b()!.win.exportDiagnostics())
}

// ── Tasks ──────────────────────────────────────────────────────────────────

export function getTasks(filter: Record<string, unknown> = {}): Task[] {
  if (!b()) return MOCK_TASKS
  // showDeferred is a client-side-only concept that maps to a different bridge call
  if (filter.showDeferred) {
    const { showDeferred: _, ...rest } = filter
    return call(() => b()!.tasks.getTasks(JSON.stringify({ ...rest, showDeferred: true })))
  }
  return call(() => b()!.tasks.getTasks(JSON.stringify(filter)))
}

export function getArchivedTasks(): Task[] {
  if (!b()) return []
  return call(() => b()!.tasks.getArchivedTasks())
}

export function createTask(data: Partial<Task>): Task | null {
  if (!b()) return null
  return call(() => b()!.tasks.createTask(JSON.stringify(data)))
}

export function updateTask(data: Partial<Task> & { id: number }): Task | null {
  if (!b()) return null
  return call(() => b()!.tasks.updateTask(JSON.stringify(data)))
}

export function deleteTask(id: number) {
  b()?.tasks.deleteTask(id)
}

export function archiveTask(id: number) {
  b()?.tasks.archiveTask(id)
}

export function restoreTask(id: number) {
  b()?.tasks.restoreTask(id)
}

export function markDone(id: number): Task | null {
  if (!b()) return null
  return call(() => b()!.tasks.markDone(id))
}

export function markInProgress(id: number) {
  b()?.tasks.markInProgress(id)
}

export function snoozeTask(taskId: number, minutes: number) {
  b()?.tasks.snoozeTask(taskId, minutes)
}

// ── Categories ────────────────────────────────────────────────────────────

export function getCategories(): Category[] {
  if (!b()) return MOCK_CATS
  return call(() => b()!.planning.getCategories())
}

export function createCategory(name: string, color: string): Category | null {
  if (!b()) return null
  return call(() => b()!.planning.createCategory(JSON.stringify({ name, color })))
}

export function updateCategory(data: { id: number; name?: string; color?: string }): Category | null {
  if (!b()) return null
  return call(() => b()!.planning.updateCategory(JSON.stringify(data)))
}

export function deleteCategory(id: number) {
  b()?.planning.deleteCategory(id)
}

// ── Projects ──────────────────────────────────────────────────────────────

export function getProjects(): Project[] {
  if (!b()) return MOCK_PROJECTS
  return call(() => b()!.projects.getProjects())
}

export function getProjectsByCategory(categoryId: number): Project[] {
  if (!b()) return []
  return call(() => b()!.projects.getProjectsByCategory(categoryId))
}

export function createProject(data: Partial<Project>): Project | null {
  if (!b()) return null
  return call(() => b()!.projects.createProject(JSON.stringify(data)))
}

export function updateProject(data: Partial<Project> & { id: number }): Project | null {
  if (!b()) return null
  return call(() => b()!.projects.updateProject(JSON.stringify(data)))
}

export function deleteProject(id: number) {
  b()?.projects.deleteProject(id)
}

export function getProjectTaskCount(projectId: number): number {
  if (!b()) return 0
  return b()!.projects.getProjectTaskCount(projectId)
}

export function getProjectProgress(projectId: number): number {
  if (!b()) return 0
  return b()!.projects.getProjectProgress(projectId)
}

// ── Goals ─────────────────────────────────────────────────────────────────

export function getGoals(): Goal[] {
  if (!b()) return MOCK_GOALS
  return call(() => b()!.goals.getGoals())
}

export function createGoal(data: Partial<Goal>): Goal | null {
  if (!b()) return null
  return call(() => b()!.goals.createGoal(JSON.stringify(data)))
}

export function updateGoal(data: Partial<Goal> & { id: number }): Goal | null {
  if (!b()) return null
  return call(() => b()!.goals.updateGoal(JSON.stringify(data)))
}

export function updateGoalStatus(id: number, status: string) {
  b()?.goals.updateGoalStatus(id, status)
}

export function deleteGoal(id: number) {
  b()?.goals.deleteGoal(id)
}

// ── Dashboard ─────────────────────────────────────────────────────────────

export function getDashboardStats(): DashboardStats {
  if (!b()) return MOCK_STATS
  return call(() => b()!.dashboard.getDashboardStats())
}

export function getMonthlyStats(): MonthlyStats[] {
  if (!b()) return MOCK_MONTHLY
  return call(() => b()!.dashboard.getMonthlyStats())
}

export function adjustStat(key: string, delta: number) {
  b()?.dashboard.adjustStat(key, delta)
}

export function resetStatAdjustments() {
  b()?.dashboard.resetStatAdjustments()
}

// ── Time blocks ───────────────────────────────────────────────────────────

export function getTimeBlocks(date?: string): TimeBlock[] {
  if (!b()) return MOCK_BLOCKS
  const d = date ?? new Date().toISOString().split('T')[0]
  return call(() => b()!.planning.getTimeBlocks(d))
}

export function getTodayBlocks(date: string): TimeBlock[] {
  if (!b()) return []
  return call(() => b()!.planning.getTodayBlocks(date))
}

export function createTimeBlock(data: Partial<TimeBlock>): TimeBlock | null {
  if (!b()) return null
  return call(() => b()!.planning.createTimeBlock(JSON.stringify(data)))
}

export function deleteTimeBlock(id: number) {
  b()?.planning.deleteTimeBlock(id)
}

// ── Notifications ─────────────────────────────────────────────────────────

export function getNotifications(): Notification[] {
  if (!b()) return []
  return call(() => b()!.win.getNotifications())
}

export function markNotificationRead(id: number) {
  b()?.win.markNotificationRead(id)
}

// ── Pomodoro ──────────────────────────────────────────────────────────────

export function logPomodoro() {
  b()?.planning.logPomodoro()
}

export function startPomodoroSession(taskId: number, minutes: number): number {
  if (!b()) return 0
  const r = call<{ sessionId: number }>(() => b()!.planning.startPomodoroSession(taskId, minutes))
  return r?.sessionId ?? 0
}

export function completePomodoroSession(sessionId: number) {
  b()?.planning.completePomodoroSession(sessionId)
}

export function abandonPomodoroSession(sessionId: number) {
  b()?.planning.abandonPomodoroSession(sessionId)
}

export function getPomodoroCount(taskId: number): number {
  if (!b()) return 0
  return b()!.planning.getPomodoroCount(taskId)
}

// ── Export / Import ───────────────────────────────────────────────────────

export function exportData(path: string): string {
  if (!b()) return ''
  return b()!.dashboard.exportData(path)
}

export function importData(filePath: string): { imported: number; skipped: number; errors: string[] } | null {
  if (!b()) return null
  return call(() => b()!.dashboard.importData(filePath))
}

export function exportIcal(path: string): string {
  if (!b()) return ''
  return b()!.dashboard.exportIcal(path)
}

export function backupNow() {
  b()?.dashboard.backupNow()
}

export function getStreaks(): import('./types').Streak[] {
  if (!b()) return []
  return call(() => b()!.dashboard.getStreaks())
}

export function updateStreak(data: Partial<import('./types').Streak> & { id: number }): import('./types').Streak | null {
  if (!b()) return null
  return call(() => b()!.dashboard.updateStreak(JSON.stringify(data)))
}

export function deleteStreak(id: number) {
  b()?.dashboard.deleteStreak(id)
}

export function chooseFolder(title: string): string | null {
  if (!b()) return null
  const r = b()!.win.chooseFolder(title)
  try { return JSON.parse(r) } catch { return null }
}

export function chooseFile(title: string, ext: string): string | null {
  if (!b()) return null
  const r = b()!.win.chooseFile(title, ext)
  try { return JSON.parse(r) } catch { return null }
}

// ── Subtasks ──────────────────────────────────────────────────────────────

export function getSubtasks(taskId: number): Subtask[] {
  if (!b()) return []
  return call(() => b()!.tasks.getSubtasks(taskId))
}

export function createSubtask(data: { taskId: number; title: string }): Subtask | null {
  if (!b()) return null
  return call(() => b()!.tasks.createSubtask(JSON.stringify(data)))
}

export function toggleSubtask(id: number): Subtask | null {
  if (!b()) return null
  return call(() => b()!.tasks.toggleSubtask(id))
}

export function deleteSubtask(id: number) {
  b()?.tasks.deleteSubtask(id)
}

export function reorderSubtasks(taskId: number, orderedIds: number[]) {
  b()?.tasks.reorderSubtasks(taskId, JSON.stringify(orderedIds))
}

// ── Tags ──────────────────────────────────────────────────────────────────

export function getTags(): Tag[] {
  if (!b()) return []
  return call(() => b()!.planning.getTags())
}

export function createTag(data: { name: string; color: string }): Tag | null {
  if (!b()) return null
  return call(() => b()!.planning.createTag(JSON.stringify(data)))
}

export function setTaskTags(taskId: number, tagIds: number[]): Tag[] {
  if (!b()) return []
  return call(() => b()!.tasks.setTaskTags(taskId, JSON.stringify(tagIds)))
}

export function deleteTag(tagId: number) {
  b()?.planning.deleteTag(tagId)
}

// ── Recurring ─────────────────────────────────────────────────────────────

export function skipRecurringInstance(taskId: number) {
  b()?.tasks.skipRecurringInstance(taskId)
}

export function setTaskCategories(taskId: number, categoryIds: number[]) {
  b()?.tasks.setTaskCategories(taskId, JSON.stringify(categoryIds))
}

export function setGoalCategories(goalId: number, categoryIds: number[]) {
  b()?.goals.setGoalCategories(goalId, JSON.stringify(categoryIds))
}

export function updateRecurrenceRule(data: {
  ruleId: number; type?: string; daysOfWeek?: string; endDate?: string
  dayOfMonth?: number; monthOfYear?: number; mode?: string
}): object | null {
  if (!b()) return null
  return call(() => b()!.planning.updateRecurrenceRule(JSON.stringify(data)))
}

export function reorderCategories(orderedIds: number[]) {
  b()?.win.reorderCategories(JSON.stringify(orderedIds))
}

// ── Settings ──────────────────────────────────────────────────────────────

export function getSettings(): Record<string, string> {
  if (!b()) return {}
  return call(() => b()!.win.getSettings())
}

export function setSetting(key: string, value: string) {
  b()?.win.setSetting(key, value)
}

// ── Mock data (dev mode) ──────────────────────────────────────────────────

const MOCK_PROJECTS: Project[] = []

const MOCK_CATS: Category[] = [
  { id: 1, name: 'Work',      color: '#6366f1', position: 1 },
  { id: 2, name: 'Health',    color: '#10b981', position: 2 },
  { id: 3, name: 'Personal',  color: '#f59e0b', position: 3 },
]

const MOCK_TASKS: Task[] = [
  {
    id: 1, title: 'Finish project proposal', description: 'Q3 planning doc',
    category: MOCK_CATS[0], categoryId: 1, categoryIds: [1], categories: [MOCK_CATS[0]],
    priority: 'HIGH', status: 'IN_PROGRESS', lifecycle: 'ANYTIME',
    dueDate: new Date().toISOString(), urgent: true, important: true,
    archived: false, tags: [{ id: 1, name: 'work', color: '#6366f1' }],
    estimatedMinutes: 90, createdAt: '', updatedAt: '',
  },
  {
    id: 2, title: 'Morning kickboxing session', description: '',
    category: MOCK_CATS[1], categoryId: 2, categoryIds: [2], categories: [MOCK_CATS[1]],
    priority: 'MEDIUM', status: 'TODO', lifecycle: 'ANYTIME',
    dueDate: new Date().toISOString(), urgent: false, important: true,
    archived: false, tags: [], estimatedMinutes: 60, createdAt: '', updatedAt: '',
  },
  {
    id: 3, title: 'Review pull requests', description: '',
    category: MOCK_CATS[0], categoryId: 1, categoryIds: [1], categories: [MOCK_CATS[0]],
    priority: 'CRITICAL', status: 'TODO', lifecycle: 'ANYTIME',
    urgent: true, important: true, archived: false, tags: [], createdAt: '', updatedAt: '',
  },
  {
    id: 4, title: 'Read 30 pages', description: '',
    category: MOCK_CATS[2], categoryId: 3, categoryIds: [3], categories: [MOCK_CATS[2]],
    priority: 'LOW', status: 'TODO', lifecycle: 'ANYTIME',
    urgent: false, important: false, archived: false, tags: [], createdAt: '', updatedAt: '',
  },
]

const MOCK_GOALS: Goal[] = [
  {
    id: 1, title: 'Ship Nexus v2.0', description: 'Launch the React UI update',
    category: MOCK_CATS[0], categoryId: 1, categoryIds: [1], categories: [MOCK_CATS[0]],
    targetDate: '2026-06-30',
    status: 'ACTIVE', completed: false, progress: 0.5,
    createdAt: '',
    tasks: [
      { id: 1, title: 'Finish project proposal', status: 'DONE' },
      { id: 3, title: 'Review pull requests', status: 'TODO' },
    ],
  },
  {
    id: 2, title: 'Get kickboxing blue belt', description: '',
    category: MOCK_CATS[1], categoryId: 2, categoryIds: [2], categories: [MOCK_CATS[1]],
    targetDate: '2026-12-01',
    status: 'ACTIVE', completed: false, progress: 0.25,
    createdAt: '',
    tasks: [{ id: 2, title: 'Morning kickboxing session', status: 'TODO' }],
  },
]

const MOCK_BLOCKS: TimeBlock[] = [
  { id: 1, title: 'Deep Work', date: '', startTime: '09:00', endTime: '11:00', color: '#6366f1' },
  { id: 2, title: 'Team Standup', date: '', startTime: '11:00', endTime: '11:30', color: '#10b981' },
  { id: 3, title: 'Kickboxing', date: '', startTime: '18:00', endTime: '19:30', color: '#f59e0b' },
]

const MOCK_STATS: DashboardStats = {
  totalActive: 14, dueToday: 3, completedThisWeek: 8,
  overdueTasks: 2, pomodoroToday: 4, focusTimeThisWeek: 120,
  weeklyCompletions: [2, 3, 1, 4, 2, 0, 0],
  categoryBreakdown: { Work: 7, Health: 4, Personal: 3 },
  streaks: [
    { id: 1, title: 'Kickboxing', currentStreak: 5, longestStreak: 12, active: true, category: MOCK_CATS[1] },
    { id: 2, title: 'Gym', currentStreak: 3, longestStreak: 8, active: true, category: MOCK_CATS[1] },
  ],
  statAdjustments: {},
}

const MOCK_MONTHLY: MonthlyStats[] = Array.from({ length: 12 }, (_, i) => {
  const d = new Date(); d.setMonth(d.getMonth() - (11 - i))
  return {
    yearMonth: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
    monthName: d.toLocaleString('en', { month: 'short' }) + ' ' + d.getFullYear(),
    completed: Math.floor(Math.random() * 20),
  }
})
