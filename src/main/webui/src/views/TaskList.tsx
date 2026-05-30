import { useState, useMemo } from 'react'
import { Plus, Search, CheckCircle2, Archive, Trash2, Edit2, Clock, Calendar, Repeat, Copy, Square, CheckSquare, SkipForward } from 'lucide-react'
import type { Task, Category, Goal, Priority, TaskStatus } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import TaskDialog from '../components/TaskDialog'
import ToastStack from '../components/ToastStack'
import { useToast } from '../components/useToast'
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

function TaskCard({ task, onEdit, onCopy, onMarkDone, onArchive, onDelete, onSkip, selected, onSelect }: {
  task: Task
  onEdit: () => void
  onCopy: () => void
  onMarkDone: () => void
  onArchive: () => void
  onDelete: () => void
  onSkip?: () => void
  selected?: boolean
  onSelect?: (id: number) => void
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
    <div className={`relative card px-5 py-3.5 animate-fade-in group transition-all ${isDone ? 'opacity-50' : ''} ${selected ? 'ring-1 ring-accent/40' : ''}`}>
      {/* Priority bar */}
      <div className="absolute left-0 top-0 bottom-0 w-[3px] rounded-l-xl" style={{ background: p.color }} />

      <div className="flex items-start gap-3 ml-1">
        {/* Selection checkbox */}
        {onSelect && (
          <button onClick={() => onSelect(task.id)} className="mt-0.5 shrink-0 text-fg-subtle hover:text-accent transition-colors opacity-0 group-hover:opacity-100">
            {selected ? <CheckSquare size={14} className="text-accent" /> : <Square size={14} />}
          </button>
        )}
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
                {task.startTime && <span className="text-fg-subtle">· {task.startTime}→{due ? due.toLocaleTimeString('en',{hour:'2-digit',minute:'2-digit',hour12:false}) : ''}</span>}
              </span>
            )}
            {task.recurrenceRuleId && (
              <span className="flex items-center gap-1 text-xs text-accent opacity-70">
                <Repeat size={10} /> Recurring
              </span>
            )}
            {task.estimatedMinutes && (
              <span className="flex items-center gap-1 text-xs text-fg-subtle">
                <Clock size={11} />
                {task.estimatedMinutes}m
              </span>
            )}
            {(task.categories?.length ? task.categories : task.category ? [task.category] : []).map(c => (
              <span key={c.id} className="flex items-center gap-1 text-xs rounded-full px-2 py-0.5"
                style={{ color: c.color, background: c.color + '18', border: `1px solid ${c.color}30` }}>
                {c.name}
              </span>
            ))}
            {task.tags?.map(t => (
              <span key={t.id} className="text-xs text-fg-subtle">#{t.name}</span>
            ))}
            {task.actualMinutes != null && task.actualMinutes > 0 && (
              <span className="flex items-center gap-0.5 text-[10px] text-fg-subtle" title={`${task.actualMinutes}m of focus time`}>
                🍅 {Math.ceil(task.actualMinutes / 25)}
              </span>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
          <button onClick={onEdit}    title="Edit"      className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-all"><Edit2   size={13} /></button>
          <button onClick={onCopy}    title="Duplicate" className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-accent transition-all"><Copy   size={13} /></button>
          {task.recurrenceRuleId && onSkip && (
            <button onClick={onSkip} title="Skip this date" className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"><SkipForward size={13} /></button>
          )}
          <button onClick={onArchive} title="Archive"   className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"><Archive size={13} /></button>
          <button onClick={onDelete}  title="Delete"    className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-danger transition-all"><Trash2  size={13} /></button>
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
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [bulkPriority, setBulkPriority] = useState<Priority | ''>('')
  const { toasts, showUndoable, dismiss } = useToast()

  const filtered = useMemo(() => tasks.filter(t => {
    if (search) {
      const q = search.toLowerCase()
      const matchTitle = t.title.toLowerCase().includes(q)
      const matchDesc  = t.description?.toLowerCase().includes(q) ?? false
      const matchTag   = t.tags?.some(tag => tag.name.toLowerCase().includes(q)) ?? false
      if (!matchTitle && !matchDesc && !matchTag) return false
    }
    if (prioFilter && t.priority !== prioFilter) return false
    if (statusFilter && t.status !== statusFilter) return false
    return true
  }), [tasks, search, prioFilter, statusFilter])

  const toggleSelect = (id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }

  const selectAll   = () => setSelectedIds(new Set(filtered.map(t => t.id)))
  const deselectAll = () => setSelectedIds(new Set())

  const handleBulkArchive = () => {
    const ids = [...selectedIds]
    deselectAll()
    showUndoable(
      `Archived ${ids.length} task(s)`,
      () => { ids.forEach(id => bridge.archiveTask(id)); onRefresh() },
    )
  }

  const handleBulkDelete = () => {
    const ids = [...selectedIds]
    deselectAll()
    showUndoable(
      `Deleted ${ids.length} task(s)`,
      () => { ids.forEach(id => bridge.deleteTask(id)); onRefresh() },
      5000,
    )
  }

  const handleBulkPriority = (p: Priority) => {
    selectedIds.forEach(id => bridge.updateTask({ id, priority: p }))
    deselectAll()
    onRefresh()
  }

  const handleSave = (data: Partial<Task> & { recurrence?: { enabled: boolean; type: string; days: string[]; endDate: string } }) => {
    if (data.id) {
      bridge.updateTask(data as Partial<Task> & { id: number })
    } else if (data.recurrence?.enabled) {
      // Create recurring task: pass recurrence fields as top-level props that the bridge reads
      bridge.createTask({
        ...data,
        recurrenceType: data.recurrence.type,
        recurrenceDays: data.recurrence.days.join(','),
        recurrenceEndDate: data.recurrence.endDate || undefined,
      } as Parameters<typeof bridge.createTask>[0])
    } else {
      bridge.createTask(data)
    }
    onRefresh()
  }

  const handleCopy = (t: Task) => {
    const { id, completedAt, recurrenceRuleId, ...rest } = t
    setEditTask({ ...rest, title: `Copy of ${rest.title}`, status: 'TODO' })
    setShowNew(true)
  }
  const handleMarkDone = (t: Task) => { bridge.markDone(t.id); onRefresh() }
  const handleArchive = (t: Task) => {
    showUndoable(`Archived "${t.title}"`, () => { bridge.archiveTask(t.id); onRefresh() })
  }
  const handleDelete = (t: Task) => {
    showUndoable(`Deleted "${t.title}"`, () => { bridge.deleteTask(t.id); onRefresh() }, 5000)
  }
  const handleRestore = (t: Task) => { bridge.restoreTask(t.id); onRefresh() }
  const handleSkip = (t: Task) => { bridge.skipRecurringInstance(t.id); onRefresh() }

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

      {/* Bulk action bar */}
      {selectedIds.size > 0 && (
        <div className="px-6 py-2 bg-accent/10 border-b border-accent/20 flex items-center gap-3 flex-wrap shrink-0">
          <span className="text-xs font-semibold text-accent">{selectedIds.size} selected</span>
          <button onClick={selectAll}   className="text-xs text-fg-subtle hover:text-fg transition-colors">Select all</button>
          <button onClick={deselectAll} className="text-xs text-fg-subtle hover:text-fg transition-colors">Deselect all</button>
          <div className="flex-1" />
          <select
            className="input py-1 text-xs w-36"
            value={bulkPriority}
            onChange={e => { if (e.target.value) { handleBulkPriority(e.target.value as Priority); setBulkPriority('') } }}
          >
            <option value="">Change priority…</option>
            {(Object.keys(PRIORITY_META) as Priority[]).map(p => <option key={p} value={p}>{PRIORITY_META[p].label}</option>)}
          </select>
          <button onClick={handleBulkArchive} className="btn-ghost text-xs py-1 px-3 flex items-center gap-1 text-warning">
            <Archive size={12} /> Archive
          </button>
          <button onClick={handleBulkDelete}  className="btn-ghost text-xs py-1 px-3 flex items-center gap-1 text-danger">
            <Trash2 size={12} /> Delete
          </button>
        </div>
      )}

      {selectedIds.size === 0 && filtered.length > 0 && (
        <div className="px-6 py-1 flex items-center gap-3 shrink-0">
          <button onClick={selectAll} className="text-[10px] text-fg-subtle hover:text-fg transition-colors">Select all</button>
        </div>
      )}

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
            onCopy={() => handleCopy(t)}
            onMarkDone={() => handleMarkDone(t)}
            onArchive={() => showArchived ? handleRestore(t) : handleArchive(t)}
            onDelete={() => handleDelete(t)}
            onSkip={t.recurrenceRuleId ? () => handleSkip(t) : undefined}
            selected={selectedIds.has(t.id)}
            onSelect={toggleSelect}
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

      <ToastStack toasts={toasts} onDismiss={dismiss} />
    </div>
  )
}
