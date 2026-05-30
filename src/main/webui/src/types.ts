export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'
export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'
export type Lifecycle = 'INBOX' | 'ANYTIME' | 'TODAY' | 'SOMEDAY'
export type RecurrenceMode = 'FIXED' | 'AFTER_COMPLETION'

export interface Category {
  id: number
  name: string
  color: string
  position: number
}

export interface Tag {
  id: number
  name: string
  color: string
}

export interface Subtask {
  id: number
  taskId: number
  title: string
  done: boolean
  position: number
}

export interface Project {
  id: number
  name: string
  description?: string
  categoryId?: number
  category?: Category
  color: string
  startDate?: string
  dueDate?: string
  status: ProjectStatus
  createdAt: string
  updatedAt: string
  /** Computed on demand by the bridge: count of active tasks. */
  taskCount?: number
  /** Computed on demand by the bridge: 0–100 % done. */
  progress?: number
}

export interface Task {
  id: number
  title: string
  description?: string
  categoryId?: number
  category?: Category
  categoryIds: number[]
  categories: Category[]
  projectId?: number
  priority: Priority
  status: TaskStatus
  lifecycle: Lifecycle
  dueDate?: string       // ISO datetime — the due/end datetime
  deferUntil?: string    // ISO datetime — hidden from main views until after this
  startTime?: string     // "HH:mm" — when the task is planned to begin
  estimatedMinutes?: number
  actualMinutes?: number    // total minutes from completed Pomodoro sessions
  snoozedUntil?: string     // ISO datetime — task won't fire reminders until after this
  urgent: boolean
  important: boolean
  archived: boolean
  goalId?: number
  recurrenceRuleId?: number
  tags: Tag[]
  subtasks?: Subtask[]
  createdAt: string
  updatedAt: string
  completedAt?: string
}

export type RecurrenceType = 'DAILY' | 'WEEKDAYS' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface MonthlyStats {
  yearMonth: string   // "2026-05"
  monthName: string   // "May 2026"
  completed: number
}

export interface LinkedTask {
  id: number
  title: string
  status: TaskStatus
}

export interface Goal {
  id: number
  title: string
  description?: string
  categoryId?: number
  category?: Category
  categoryIds: number[]
  categories: Category[]
  targetDate?: string
  status: GoalStatus
  completed: boolean
  progress: number           // 0.0–1.0 computed from linked tasks
  createdAt: string
  tasks: LinkedTask[]
}

export interface Streak {
  id: number
  title: string
  categoryId?: number
  category?: Category
  currentStreak: number
  longestStreak: number
  lastCompletedDate?: string
  active: boolean
}

export interface TimeBlock {
  id: number
  title: string
  date: string
  startTime: string
  endTime: string
  color: string
  taskId?: number
}

export interface DashboardStats {
  totalActive: number
  dueToday: number
  completedThisWeek: number
  overdueTasks: number
  pomodoroToday: number
  focusTimeThisWeek: number   // total completed Pomodoro minutes this week
  weeklyCompletions: number[]
  categoryBreakdown: Record<string, number>
  streaks: Streak[]
  statAdjustments: Record<string, number>  // manual offsets applied to each counter
}

export interface Notification {
  id: number
  title: string
  body: string
  type: string
  read: boolean
  createdAt: string
  taskId?: number
}

export type NavSection =
  | 'dashboard' | 'all-tasks' | 'today' | 'week'
  | 'goals' | 'matrix' | 'pomodoro' | 'archive' | 'calendar' | 'settings'
  | 'inbox' | 'anytime' | 'someday' | 'scheduled' | 'projects' | 'review' | 'kanban'
  | { type: 'category'; category: Category }
  | { type: 'project'; project: Project }

export const PRIORITY_META: Record<Priority, { label: string; color: string; bg: string }> = {
  LOW:      { label: 'Low',      color: '#10b981', bg: 'rgba(16,185,129,0.12)' },
  MEDIUM:   { label: 'Medium',   color: '#6366f1', bg: 'rgba(99,102,241,0.12)' },
  HIGH:     { label: 'High',     color: '#f59e0b', bg: 'rgba(245,158,11,0.12)' },
  CRITICAL: { label: 'Critical', color: '#ef4444', bg: 'rgba(239,68,68,0.12)'  },
}

export const STATUS_META: Record<TaskStatus, { label: string; color: string; bg: string }> = {
  TODO:        { label: 'To Do',       color: '#6b7280', bg: 'rgba(107,114,128,0.12)' },
  IN_PROGRESS: { label: 'In Progress', color: '#6366f1', bg: 'rgba(99,102,241,0.15)'  },
  DONE:        { label: 'Done',        color: '#10b981', bg: 'rgba(16,185,129,0.12)'  },
  CANCELLED:   { label: 'Cancelled',   color: '#4b5563', bg: 'rgba(75,85,99,0.10)'    },
}
