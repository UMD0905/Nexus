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
      finance: {
        getTransactions(): string
        getTransactionsByMonth(year: number, month: number): string
        addTransaction(json: string): string
        updateTransaction(json: string): string
        deleteTransaction(id: number): void
        getStats(): string
        getOverrides(): string
        setOverride(key: string, amount: string): void
        clearOverride(key: string): void
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
    // Direct sub-bridge window members — injected by MainWindow.tryFinish() via setMember
    // to avoid unreliable Java field traversal in JavaFX WebKit
    nexusBridgeTasks?:     NonNullable<Window['nexusBridge']>['tasks']
    nexusBridgeGoals?:     NonNullable<Window['nexusBridge']>['goals']
    nexusBridgeDashboard?: NonNullable<Window['nexusBridge']>['dashboard']
    nexusBridgePlanning?:  NonNullable<Window['nexusBridge']>['planning']
    nexusBridgeWin?:       NonNullable<Window['nexusBridge']>['win']
    nexusBridgeProjects?:  NonNullable<Window['nexusBridge']>['projects']
    nexusBridgeFinance?:   NonNullable<Window['nexusBridge']>['finance']
  }
}

function call<T>(fn: () => string): T {
  try {
    const result = JSON.parse(fn()) as T
    // Detect Java-side errors — log and treat as empty
    if (result !== null && typeof result === 'object' && !Array.isArray(result) && 'error' in (result as Record<string, unknown>)) {
      console.error('[bridge]', (result as Record<string, unknown>).error)
      return [] as unknown as T
    }
    return result
  } catch (e) {
    console.error('[bridge] call failed:', e)
    return [] as unknown as T
  }
}

const b = () => window.nexusBridge

// Sub-bridge accessors — use dedicated window members injected via setMember
// instead of field traversal (e.g. nexusBridge.tasks) which is unreliable in
// JavaFX WebKit and can silently return undefined after the first page interaction.
const bTasks     = () => window.nexusBridgeTasks
const bGoals     = () => window.nexusBridgeGoals
const bDashboard = () => window.nexusBridgeDashboard
const bPlanning  = () => window.nexusBridgePlanning
const bWin       = () => window.nexusBridgeWin
const bProjects  = () => window.nexusBridgeProjects
const bFinance   = () => window.nexusBridgeFinance

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
  return call(() => bWin()!.getAppInfo())
}

export function exportDiagnostics(): string | null {
  if (!b()) return null
  return call(() => bWin()!.exportDiagnostics())
}

// ── Tasks ──────────────────────────────────────────────────────────────────

export function getTasks(filter: Record<string, unknown> = {}): Task[] {
  if (!b()) return MOCK_TASKS
  try {
    const raw = filter.showDeferred
      ? (() => { const { showDeferred: _, ...rest } = filter; return bTasks()!.getTasks(JSON.stringify({ ...rest, showDeferred: true })) })()
      : bTasks()!.getTasks(JSON.stringify(filter))
    const result = JSON.parse(raw)
    return Array.isArray(result) ? result as Task[] : []
  } catch (e) {
    console.error('[bridge] getTasks failed:', e)
    return []
  }
}

export function getArchivedTasks(): Task[] {
  if (!b()) return []
  return call(() => bTasks()!.getArchivedTasks())
}

export function createTask(data: Partial<Task>): Task | null {
  if (!b()) return null
  try {
    const raw = bTasks()!.createTask(JSON.stringify(data))
    const result = JSON.parse(raw) as unknown
    if (!result || typeof result !== 'object' || Array.isArray(result) || 'error' in (result as Record<string, unknown>)) {
      console.error('[bridge] createTask error:', (result as Record<string, unknown>)?.error)
      return null
    }
    return result as Task
  } catch (e) {
    console.error('[bridge] createTask failed:', e)
    return null
  }
}

export function updateTask(data: Partial<Task> & { id: number }): Task | null {
  if (!b()) return null
  try {
    const raw = bTasks()!.updateTask(JSON.stringify(data))
    const result = JSON.parse(raw) as unknown
    if (!result || typeof result !== 'object' || Array.isArray(result) || 'error' in (result as Record<string, unknown>)) {
      console.error('[bridge] updateTask error:', (result as Record<string, unknown>)?.error)
      return null
    }
    return result as Task
  } catch (e) {
    console.error('[bridge] updateTask failed:', e)
    return null
  }
}

