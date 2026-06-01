import { useState, useMemo, useEffect, useCallback } from 'react'
import { Clock, CheckCircle2, Calendar, Zap, Plus } from 'lucide-react'
import type { Task, TimeBlock, Category, Goal } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import TaskDialog from '../components/TaskDialog'
import * as bridge from '../bridge'

interface Props {
  tasks: Task[]
  timeBlocks: TimeBlock[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

const HOURS = Array.from({ length: 16 }, (_, i) => i + 7) // 7am–10pm

function hourLabel(h: number) {
  if (h === 12) return '12 PM'
  return h < 12 ? `${h} AM` : `${h - 12} PM`
}

function pad2(n: number) { return String(n).padStart(2, '0') }

export default function Today({ tasks, timeBlocks, categories, goals, onRefresh }: Props) {
  const now = new Date()
  const currentHour = now.getHours() + now.getMinutes() / 60

  const [newTaskHour, setNewTaskHour] = useState<number | null>(null)
  const [focusedPendingIdx, setFocusedPendingIdx] = useState(-1)

  const todayTasks = useMemo(() =>
    tasks.filter(t => {
      if (!t.dueDate) return false
      const d = new Date(t.dueDate)
      return d.toDateString() === now.toDateString()
    }), [tasks])

  const doneTasks    = todayTasks.filter(t => t.status === 'DONE')
  const pendingTasks = todayTasks.filter(t => t.status !== 'DONE')
  const progress     = todayTasks.length ? Math.round((doneTasks.length / todayTasks.length) * 100) : 0

  const dateStr = now.toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric' })

  // Reset focus when pending list changes
  useEffect(() => { setFocusedPendingIdx(-1) }, [pendingTasks.length])

  const handleMarkDone = useCallback((t: Task) => {
    bridge.markDone(t.id)
    onRefresh()
  }, [onRefresh])

  // Global Ctrl+D — fires when this view's panel is focused
  useEffect(() => {
    const handler = () => {
      if (focusedPendingIdx >= 0 && pendingTasks[focusedPendingIdx]) {
        handleMarkDone(pendingTasks[focusedPendingIdx])
      }
    }
    window.addEventListener('nexus:mark-done', handler as EventListener)
    return () => window.removeEventListener('nexus:mark-done', handler as EventListener)
  }, [focusedPendingIdx, pendingTasks, handleMarkDone])

  const handlePanelKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (pendingTasks.length === 0) return
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setFocusedPendingIdx(i => Math.min(i < 0 ? 0 : i + 1, pendingTasks.length - 1))
        break
      case 'ArrowUp':
        e.preventDefault()
        setFocusedPendingIdx(i => Math.max(i - 1, 0))
        break
      case 'Enter':
      case ' ':
        if (focusedPendingIdx >= 0) {
          e.preventDefault()
          handleMarkDone(pendingTasks[focusedPendingIdx])
        }
        break
      case 'Escape':
        setFocusedPendingIdx(-1)
        break
    }
  }

  const handleNewTaskSave = (data: Partial<Task> & { recurrence?: unknown }) => {
    bridge.createTask(data as Partial<Task>)
    setNewTaskHour(null)
    onRefresh()
  }

