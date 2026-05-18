import { useMemo } from 'react'
import { Clock, CheckCircle2, Calendar, Zap } from 'lucide-react'
import type { Task, TimeBlock } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'

interface Props {
  tasks: Task[]
  timeBlocks: TimeBlock[]
  onRefresh: () => void
}

const HOURS = Array.from({ length: 16 }, (_, i) => i + 7) // 7am–10pm

function hourLabel(h: number) {
  if (h === 12) return '12 PM'
  return h < 12 ? `${h} AM` : `${h - 12} PM`
}

export default function Today({ tasks, timeBlocks }: Props) {
  const now = new Date()
  const currentHour = now.getHours() + now.getMinutes() / 60

  const todayTasks = useMemo(() =>
    tasks.filter(t => {
      if (!t.dueDate) return false
      const d = new Date(t.dueDate)
      return d.toDateString() === now.toDateString()
    }), [tasks])

  const doneTasks = todayTasks.filter(t => t.status === 'DONE')
  const pendingTasks = todayTasks.filter(t => t.status !== 'DONE')
  const progress = todayTasks.length ? Math.round((doneTasks.length / todayTasks.length) * 100) : 0

  const dateStr = now.toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric' })

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
                  <div className="flex-1 border-t border-white/[0.05] relative pt-0.5 pb-3">
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
                        style={{
                          background: (b.color ?? '#6366f1') + '22',
                          borderLeft: `3px solid ${b.color ?? '#6366f1'}`,
                          color: b.color ?? '#6366f1',
                        }}>
                        <span className="font-semibold">{b.title}</span>
                        <span className="ml-2 opacity-60">{b.startTime} – {b.endTime}</span>
                      </div>
                    ))}

                    {/* Empty state pulse */}
                    {blocksInHour.length === 0 && isCurrentHour && (
                      <div className="h-8 rounded-lg border border-dashed border-accent/20 flex items-center px-3">
                        <span className="text-[10px] text-accent/50">Now</span>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* Right panel — today's tasks */}
        <div className="w-72 border-l border-white/[0.06] flex flex-col bg-[#0a1020] shrink-0">
          <div className="px-4 py-3 border-b border-white/[0.06]">
            <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider flex items-center gap-2">
              <Calendar size={11} className="text-accent" /> Due Today
            </p>
          </div>

          <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2">
            {todayTasks.length === 0 && (
              <div className="flex flex-col items-center justify-center h-32 gap-2 text-fg-subtle">
                <CheckCircle2 size={28} className="opacity-20" />
                <p className="text-xs">Nothing due today</p>
              </div>
            )}

            {/* Pending */}
            {pendingTasks.map(t => {
              const p = PRIORITY_META[t.priority]
              const s = STATUS_META[t.status]
              return (
                <div key={t.id} className="card px-3 py-3 relative overflow-hidden"
                  style={{ borderLeft: `3px solid ${p.color}` }}>
                  <div className="flex items-start gap-2">
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
    </div>
  )
}