export function deleteTask(id: number) {
  bTasks()?.deleteTask(id)
}

export function archiveTask(id: number) {
  bTasks()?.archiveTask(id)
}

export function restoreTask(id: number) {
  bTasks()?.restoreTask(id)
}

export function markDone(id: number): Task | null {
  if (!b()) return null
  try {
    const raw = bTasks()!.markDone(id)
    const result = JSON.parse(raw) as unknown
    if (!result || typeof result !== 'object' || Array.isArray(result) || 'error' in (result as Record<string, unknown>)) {
      console.error('[bridge] markDone error:', (result as Record<string, unknown>)?.error)
      return null
    }
    return result as Task
  } catch (e) {
    console.error('[bridge] markDone failed:', e)
    return null
  }
}

export function markInProgress(id: number) {
  bTasks()?.markInProgress(id)
}

export function snoozeTask(taskId: number, minutes: number) {
  bTasks()?.snoozeTask(taskId, minutes)
}

// ── Categories ────────────────────────────────────────────────────────────

export function getCategories(): Category[] {
  if (!b()) return MOCK_CATS
  return call(() => bPlanning()!.getCategories())
}

export function createCategory(name: string, color: string): Category | null {
  if (!b()) return null
  return call(() => bPlanning()!.createCategory(JSON.stringify({ name, color })))
}

export function updateCategory(data: { id: number; name?: string; color?: string }): Category | null {
  if (!b()) return null
  return call(() => bPlanning()!.updateCategory(JSON.stringify(data)))
}

export function deleteCategory(id: number) {
  bPlanning()?.deleteCategory(id)
}

// ── Projects ──────────────────────────────────────────────────────────────

export function getProjects(): Project[] {
  if (!b()) return MOCK_PROJECTS
  return call(() => bProjects()!.getProjects())
}

export function getProjectsByCategory(categoryId: number): Project[] {
  if (!b()) return []
  return call(() => bProjects()!.getProjectsByCategory(categoryId))
}

export function createProject(data: Partial<Project>): Project | null {
  if (!b()) return null
  return call(() => bProjects()!.createProject(JSON.stringify(data)))
}

export function updateProject(data: Partial<Project> & { id: number }): Project | null {
  if (!b()) return null
  return call(() => bProjects()!.updateProject(JSON.stringify(data)))
}

export function deleteProject(id: number) {
  bProjects()?.deleteProject(id)
}

export function getProjectTaskCount(projectId: number): number {
  if (!b()) return 0
  return bProjects()?.getProjectTaskCount(projectId) ?? 0
}

export function getProjectProgress(projectId: number): number {
  if (!b()) return 0
  return bProjects()?.getProjectProgress(projectId) ?? 0
}

// ── Goals ─────────────────────────────────────────────────────────────────

export function getGoals(): Goal[] {
  if (!b()) return MOCK_GOALS
  return call(() => bGoals()!.getGoals())
}

export function createGoal(data: Partial<Goal>): Goal | null {
  if (!b()) return null
  return call(() => bGoals()!.createGoal(JSON.stringify(data)))
}

export function updateGoal(data: Partial<Goal> & { id: number }): Goal | null {
  if (!b()) return null
  return call(() => bGoals()!.updateGoal(JSON.stringify(data)))
}

export function updateGoalStatus(id: number, status: string) {
  bGoals()?.updateGoalStatus(id, status)
}

export function deleteGoal(id: number) {
  bGoals()?.deleteGoal(id)
}

// ── Dashboard ─────────────────────────────────────────────────────────────

export function getDashboardStats(): DashboardStats {
  if (!b()) return MOCK_STATS
  try {
    const raw = bDashboard()!.getDashboardStats()
    const result = JSON.parse(raw) as unknown
    if (!result || typeof result !== 'object' || Array.isArray(result) || 'error' in (result as Record<string, unknown>)) {
      console.error('[bridge] getDashboardStats error:', (result as Record<string, unknown>)?.error)
      return MOCK_STATS
    }
    return result as DashboardStats
  } catch (e) {
    console.error('[bridge] getDashboardStats failed:', e)
    return MOCK_STATS
  }
}

export function getMonthlyStats(): MonthlyStats[] {
  if (!b()) return MOCK_MONTHLY
  return call(() => bDashboard()!.getMonthlyStats())
}

export function adjustStat(key: string, delta: number) {
  bDashboard()?.adjustStat(key, delta)
}

