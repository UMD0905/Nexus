import { useState, useMemo } from 'react'
import { ChevronLeft, ChevronRight, Plus, CheckCircle2, Clock } from 'lucide-react'
import type { Task, Category, Goal } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import * as bridge from '../bridge'
import TaskDialog from '../components/TaskDialog'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

const MONTHS = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December',
]
const DOW_FULL  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']

function pad(n: number) { return String(n).padStart(2, '0') }
function toDateStr(y: number, m: number, d: number) {
  return `${y}-${pad(m + 1)}-${pad(d)}`
}

function taskDateStr(task: Task): string | null {
  if (!task.dueDate) return null
  return task.dueDate.split('T')[0]
}

export default function Calendar({ tasks, categories, goals, onRefresh }: Props) {
  const today = new Date()
  const [viewYear,  setViewYear]  = useState(today.getFullYear())
  const [viewMonth, setViewMonth] = useState(today.getMonth())
  const [selected,  setSelected]  = useState<string | null>(
    toDateStr(today.getFullYear(), today.getMonth(), today.getDate())
  )
  const [showDialog, setShowDialog] = useState(false)
  const [editTask,   setEditTask]   = useState<Partial<Task> | undefined>()

  const todayStr = toDateStr(today.getFullYear(), today.getMonth(), today.getDate())

  // ── Calendar grid ──────────────────────────────────────────────────────────
  const { cells, prevY, prevM, nextY, nextM } = useMemo(() => {
    type Cell = { year: number; month: number; day: number; current: boolean }
    const cells: Cell[] = []
    const firstDow    = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7
    const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
    const prevDays    = new Date(viewYear, viewMonth, 0).getDate()

    const prevY = viewMonth === 0  ? viewYear - 1 : viewYear
    const prevM = viewMonth === 0  ? 11            : viewMonth - 1
    const nextY = viewMonth === 11 ? viewYear + 1  : viewYear
    const nextM = viewMonth === 11 ? 0              : viewMonth + 1

    for (let i = firstDow - 1; i >= 0; i--)
      cells.push({ year: prevY, month: prevM, day: prevDays - i, current: false })
    for (let d = 1; d <= daysInMonth; d++)
      cells.push({ year: viewYear, month: viewMonth, day: d, current: true })
    const fill = (7 - (cells.length % 7)) % 7
    for (let d = 1; d <= fill; d++)
      cells.push({ year: nextY, month: nextM, day: d, current: false })

    return { cells, prevY, prevM, nextY, nextM }
  }, [viewYear, viewMonth])

  // ── Task index by date ─────────────────────────────────────────────────────
  const tasksByDate = useMemo(() => {
    const map: Record<string, Task[]> = {}
    for (const t of tasks) {
      const d = taskDateStr(t)
      if (d) { (map[d] ??= []).push(t) }
    }
    return map
  }, [tasks])

  // ── Selected day tasks ────────────────────────────────────────────────────
  const selectedTasks = selected ? (tasksByDate[selected] ?? []) : []

  const prevMonth = () => { setViewYear(prevY); setViewMonth(prevM) }
  const nextMonth = () => { setViewYear(nextY); setViewMonth(nextM) }

  const openNew = (dateStr?: string) => {
    setEditTask({
      dueDate: dateStr ? dateStr + 'T00:00:00' : undefined,
    })
    setShowDialog(true)
  }

  const openEdit = (task: Task) => {
    setEditTask(task)
    setShowDialog(true)
  }

  const handleSave = (data: Partial<Task>) => {
    if (data.id) {
      bridge.updateTask(data as Partial<Task> & { id: number })
    } else {
      bridge.createTask(data)
    }
    onRefresh()
  }

  const handleDone = (task: Task) => {
    bridge.markDone(task.id)
    onRefresh()
  }

  return (
    <div className="flex h-full bg-canvas overflow-hidden">
      {/* ── Left: month grid ─────────────────────────────────────────────── */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* Toolbar */}
        <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
          <button onClick={prevMonth}
            className="p-1.5 rounded-lg hover:bg-black/[0.06] dark:hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
            <ChevronLeft size={16} />
          </button>
          <h1 className="text-lg font-bold text-fg flex-1 text-center">
            {MONTHS[viewMonth]} {viewYear}
          </h1>
          <button onClick={nextMonth}
            className="p-1.5 rounded-lg hover:bg-black/[0.06] dark:hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
            <ChevronRight size={16} />
          </button>
          <button
            className="btn-primary flex items-center gap-1.5 py-1.5 text-xs"
            onClick={() => openNew(selected ?? undefined)}>
            <Plus size={14} /> New Task
          </button>
        </div>

        {/* Day-of-week headers */}
        <div className="grid grid-cols-7 border-b border-white/[0.06] bg-[#0e1524] shrink-0">
          {DOW_FULL.map(d => (
            <div key={d}
              className="text-center text-[10px] font-bold text-fg-subtle uppercase py-2 tracking-wider">
              {d}
            </div>
          ))}
        </div>

        {/* Grid */}
        <div className="flex-1 overflow-y-auto">
          <div className="grid grid-cols-7 h-full"
            style={{ gridAutoRows: 'minmax(90px, 1fr)' }}>
            {cells.map((cell, i) => {
              const ds      = toDateStr(cell.year, cell.month, cell.day)
              const isToday = ds === todayStr
              const isSel   = ds === selected
              const dayTasks = tasksByDate[ds] ?? []

              return (
                <div
                  key={i}
                  onClick={() => setSelected(ds)}
                  className={[
                    'border-b border-r border-white/[0.04] p-1.5 cursor-pointer transition-colors',
                    isSel   ? 'bg-accent/[0.08]' : 'hover:bg-white/[0.02]',
                    !cell.current ? 'opacity-40' : '',
                  ].join(' ')}
                >
                  {/* Day number */}
                  <div className="flex items-center justify-between mb-1">
                    <span className={[
                      'text-xs font-bold w-6 h-6 flex items-center justify-center rounded-full',
                      isToday ? 'bg-accent text-white' : isSel ? 'text-accent' : 'text-fg-subtle',
                    ].join(' ')}>
                      {cell.day}
                    </span>
                    {dayTasks.length > 0 && (
                      <span className="text-[9px] text-fg-subtle font-medium">
                        {dayTasks.length}
                      </span>
                    )}
                  </div>

                  {/* Task pills — show up to 3 */}
                  <div className="space-y-0.5">
                    {dayTasks.slice(0, 3).map(t => {
                      const meta  = PRIORITY_META[t.priority]
                      const done  = t.status === 'DONE'
                      return (
                        <div
                          key={t.id}
                          onClick={e => { e.stopPropagation(); setSelected(ds); openEdit(t) }}
                          className={[
                            'text-[9px] px-1.5 py-0.5 rounded truncate font-medium leading-tight cursor-pointer',
                            done ? 'line-through opacity-50' : '',
                          ].join(' ')}
                          style={{
                            background: meta.color + '22',
                            color: done ? '#6b7280' : meta.color,
                            borderLeft: `2px solid ${done ? '#6b7280' : meta.color}`,
                          }}
                          title={t.title}
                        >
                          {t.title}
                        </div>
                      )
                    })}
                    {dayTasks.length > 3 && (
                      <div className="text-[9px] text-fg-subtle px-1 font-medium">
                        +{dayTasks.length - 3} more
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* ── Right: day panel ──────────────────────────────────────────────── */}
      <div className="w-72 shrink-0 border-l border-white/[0.06] flex flex-col bg-[#0a1020] overflow-hidden">
        {selected ? (
          <>
            {/* Day header */}
            <div className="px-4 py-4 border-b border-white/[0.06] shrink-0">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-bold text-fg">
                    {new Date(selected + 'T12:00:00').toLocaleDateString('en', {
                      weekday: 'long', month: 'long', day: 'numeric',
                    })}
                  </p>
                  {selected === todayStr && (
                    <span className="text-[10px] text-accent font-semibold">Today</span>
                  )}
                </div>
                <button
                  className="p-1.5 rounded-lg text-fg-subtle hover:text-accent hover:bg-accent/10 transition-colors"
                  title="Add task on this day"
                  onClick={() => openNew(selected)}>
                  <Plus size={14} />
                </button>
              </div>
            </div>

            {/* Tasks */}
            <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2">
              {selectedTasks.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 gap-2 text-fg-subtle">
                  <CheckCircle2 size={28} className="opacity-20" />
                  <p className="text-xs text-center">No tasks due this day</p>
                  <button
                    className="text-xs text-accent hover:underline mt-1"
                    onClick={() => openNew(selected)}>
                    + Add task
                  </button>
                </div>
              ) : (
                selectedTasks.map(t => {
                  const meta  = PRIORITY_META[t.priority]
                  const smeta = STATUS_META[t.status]
                  const done  = t.status === 'DONE'
                  return (
                    <div
                      key={t.id}
                      className="card px-3 py-2.5 cursor-pointer hover:border-white/[0.12] transition-colors"
                      style={{ borderLeft: `3px solid ${done ? '#374151' : meta.color}` }}
                      onClick={() => openEdit(t)}
                    >
                      <p className={`text-xs font-semibold ${done ? 'line-through text-fg-muted' : 'text-fg'}`}>
                        {t.title}
                      </p>
                      <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                        <span className="text-[9px] px-1.5 py-0.5 rounded font-medium"
                          style={{ color: meta.color, background: meta.color + '22' }}>
                          {meta.label}
                        </span>
                        <span className="text-[9px] px-1.5 py-0.5 rounded font-medium"
                          style={{ color: smeta.color, background: smeta.bg }}>
                          {smeta.label}
                        </span>
                        {t.estimatedMinutes && (
                          <span className="text-[9px] text-fg-subtle flex items-center gap-0.5">
                            <Clock size={9} /> {t.estimatedMinutes}m
                          </span>
                        )}
                      </div>
                      {!done && (
                        <button
                          className="mt-2 text-[9px] text-success hover:underline"
                          onClick={e => { e.stopPropagation(); handleDone(t) }}>
                          Mark done
                        </button>
                      )}
                    </div>
                  )
                })
              )}
            </div>
          </>
        ) : (
          <div className="flex items-center justify-center flex-1 text-fg-subtle text-xs">
            Select a day
          </div>
        )}
      </div>

      {showDialog && (
        <TaskDialog
          task={editTask}
          categories={categories}
          goals={goals}
          onSave={handleSave}
          onClose={() => { setShowDialog(false); setEditTask(undefined) }}
        />
      )}
    </div>
  )
}