  // Pre-fill TaskDialog when clicking an hour cell
  const prefillForHour = (h: number): Partial<Task> => {
    const dateISO = now.toISOString().split('T')[0]
    return {
      dueDate:   `${dateISO}T${pad2(h)}:00:00`,
      startTime: `${pad2(h)}:00`,
      status:    'TODO',
      priority:  'MEDIUM',
    }
  }

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <div className="flex-1">
          <h1 className="text-lg font-bold text-fg">Today</h1>
          <p className="text-xs text-fg-subtle">{dateStr}</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-fg-subtle">
          <Zap size={13} className="text-accent" />
          <span>{doneTasks.length}/{todayTasks.length} tasks done</span>
          <div className="w-24 h-1.5 bg-white/[0.08] rounded-full overflow-hidden">
            <div className="h-full bg-accent rounded-full transition-all" style={{ width: `${progress}%` }} />
          </div>
          <span className="text-accent font-semibold">{progress}%</span>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Timeline */}
        <div className="flex-1 overflow-y-auto px-6 py-5">
          <div className="relative">
            {HOURS.map(h => {
              const blocksInHour = timeBlocks.filter(b => {
                const start = parseInt(b.startTime?.split(':')[0] ?? '0')
                return start === h
              })
              const isCurrentHour = Math.floor(currentHour) === h
              const pct = isCurrentHour ? ((currentHour - h) * 100).toFixed(1) : null

              return (
                <div key={h} className="flex gap-4 min-h-[64px] group">
                  {/* Hour label */}
                  <div className="w-14 pt-0.5 shrink-0 text-right">
                    <span className={`text-[10px] font-medium ${isCurrentHour ? 'text-accent' : 'text-fg-subtle'}`}>
                      {hourLabel(h)}
                    </span>
                  </div>

                  {/* Grid column */}
                  <div
                    className="flex-1 border-t border-white/[0.05] relative pt-0.5 pb-3 cursor-pointer"
                    onClick={() => setNewTaskHour(h)}
                  >
                    {/* Current time indicator */}
                    {pct !== null && (
                      <div
                        className="absolute left-0 right-0 flex items-center gap-2 z-10 pointer-events-none"
                        style={{ top: `${pct}%` }}
                      >
                        <div className="w-2 h-2 rounded-full bg-accent shrink-0 shadow-[0_0_8px_rgba(99,102,241,0.8)]" />
                        <div className="flex-1 h-px bg-accent opacity-60" />
                      </div>
                    )}

                    {/* Time blocks */}
                    {blocksInHour.map(b => (
                      <div key={b.id}
                        className="mb-1.5 px-3 py-2 rounded-lg text-xs font-medium animate-fade-in"
                        onClick={e => e.stopPropagation()}
                        style={{
                          background: (b.color ?? '#6366f1') + '22',
                          borderLeft: `3px solid ${b.color ?? '#6366f1'}`,
                          color: b.color ?? '#6366f1',
                        }}>
                        <span className="font-semibold">{b.title}</span>
                        <span className="ml-2 opacity-60">{b.startTime} – {b.endTime}</span>
                      </div>
                    ))}

                    {/* Hover hint */}
                    {blocksInHour.length === 0 && (
                      <div className="h-8 rounded-lg border border-dashed border-white/[0.05] group-hover:border-accent/20 flex items-center px-3 transition-colors">
                        {isCurrentHour
                          ? <span className="text-[10px] text-accent/50">Now</span>
                          : <span className="text-[10px] text-fg-subtle/0 group-hover:text-fg-subtle/40 transition-colors flex items-center gap-1">
                              <Plus size={9} /> New task
                            </span>
                        }
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* Right panel — today's tasks
            tabIndex makes it keyboard-focusable; ↑↓ navigates, Enter/Space marks done */}
        <div className="w-72 border-l border-white/[0.06] flex flex-col bg-[#0a1020] shrink-0">
          <div className="px-4 py-3 border-b border-white/[0.06]">
            <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider flex items-center gap-2">
              <Calendar size={11} className="text-accent" /> Due Today
            </p>
            <p className="text-[9px] text-fg-subtle/40 mt-0.5">↑↓ navigate · Enter/Space done</p>
          </div>

          <div
            className="flex-1 overflow-y-auto px-3 py-3 space-y-2 outline-none"
            tabIndex={0}
            onKeyDown={handlePanelKeyDown}
            onFocus={e => {
              if (e.target === e.currentTarget && focusedPendingIdx === -1 && pendingTasks.length > 0) {
                setFocusedPendingIdx(0)
              }
            }}
          >
            {todayTasks.length === 0 && (
              <div className="flex flex-col items-center justify-center h-32 gap-2 text-fg-subtle">
                <CheckCircle2 size={28} className="opacity-20" />
                <p className="text-xs">Nothing due today</p>
              </div>
            )}

            {/* Pending */}
            {pendingTasks.map((t, idx) => {
              const p = PRIORITY_META[t.priority]
              const s = STATUS_META[t.status]
              const isFocused = idx === focusedPendingIdx
              return (
                <div
                  key={t.id}
                  className={`card px-3 py-3 relative overflow-hidden cursor-pointer transition-all ${isFocused ? 'ring-2 ring-accent/60 bg-white/[0.03]' : ''}`}
                  style={{ borderLeft: `3px solid ${p.color}` }}
                  onMouseEnter={() => setFocusedPendingIdx(idx)}
                  onClick={() => handleMarkDone(t)}
                  title="Click or press Enter/Space to mark done"
                >
                  <div className="flex items-start gap-2">
                    <button
                      className="mt-0.5 shrink-0 text-fg-subtle hover:text-success transition-colors"
                      onClick={e => { e.stopPropagation(); handleMarkDone(t) }}
                    >
                      <CheckCircle2 size={14} />
                    </button>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-fg truncate">{t.title}</p>
                      <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                        <span className="badge text-[9px]" style={{ color: s.color, background: s.bg }}>{s.label}</span>
                        {t.estimatedMinutes && (
                          <span className="flex items-center gap-0.5 text-[10px] text-fg-subtle">
                            <Clock size={9} />{t.estimatedMinutes}m
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}

            {/* Done */}
            {doneTasks.length > 0 && (
              <>
                <div className="pt-2 pb-1">
                  <p className="text-[9px] font-bold text-fg-subtle uppercase tracking-wider px-1">Completed</p>
                </div>
                {doneTasks.map(t => (
                  <div key={t.id} className="card px-3 py-3 opacity-40" style={{ borderLeft: '3px solid #10b981' }}>
                    <p className="text-xs text-fg-muted line-through truncate">{t.title}</p>
                  </div>
                ))}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Task dialog — opened by clicking an hour cell */}
      {newTaskHour !== null && (
        <TaskDialog
          task={prefillForHour(newTaskHour)}
          categories={categories}
          goals={goals}
          onSave={handleNewTaskSave}
          onClose={() => setNewTaskHour(null)}
        />
      )}
    </div>
  )
}