export function resetStatAdjustments() {
  bDashboard()?.resetStatAdjustments()
}

// ── Time blocks ───────────────────────────────────────────────────────────

export function getTimeBlocks(date?: string): TimeBlock[] {
  if (!b()) return MOCK_BLOCKS
  const d = date ?? new Date().toISOString().split('T')[0]
  return call(() => bPlanning()!.getTimeBlocks(d))
}

export function getTodayBlocks(date: string): TimeBlock[] {
  if (!b()) return []
  return call(() => bPlanning()!.getTodayBlocks(date))
}

export function createTimeBlock(data: Partial<TimeBlock>): TimeBlock | null {
  if (!b()) return null
  return call(() => bPlanning()!.createTimeBlock(JSON.stringify(data)))
}

export function deleteTimeBlock(id: number) {
  bPlanning()?.deleteTimeBlock(id)
}

// ── Notifications ─────────────────────────────────────────────────────────

export function getNotifications(): Notification[] {
  if (!b()) return []
  return call(() => bWin()!.getNotifications())
}

export function markNotificationRead(id: number) {
  bWin()?.markNotificationRead(id)
}

// ── Pomodoro ──────────────────────────────────────────────────────────────

export function logPomodoro() {
  bPlanning()?.logPomodoro()
}

export function startPomodoroSession(taskId: number, minutes: number): number {
  if (!b()) return 0
  const r = call<{ sessionId: number }>(() => bPlanning()!.startPomodoroSession(taskId, minutes))
  return r?.sessionId ?? 0
}

export function completePomodoroSession(sessionId: number) {
  bPlanning()?.completePomodoroSession(sessionId)
}

export function abandonPomodoroSession(sessionId: number) {
  bPlanning()?.abandonPomodoroSession(sessionId)
}

export function getPomodoroCount(taskId: number): number {
  if (!b()) return 0
  return bPlanning()?.getPomodoroCount(taskId) ?? 0
}

// ── Export / Import ───────────────────────────────────────────────────────

export function exportData(path: string): string {
  if (!b()) return ''
  return bDashboard()?.exportData(path) ?? ''
}

export function importData(filePath: string): { imported: number; skipped: number; errors: string[] } | null {
  if (!b()) return null
  return call(() => bDashboard()!.importData(filePath))
}

export function exportIcal(path: string): string {
  if (!b()) return ''
  return bDashboard()?.exportIcal(path) ?? ''
}

export function backupNow() {
  bDashboard()?.backupNow()
}

export function getStreaks(): import('./types').Streak[] {
  if (!b()) return []
  return call(() => bDashboard()!.getStreaks())
}

export function updateStreak(data: Partial<import('./types').Streak> & { id: number }): import('./types').Streak | null {
  if (!b()) return null
  return call(() => bDashboard()!.updateStreak(JSON.stringify(data)))
}

export function deleteStreak(id: number) {
  bDashboard()?.deleteStreak(id)
}

export function chooseFolder(title: string): string | null {
  if (!b()) return null
  const r = bWin()!.chooseFolder(title)
  try { return JSON.parse(r) } catch { return null }
}

export function chooseFile(title: string, ext: string): string | null {
  if (!b()) return null
  const r = bWin()!.chooseFile(title, ext)
  try { return JSON.parse(r) } catch { return null }
}

// ── Subtasks ──────────────────────────────────────────────────────────────

export function getSubtasks(taskId: number): Subtask[] {
  if (!b()) return []
  return call(() => bTasks()!.getSubtasks(taskId))
}

export function createSubtask(data: { taskId: number; title: string }): Subtask | null {
  if (!b()) return null
  return call(() => bTasks()!.createSubtask(JSON.stringify(data)))
}

export function toggleSubtask(id: number): Subtask | null {
  if (!b()) return null
  return call(() => bTasks()!.toggleSubtask(id))
}

export function deleteSubtask(id: number) {
  bTasks()?.deleteSubtask(id)
}

export function reorderSubtasks(taskId: number, orderedIds: number[]) {
  bTasks()?.reorderSubtasks(taskId, JSON.stringify(orderedIds))
}

// ── Tags ──────────────────────────────────────────────────────────────────

export function getTags(): Tag[] {
  if (!b()) return []
  return call(() => bPlanning()!.getTags())
}

export function createTag(data: { name: string; color: string }): Tag | null {
  if (!b()) return null
  return call(() => bPlanning()!.createTag(JSON.stringify(data)))
}

