/**
 * Typed bridge to the JavaFX backend.
 * In production, window.nexusBridge is injected by NexusBridge.java.
 * In dev (browser), mock data is returned.
 */

import type { Task, Category, Goal, TimeBlock, DashboardStats, Notification } from './types'

declare global {
  interface Window {
    nexusBridge?: {
      minimizeWindow(): void
      maximizeWindow(): void
      closeWindow(): void
      getTasks(filterJson: string): string
      getArchivedTasks(): string
      createTask(json: string): string
      updateTask(json: string): string
      deleteTask(id: number): void
      archiveTask(id: number): void
      restoreTask(id: number): void
      markDone(id: number): string
      getCategories(): string
      createCategory(json: string): string
      getGoals(): string
      createGoal(json: string): string
      updateGoal(json: string): string
      updateGoalStatus(id: number, status: string): void
      deleteGoal(id: number): void
      getDashboardStats(): string
      getTimeBlocks(date: string): string
      getTodayBlocks(date: string): string
      createTimeBlock(json: string): string
      deleteTimeBlock(id: number): void
      getNotifications(): string
      markNotificationRead(id: number): void
      logPomodoro(): void
      exportData(path: string): string
    }
    onBridgeEvent?: (eventJson: string) => void
  }
}

function call<T>(fn: () => string): T {
  try { return JSON.parse(fn()) as T }
  catch { return [] as unknown as T }
}

const hasBridge = () => !!window.nexusBridge

// ── Window controls ───────────────────────────────────────────────────────

export function minimizeWindow() { if (hasBridge()) window.nexusBridge!.minimizeWindow() }
export function maximizeWindow() { if (hasBridge()) window.nexusBridge!.maximizeWindow() }
export function closeWindow()    { if (hasBridge()) window.nexusBridge!.closeWindow()    }

// ── Tasks ──────────────────────────────────────────────────────────────────

export function getTasks(filter: Record<string, unknown> = {}): Task[] {
  if (!hasBridge()) return MOCK_TASKS
  return call(() => window.nexusBridge!.getTasks(JSON.stringify(filter)))
}

export function getArchivedTasks(): Task[] {
  if (!hasBridge()) return []
  return call(() => window.nexusBridge!.getArchivedTasks())
}

export function createTask(data: Partial<Task>): Task | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.createTask(JSON.stringify(data)))
}

export function updateTask(data: Partial<Task> & { id: number }): Task | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.updateTask(JSON.stringify(data)))
}

export function deleteTask(id: number) {
  if (hasBridge()) window.nexusBridge!.deleteTask(id)
}

export function archiveTask(id: number) {
  if (hasBridge()) window.nexusBridge!.archiveTask(id)
}

export function restoreTask(id: number) {
  if (hasBridge()) window.nexusBridge!.restoreTask(id)
}

export function markDone(id: number): Task | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.markDone(id))
}

// ── Categories ────────────────────────────────────────────────────────────

export function getCategories(): Category[] {
  if (!hasBridge()) return MOCK_CATS
  return call(() => window.nexusBridge!.getCategories())
}

export function createCategory(name: string, color: string): Category | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.createCategory(JSON.stringify({ name, color })))
}

// ── Goals ─────────────────────────────────────────────────────────────────

export function getGoals(): Goal[] {
  if (!hasBridge()) return MOCK_GOALS
  return call(() => window.nexusBridge!.getGoals())
}

export function createGoal(data: Partial<Goal>): Goal | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.createGoal(JSON.stringify(data)))
}

export function updateGoal(data: Partial<Goal> & { id: number }): Goal | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.updateGoal(JSON.stringify(data)))
}

export function updateGoalStatus(id: number, status: string) {
  if (hasBridge()) window.nexusBridge!.updateGoalStatus(id, status)
}

export function deleteGoal(id: number) {
  if (hasBridge()) window.nexusBridge!.deleteGoal(id)
}

// ── Dashboard ─────────────────────────────────────────────────────────────

export function getDashboardStats(): DashboardStats {
  if (!hasBridge()) return MOCK_STATS
  return call(() => window.nexusBridge!.getDashboardStats())
}

// ── Time blocks ───────────────────────────────────────────────────────────

