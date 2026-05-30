import { useMemo } from 'react'
import { CheckCircle2, AlertTriangle, Flame, Clock } from 'lucide-react'
import type { Task, Goal } from '../types'
import * as bridge from '../bridge'

interface Props {
  tasks: Task[]
  archivedTasks: Task[]
  goals: Goal[]
  onRefresh: () => void
}

const ONE_WEEK_MS   = 7  * 24 * 60 * 60 * 1000
const TWO_WEEKS_MS  = 14 * 24 * 60 * 60 * 1000

export default function Review({ tasks, archivedTasks, goals, onRefresh }: Props) {
  const now = Date.now()

  const completedThisWeek = useMemo(() =>
    archivedTasks.filter(t => {
      if (!t.completedAt) return false
      return now - new Date(t.completedAt).getTime() < ONE_WEEK_MS
    }), [archivedTasks, now])

  const overdueNotTouched = useMemo(() =>
    tasks.filter(t => {
      if (t.status === 'DONE' || !t.dueDate) return false
      const due = new Date(t.dueDate).getTime()
      return due < now && now - new Date(t.updatedAt).getTime() > ONE_WEEK_MS
    }), [tasks, now])

  const goalsNoProgress = useMemo(() =>
    goals.filter(g => {
      if (g.status !== 'ACTIVE') return false
      // Check if the goal has had a task completed in the past 14 days
      const linkedDone = archivedTasks.filter(t =>
        t.goalId === g.id && t.completedAt &&
        now - new Date(t.completedAt).getTime() < TWO_WEEKS_MS
      )
      return linkedDone.length === 0
    }), [goals, archivedTasks, now])

  const overdueTasks = tasks.filter(t => t.dueDate && new Date(t.dueDate).getTime() < now && t.status !== 'DONE')

  const snoozeTask = (taskId: number, days: number) => {
    bridge.snoozeTask(taskId, days * 24 * 60)
    onRefresh()
  }

  const markDone = (taskId: number) => {
    bridge.markDone(taskId)
    onRefresh()
  }

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0">
        <h1 className="text-xl font-bold text-fg">Weekly Review</h1>
        <p className="text-xs text-fg-subtle mt-0.5">
          {new Date().toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}
        </p>
      </div>

      <div className="flex-1 overflow-y-auto px-8 py-5 space-y-6">

        {/* Wins */}
        <section>
          <h2 className="flex items-center gap-2 text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">
            <CheckCircle2 size={12} className="text-emerald-400" /> Completed this week ({completedThisWeek.length})
          </h2>
          {completedThisWeek.length === 0 ? (
            <p className="text-xs text-fg-subtle">Nothing completed yet — it's fine!</p>
          ) : (
            <div className="space-y-1">
              {completedThisWeek.slice(0, 15).map(t => (
                <div key={t.id} className="flex items-center gap-2 px-3 py-2 bg-emerald-500/[0.06] rounded-lg border border-emerald-500/10">
                  <CheckCircle2 size={12} className="text-emerald-400 shrink-0" />
                  <span className="text-xs text-fg">{t.title}</span>
                  {t.completedAt && (
                    <span className="ml-auto text-[10px] text-fg-subtle shrink-0">
                      {new Date(t.completedAt).toLocaleDateString('en', { weekday: 'short', month: 'short', day: 'numeric' })}
                    </span>
                  )}
                </div>
              ))}
              {completedThisWeek.length > 15 && (
                <p className="text-xs text-fg-subtle px-3">…and {completedThisWeek.length - 15} more</p>
              )}
            </div>
          )}
        </section>

        {/* Overdue / not touched */}
        {overdueNotTouched.length > 0 && (
          <section>
            <h2 className="flex items-center gap-2 text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">
              <AlertTriangle size={12} className="text-amber-400" /> Overdue & untouched ({overdueNotTouched.length})
            </h2>
            <div className="space-y-1">
              {overdueNotTouched.map(t => (
                <div key={t.id} className="flex items-center gap-2 px-3 py-2 bg-amber-500/[0.06] rounded-lg border border-amber-500/10">
                  <span className="text-xs text-fg flex-1">{t.title}</span>
                  <button onClick={() => markDone(t.id)}
                    className="text-[10px] text-emerald-400 hover:text-emerald-300 px-2 py-1 rounded hover:bg-emerald-500/10">
                    Done
                  </button>
                  <button onClick={() => snoozeTask(t.id, 7)}
                    className="text-[10px] text-fg-subtle hover:text-fg px-2 py-1 rounded hover:bg-white/[0.06]">
                    +7d
                  </button>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Goals with no progress */}
        {goalsNoProgress.length > 0 && (
          <section>
            <h2 className="flex items-center gap-2 text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">
              <Flame size={12} className="text-red-400" /> Goals with no progress in 2 weeks ({goalsNoProgress.length})
            </h2>
            <div className="space-y-2">
              {goalsNoProgress.map(g => (
                <div key={g.id} className="px-3 py-2 bg-red-500/[0.06] rounded-lg border border-red-500/10">
                  <p className="text-xs font-semibold text-fg">{g.title}</p>
                  <p className="text-[10px] text-fg-subtle mt-0.5">
                    {Math.round(g.progress * 100)}% complete · create or link tasks to move this forward
                  </p>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* All overdue (broader list) */}
        {overdueTasks.length > 0 && (
          <section>
            <h2 className="flex items-center gap-2 text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">
              <Clock size={12} className="text-red-400" /> All overdue ({overdueTasks.length})
            </h2>
            <div className="space-y-1">
              {overdueTasks.map(t => (
                <div key={t.id} className="flex items-center gap-2 px-3 py-2 bg-surface border border-white/[0.07] rounded-lg">
                  <span className="text-xs text-fg flex-1">{t.title}</span>
                  {t.dueDate && (
                    <span className="text-[10px] text-red-400 shrink-0">
                      {new Date(t.dueDate).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
                    </span>
                  )}
                  <button onClick={() => markDone(t.id)}
                    className="text-[10px] text-emerald-400 hover:text-emerald-300 px-2 py-1 rounded hover:bg-emerald-500/10">
                    Done
                  </button>
                </div>
              ))}
            </div>
          </section>
        )}

        {completedThisWeek.length === 0 && overdueNotTouched.length === 0
          && goalsNoProgress.length === 0 && overdueTasks.length === 0 && (
          <div className="flex flex-col items-center py-16">
            <p className="text-4xl mb-4">🎉</p>
            <p className="text-fg font-semibold">Looking great!</p>
            <p className="text-sm text-fg-subtle mt-1">Nothing to review right now. Keep the momentum.</p>
          </div>
        )}
      </div>
    </div>
  )
}
