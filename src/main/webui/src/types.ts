export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'

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

export interface Task {
  id: number
  title: string
  description?: string
  categoryId?: number
  category?: Category
  priority: Priority
  status: TaskStatus
  dueDate?: string
  estimatedMinutes?: number
  actualMinutes?: number
  urgent: boolean
  important: boolean
  archived: boolean
  goalId?: number
  recurrenceRuleId?: number
  tags: Tag[]
  createdAt: string
  updatedAt: string
  completedAt?: string
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
  weeklyCompletions: number[]
  categoryBreakdown: Record<string, number>
  streaks: Streak[]
}

export interface Notification {
  id: number
  title: string
  body: string
  type: string
  read: boolean
  createdAt: string
}

export type NavSection =
  | 'dashboard' | 'all-tasks' | 'today' | 'week'
  | 'goals' | 'matrix' | 'pomodoro' | 'archive' | 'calendar'
  | { type: 'category'; category: Category }

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
