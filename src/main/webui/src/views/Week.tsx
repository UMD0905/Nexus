import { useMemo } from 'react'
import { CheckCircle2, Circle } from 'lucide-react'
import type { Task } from '../types'
import { PRIORITY_META } from '../types'

interface Props { tasks: Task[] }

const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

function getWeekDays() {
  const now = new Date()
  const dow = now.getDay() // 0=Sun
  const monday = new Date(now)
  monday.setDate(now.getDate() - ((dow + 6) % 7))
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    return d
  })
}

export default function Week({ tasks }: Props) {
  const days = useMemo(() => getWeekDays(), [])
  const today = new Date()

  const tasksByDay = useMemo(() => {
    return days.map(day => ({
      date: day,
      tasks: tasks.filter(t => {
        if (!t.dueDate) return false
        return new Date(t.dueDate).toDateString() === day.toDateString()
      }),
    }))
  }, [tasks, days])

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg">This Week</h1>
        <span className="text-xs text-fg-subtle">
          {days[0].toLocaleDateString('en', { month: 'short', day: 'numeric' })} –{' '}
          {days[6].toLocaleDateString('en', { month: 'short', day: 'numeric', year: 'numeric' })}
        </span>
      </div>

      {/* Day columns */}
      <div className="flex flex-1 overflow-hidden divide-x divide-white/[0.05]">
        {tasksByDay.map(({ date, tasks: dayTasks }, idx) => {
          const isToday = date.toDateString() === today.toDateString()
          const done = dayTasks.filter(t => t.status === 'DONE').length
          const total = dayTasks.length

          return (
            <div key={idx} className={`flex flex-col flex-1 min-w-0 ${isToday ? 'bg-accent/[0.03]' : ''}`}>
              {/* Column header */}
              <div className={`px-3 py-3 border-b border-white/[0.05] ${isToday ? 'border-b-accent/30' : ''}`}>
                <div className="flex items-center justify-between mb-0.5">
                  <span className={`text-xs font-bold uppercase tracking-wider ${isToday ? 'text-accent' : 'text-fg-subtle'}`}>
                    {DAY_NAMES[idx]}
                  </span>
                  {isToday && <span className="text-[9px] bg-accent text-white px-1.5 py-0.5 rounded-full font-bold">TODAY</span>}
                </div>
                <span className={`text-2xl font-bold ${isToday ? 'text-accent' : 'text-fg-muted'}`}>
                  {date.getDate()}
                </span>
                {total > 0 && (
                  <div className="mt-1.5">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-[9px] text-fg-subtle">{done}/{total}</span>
                    </div>
                    <div className="h-0.5 bg-white/[0.06] rounded-full overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all"
                        style={{
                          width: `${total ? (done / total) * 100 : 0}%`,
                          background: isToday ? '#6366f1' : '#10b981',
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Tasks */}
              <div className="flex-1 overflow-y-auto px-2 py-2 space-y-1.5">
                {dayTasks.length === 0 && (
                  <div className="flex flex-col items-center justify-center h-20 gap-1 text-fg-subtle opacity-30">
                    <Circle size={16} />
                  </div>
                )}
                {dayTasks.map(t => {
                  const p = PRIORITY_META[t.priority]
                  const isDone = t.status === 'DONE'
                  return (
                    <div key={t.id}
                      className={`px-2.5 py-2 rounded-lg text-xs font-medium border-l-2 animate-fade-in transition-opacity ${isDone ? 'opacity-40' : ''}`}
                      style={{
                        borderLeftColor: p.color,
                        background: p.color + '12',
                      }}>
                      <div className="flex items-start gap-1.5">
                        <CheckCircle2 size={10} className={`mt-0.5 shrink-0 ${isDone ? 'text-success' : 'text-fg-subtle'}`} />
                        <span className={`leading-tight ${isDone ? 'line-through text-fg-muted' : 'text-fg'}`}>
                          {t.title}
                        </span>
                      </div>
                      {t.category && (
                        <div className="mt-1 ml-4">
                          <span className="text-[9px] opacity-60" style={{ color: t.category.color }}>
                            {t.category.name}
                          </span>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