export function getTimeBlocks(date?: string): TimeBlock[] {
  if (!hasBridge()) return MOCK_BLOCKS
  const d = date ?? new Date().toISOString().split('T')[0]
  return call(() => window.nexusBridge!.getTimeBlocks(d))
}

export function getTodayBlocks(date: string): TimeBlock[] {
  if (!hasBridge()) return []
  return call(() => window.nexusBridge!.getTodayBlocks(date))
}

export function createTimeBlock(data: Partial<TimeBlock>): TimeBlock | null {
  if (!hasBridge()) return null
  return call(() => window.nexusBridge!.createTimeBlock(JSON.stringify(data)))
}

export function deleteTimeBlock(id: number) {
  if (hasBridge()) window.nexusBridge!.deleteTimeBlock(id)
}

// ── Notifications ─────────────────────────────────────────────────────────

export function getNotifications(): Notification[] {
  if (!hasBridge()) return []
  return call(() => window.nexusBridge!.getNotifications())
}

export function markNotificationRead(id: number) {
  if (hasBridge()) window.nexusBridge!.markNotificationRead(id)
}

// ── Pomodoro ──────────────────────────────────────────────────────────────

export function logPomodoro() {
  if (hasBridge()) window.nexusBridge!.logPomodoro()
}

// ── Export ────────────────────────────────────────────────────────────────

export function exportData(path: string): string {
  if (!hasBridge()) return ''
  return window.nexusBridge!.exportData(path)
}

// ── Mock data (dev mode) ──────────────────────────────────────────────────

const MOCK_CATS: Category[] = [
  { id: 1, name: 'Work',      color: '#6366f1', position: 1 },
  { id: 2, name: 'Health',    color: '#10b981', position: 2 },
  { id: 3, name: 'Personal',  color: '#f59e0b', position: 3 },
]

const MOCK_TASKS: Task[] = [
  {
    id: 1, title: 'Finish project proposal', description: 'Q3 planning doc',
    category: MOCK_CATS[0], categoryId: 1, priority: 'HIGH', status: 'IN_PROGRESS',
    dueDate: new Date().toISOString(), urgent: true, important: true,
    archived: false, tags: [{ id: 1, name: 'work', color: '#6366f1' }],
    estimatedMinutes: 90, createdAt: '', updatedAt: '',
  },
  {
    id: 2, title: 'Morning kickboxing session', description: '',
    category: MOCK_CATS[1], categoryId: 2, priority: 'MEDIUM', status: 'TODO',
    dueDate: new Date().toISOString(), urgent: false, important: true,
    archived: false, tags: [], estimatedMinutes: 60, createdAt: '', updatedAt: '',
  },
  {
    id: 3, title: 'Review pull requests', description: '',
    category: MOCK_CATS[0], categoryId: 1, priority: 'CRITICAL', status: 'TODO',
    urgent: true, important: true, archived: false, tags: [], createdAt: '', updatedAt: '',
  },
  {
    id: 4, title: 'Read 30 pages', description: '',
    category: MOCK_CATS[2], categoryId: 3, priority: 'LOW', status: 'TODO',
    urgent: false, important: false, archived: false, tags: [], createdAt: '', updatedAt: '',
  },
]

const MOCK_GOALS: Goal[] = [
  {
    id: 1, title: 'Ship Nexus v2.0', description: 'Launch the React UI update',
    category: MOCK_CATS[0], categoryId: 1, targetDate: '2026-06-30',
    status: 'ACTIVE', completed: false, progress: 0.5,
    createdAt: '',
    tasks: [
      { id: 1, title: 'Finish project proposal', status: 'DONE' },
      { id: 3, title: 'Review pull requests', status: 'TODO' },
    ],
  },
  {
    id: 2, title: 'Get kickboxing blue belt', description: '',
    category: MOCK_CATS[1], categoryId: 2, targetDate: '2026-12-01',
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
  overdueTasks: 2, pomodoroToday: 4,
  weeklyCompletions: [2, 3, 1, 4, 2, 0, 0],
  categoryBreakdown: { Work: 7, Health: 4, Personal: 3 },
  streaks: [
    { id: 1, title: 'Kickboxing', currentStreak: 5, longestStreak: 12, active: true, category: MOCK_CATS[1] },
    { id: 2, title: 'Gym', currentStreak: 3, longestStreak: 8, active: true, category: MOCK_CATS[1] },
  ],
}