export function setTaskTags(taskId: number, tagIds: number[]): Tag[] {
  if (!b()) return []
  return call(() => bTasks()!.setTaskTags(taskId, JSON.stringify(tagIds)))
}

export function deleteTag(tagId: number) {
  bPlanning()?.deleteTag(tagId)
}

// ── Recurring ─────────────────────────────────────────────────────────────

export function skipRecurringInstance(taskId: number) {
  bTasks()?.skipRecurringInstance(taskId)
}

export function setTaskCategories(taskId: number, categoryIds: number[]) {
  bTasks()?.setTaskCategories(taskId, JSON.stringify(categoryIds))
}

export function setGoalCategories(goalId: number, categoryIds: number[]) {
  bGoals()?.setGoalCategories(goalId, JSON.stringify(categoryIds))
}

export function updateRecurrenceRule(data: {
  ruleId: number; type?: string; daysOfWeek?: string; endDate?: string
  dayOfMonth?: number; monthOfYear?: number; mode?: string
}): object | null {
  if (!b()) return null
  return call(() => bPlanning()!.updateRecurrenceRule(JSON.stringify(data)))
}

export function reorderCategories(orderedIds: number[]) {
  bWin()?.reorderCategories(JSON.stringify(orderedIds))
}

// ── Settings ──────────────────────────────────────────────────────────────

export function getSettings(): Record<string, string> {
  if (!b()) return {}
  return call(() => bWin()!.getSettings())
}

export function setSetting(key: string, value: string) {
  bWin()?.setSetting(key, value)
}

// ── Finance ───────────────────────────────────────────────────────────────

export interface FinanceStats {
  all: { incomeUzs: number; expenseUzs: number; incomeUsd: number; expenseUsd: number; balanceUzs: number; balanceUsd: number }
  month: { incomeUzs: number; expenseUzs: number; incomeUsd: number; expenseUsd: number; balanceUzs: number; balanceUsd: number }
}

export interface FinanceTx {
  id: number
  type: 'INCOME' | 'EXPENSE'
  amount: number
  currency: 'UZS' | 'USD'
  category: string | null
  description: string | null
  txnDate: string
  createdAt: string
}

export function getTransactions(): FinanceTx[] {
  if (!b()) return []
  return call(() => bFinance()!.getTransactions())
}

export function getTransactionsByMonth(year: number, month: number): FinanceTx[] {
  if (!b()) return []
  return call(() => bFinance()!.getTransactionsByMonth(year, month))
}

export function addTransaction(data: Omit<FinanceTx, 'id' | 'createdAt'>): FinanceTx | null {
  if (!b()) return null
  try {
    const raw = bFinance()!.addTransaction(JSON.stringify(data))
    const result = JSON.parse(raw)
    if (!result || 'error' in result) return null
    return result as FinanceTx
  } catch { return null }
}

export function updateTransaction(data: FinanceTx): FinanceTx | null {
  if (!b()) return null
  try {
    const raw = bFinance()!.updateTransaction(JSON.stringify(data))
    const result = JSON.parse(raw)
    if (!result || 'error' in result) return null
    return result as FinanceTx
  } catch { return null }
}

export function deleteTransaction(id: number) {
  bFinance()?.deleteTransaction(id)
}

export function getFinanceStats(): FinanceStats | null {
  if (!b()) return null
  try {
    const raw = bFinance()!.getStats()
    const result = JSON.parse(raw)
    if (!result || 'error' in result) return null
    return result as FinanceStats
  } catch { return null }
}

export type FinanceOverrideKey =
  | 'all.balance_uzs' | 'all.balance_usd'
  | 'month.income_uzs' | 'month.income_usd'
  | 'month.expense_uzs' | 'month.expense_usd'

export type FinanceOverrides = Partial<Record<FinanceOverrideKey, number>>

export function getFinanceOverrides(): FinanceOverrides {
  if (!b()) return {}
  try {
    const raw = bFinance()!.getOverrides()
    const result = JSON.parse(raw)
    if (!result || 'error' in result) return {}
    return result as FinanceOverrides
  } catch { return {} }
}

export function setFinanceOverride(key: FinanceOverrideKey, amount: number) {
  bFinance()?.setOverride(key, String(amount))
}

export function clearFinanceOverride(key: FinanceOverrideKey) {
  bFinance()?.clearOverride(key)
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
