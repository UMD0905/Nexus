import { useState, useMemo } from 'react'
import { Plus, Search, CheckCircle2, Archive, Trash2, Edit2, Clock, Calendar } from 'lucide-react'
import type { Task, Category, Goal, Priority, TaskStatus } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import TaskDialog from '../components/TaskDialog'
import * as bridge from '../bridge'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  showArchived?: boolean
  categoryFilter?: Category
  title: string
  onRefresh: () => void
}

function TaskCard({ task, onEdit, onMarkDone, onArchive, onDelete }: {
  task: Task
  onEdit: () => void
  onMarkDone: () => void
  onArchive: () => void
  onDelete: () => void
}) {
  const p = PRIORITY_META[task.priority]
  const s = STATUS_META[task.status]
  const isDone = task.status === 'DONE'
  const today = new Date()
  const due = task.dueDate ? new Date(task.dueDate) : null
  const isOverdue = due && due < today && !isDone
  const isToday = due && due.toDateString() === today.toDateString()

  const dueLabel = due
    ? isToday ? 'Today'
    : due.toDateString() === new Date(today.getTime() + 86400000).toDateString() ? 'Tomorrow'
    : due.toLocaleDateString('en', { month: 'short', day: 'numeric' })
    : null

  return (
    <div className={`relative card px-5 py-3.5 animate-fade-in group transition-all ${isDone ? 'opacity-50' : ''}`}>
      {/* Priority bar */}
      <div className="absolute left-0 top-0 bottom-0 w-[3px] rounded-l-xl" style={{ background: p.color }} />

      <div className="flex items-start gap-3 ml-1">
        {/* Done toggle */}
        <button onClick={onMarkDone} className="mt-0.5 shrink-0 text-fg-subtle hover:text-success transition-colors">
          <CheckCircle2 size={16} className={isDone ? 'text-success' : ''} />
        </button>

        {/* Main content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className={`text-sm font-semibold text-fg leading-tight ${isDone ? 'line-through text-fg-muted' : ''}`}>
              {task.title}
            </span>
            {/* Status badge */}
            <span className="badge text-[10px]" style={{ color: s.color, background: s.bg }}>
              {s.label}
            </span>
            {/* Priority */}
            <span className="badge text-[10px]" style={{ color: p.color, background: p.bg }}>
              {p.label}
            </span>
          </div>

          {/* Meta row */}
          <div className="flex items-center gap-3 mt-1.5 flex-wrap">
            {dueLabel && (
              <span className={`flex items-center gap-1 text-xs ${isOverdue ? 'text-danger font-semibold' : 'text-fg-subtle'}`}>
                <Calendar size={11} />
                {dueLabel}
              </span>
            )}
            {task.estimatedMinutes && (
              <span className="flex items-center gap-1 text-xs text-fg-subtle">
                <Clock size={11} />
                {task.estimatedMinutes}m
              </span>
            )}
            {task.category && (
              <span className="flex items-center gap-1 text-xs rounded-full px-2 py-0.5"
                style={{ color: task.category.color, background: task.category.color + '18', border: `1px solid ${task.category.color}30` }}>
                {task.category.name}
              </span>
            )}
            {task.tags?.map(t => (
              <span key={t.id} className="text-xs text-fg-subtle">#{t.name}</span>
            ))}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
          <button onClick={onEdit}    className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-all"><Edit2    size={13} /></button>
          <button onClick={onArchive} className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"><Archive  size={13} /></button>
          <button onClick={onDelete}  className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-danger transition-all"><Trash2   size={13} /></button>
        </div>
      </div>
    </div>
  )
}

export default function TaskList({ tasks, categories, goals, title, onRefresh, showArchived }: Props) {
  const [search, setSearch]           = useState('')
  const [prioFilter, setPrioFilter]   = useState<Priority | ''>('')
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('')
  const [editTask, setEditTask]       = useState<Partial<Task> | null>(null)
  const [showNew, setShowNew]         = useState(false)

  const filtered = useMemo(() => tasks.filter(t => {
    if (search && !t.title.toLowerCase().includes(search.toLowerCase())) return false
    if (prioFilter && t.priority !== prioFilter) return false
    if (statusFilter && t.status !== statusFilter) return false
    return true
  }), [tasks, search, prioFilter, statusFilter])

  const handleSave = (data: Partial<Task>) => {
    if (data.id) bridge.updateTask(data as Partial<Task> & { id: number })
    else bridge.createTask(data)
    onRefresh()
  }

  const handleMarkDone = (t: Task) => { bridge.markDone(t.id); onRefresh() }
  const handleArchive = (t: Task) => { bridge.archiveTask(t.id); onRefresh() }
  const handleDelete = (t: Task) => {
    if (confirm(`Delete "${t.title}"?`)) { bridge.deleteTask(t.id); onRefresh() }
  }
  const handleRestore = (t: Task) => { bridge.restoreTask(t.id); onRefresh() }

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0 flex-wrap">
        <h1 className="text-lg font-bold text-fg mr-2">{title}</h1>

        <div className="relative flex-1 max-w-xs">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-fg-subtle" />
          <input className="input pl-8 py-1.5 text-xs" placeholder="Search…" value={search} onChange={e => setSearch(e.target.value)} />
        </div>

        <select className="input py-1.5 text-xs w-32" value={prioFilter} onChange={e => setPrioFilter(e.target.value as Priority | '')}>
          <option value="">All Priorities</option>
          {(Object.keys(PRIORITY_META) as Priority[]).map(p => <option key={p} value={p}>{PRIORITY_META[p].label}</option>)}
        </select>

        <select className="input py-1.5 text-xs w-32" value={statusFilter} onChange={e => setStatusFilter(e.target.value as TaskStatus | '')}>
          <option value="">All Statuses</option>
          {(Object.keys(STATUS_META) as TaskStatus[]).map(s => <option key={s} value={s}>{STATUS_META[s].label}</option>)}
        </select>

        <div className="flex-1" />
        {!showArchived && (
          <button className="btn-primary flex items-center gap-1.5 py-1.5" onClick={() => setShowNew(true)}>
            <Plus size={14} /> New Task
          </button>
        )}
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
        {filtered.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-fg-subtle">
            <CheckCircle2 size={40} className="opacity-20" />
            <p className="text-sm">No tasks here. {!showArchived && 'Press "New Task" to add one.'}</p>
          </div>
        )}
        {filtered.map(t => (
          <TaskCard
            key={t.id}
            task={t}
            onEdit={() => setEditTask(t)}
            onMarkDone={() => handleMarkDone(t)}
            onArchive={() => showArchived ? handleRestore(t) : handleArchive(t)}
            onDelete={() => handleDelete(t)}
          />
        ))}
      </div>

      {/* Dialogs */}
      {(showNew || editTask) && (
        <TaskDialog
          task={editTask ?? undefined}
          categories={categories}
          goals={goals}
          onSave={handleSave}
          onClose={() => { setShowNew(false); setEditTask(null) }}
        />
      )}
    </div>
  )
}
