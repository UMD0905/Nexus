import { useState, useMemo, useRef, useEffect, useCallback } from 'react'
import {
  Plus, Search, CheckCircle2, Archive, Trash2, Edit2, Clock, Calendar,
  Repeat, Copy, Square, CheckSquare, SkipForward, Tag, X,
} from 'lucide-react'
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

function TaskCard({
  task, onEdit, onCopy, onMarkDone, onArchive, onDelete, onSkip,
  selected, onSelect, onShiftSelect, focused, onFocus, selectionMode,
}: {
  task: Task
  onEdit: () => void
  onCopy: () => void
  onMarkDone: () => void
  onArchive: () => void
  onDelete: () => void
  onSkip?: () => void
  selected?: boolean
  onSelect?: (id: number) => void
  onShiftSelect?: (id: number) => void
  focused?: boolean
  onFocus?: () => void
  selectionMode?: boolean   // true when ≥1 task is selected
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

  const handleCheckClick = (e: React.MouseEvent) => {
    if (!onSelect) return
    if (e.shiftKey && onShiftSelect) {
      onShiftSelect(task.id)
    } else {
      onSelect(task.id)
    }
  }

  return (
    <div
      className={`relative card px-5 py-3.5 animate-fade-in group transition-all
        ${isDone ? 'opacity-50' : ''}
        ${selected
          ? 'ring-2 ring-accent/50 bg-accent/[0.06] border-accent/20'
          : focused ? 'ring-2 ring-accent/60 bg-white/[0.03]' : ''}`}
      onMouseEnter={onFocus}
    >
      {/* Priority bar */}
      <div className="absolute left-0 top-0 bottom-0 w-[3px] rounded-l-xl" style={{ background: p.color }} />

      <div className="flex items-start gap-3 ml-1">
        {/* Selection checkbox — always visible when selected or in selection mode */}
        {onSelect && (
          <button
            onClick={handleCheckClick}
            title={selected ? 'Deselect (click) · Shift+click for range' : 'Select (click) · Shift+click for range'}
            className={`mt-0.5 shrink-0 transition-all
              ${selected || selectionMode
                ? 'opacity-100 text-accent'
                : 'opacity-0 group-hover:opacity-100 text-fg-subtle hover:text-accent'}`}
          >
            {selected ? <CheckSquare size={14} /> : <Square size={14} />}
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
            <span className="badge text-[10px]" style={{ color: s.color, background: s.bg }}>{s.label}</span>
            <span className="badge text-[10px]" style={{ color: p.color, background: p.bg }}>{p.label}</span>
          </div>

          <div className="flex items-center gap-3 mt-1.5 flex-wrap">
            {dueLabel && (
              <span className={`flex items-center gap-1 text-xs ${isOverdue ? 'text-danger font-semibold' : 'text-fg-subtle'}`}>
                <Calendar size={11} />
                {dueLabel}
                {task.startTime && (
                  <span className="text-fg-subtle">
                    · {task.startTime}→{due ? due.toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit', hour12: false }) : ''}
                  </span>
                )}
              </span>
            )}
            {task.recurrenceRuleId && (
              <span className="flex items-center gap-1 text-xs text-accent opacity-70">
                <Repeat size={10} /> Recurring
              </span>
            )}
            {task.estimatedMinutes && (
              <span className="flex items-center gap-1 text-xs text-fg-subtle">
                <Clock size={11} />{task.estimatedMinutes}m
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

        {/* Per-card actions — hidden while in selection mode to avoid clutter */}
        {!selectionMode && (
          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
            <button onClick={onEdit}    title="Edit"      className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-all"><Edit2   size={13} /></button>
            <button onClick={onCopy}    title="Duplicate" className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-accent transition-all"><Copy   size={13} /></button>
            {task.recurrenceRuleId && onSkip && (
              <button onClick={onSkip} title="Skip this date" className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"><SkipForward size={13} /></button>
            )}
            <button onClick={onArchive} title="Archive"   className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"><Archive size={13} /></button>
            <button onClick={onDelete}  title="Delete"    className="p-1.5 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-danger transition-all"><Trash2  size={13} /></button>
          </div>
        )}
      </div>
    </div>
  )
}

export default function TaskList({ tasks, categories, goals, title, onRefresh, showArchived }: Props) {
  const [search, setSearch]             = useState('')
  const [prioFilter, setPrioFilter]     = useState<Priority | ''>('')
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('')
  const [editTask, setEditTask]         = useState<Partial<Task> | null>(null)
  const [showNew, setShowNew]           = useState(false)
  const [selectedIds, setSelectedIds]   = useState<Set<number>>(new Set())
  const [focusedIdx, setFocusedIdx]     = useState(-1)
  const rowRefs = useRef<(HTMLDivElement | null)[]>([])
  const lastSelectedIdx = useRef<number>(-1)   // for shift-click range
  const { toasts, showUndoable, dismiss } = useToast()

  const filtered = useMemo(() => tasks.filter(t => {
    if (search) {
      const q = search.toLowerCase()
      if (!t.title.toLowerCase().includes(q) &&
          !t.description?.toLowerCase().includes(q) &&
          !t.tags?.some(tag => tag.name.toLowerCase().includes(q))) return false
    }
    if (prioFilter && t.priority !== prioFilter) return false
    if (statusFilter && t.status !== statusFilter) return false
    return true
  }), [tasks, search, prioFilter, statusFilter])

  // Reset keyboard focus when filters change
  useEffect(() => { setFocusedIdx(-1) }, [search, prioFilter, statusFilter])

  // Scroll the focused row into view
  useEffect(() => {
    rowRefs.current[focusedIdx]?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [focusedIdx])

  // Clear selection when filtered list changes shape (e.g. filter added)
  useEffect(() => {
    setSelectedIds(prev => {
      if (prev.size === 0) return prev
      const visibleIds = new Set(filtered.map(t => t.id))
      const next = new Set([...prev].filter(id => visibleIds.has(id)))
      return next.size === prev.size ? prev : next
    })
  }, [filtered])

  const handleMarkDone = useCallback((t: Task) => {
    bridge.markDone(t.id)
    onRefresh()
  }, [onRefresh])

  // Global Ctrl+D — dispatched from App.tsx
  useEffect(() => {
    const handler = () => {
      if (focusedIdx >= 0 && filtered[focusedIdx]) handleMarkDone(filtered[focusedIdx])
    }
    window.addEventListener('nexus:mark-done', handler as EventListener)
    return () => window.removeEventListener('nexus:mark-done', handler as EventListener)
  }, [focusedIdx, filtered, handleMarkDone])

  // ── Selection helpers ─────────────────────────────────────────────────────

  const toggleSelect = useCallback((id: number) => {
    const idx = filtered.findIndex(t => t.id === id)
    if (idx >= 0) lastSelectedIdx.current = idx
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }, [filtered])

  // Shift-click: select everything between lastSelectedIdx and the clicked row
  const shiftSelect = useCallback((id: number) => {
    const clickedIdx = filtered.findIndex(t => t.id === id)
    if (clickedIdx < 0) return
    const anchor = lastSelectedIdx.current >= 0 ? lastSelectedIdx.current : clickedIdx
    const lo = Math.min(anchor, clickedIdx)
    const hi = Math.max(anchor, clickedIdx)
    setSelectedIds(prev => {
      const next = new Set(prev)
      for (let i = lo; i <= hi; i++) next.add(filtered[i].id)
      return next
    })
    lastSelectedIdx.current = clickedIdx
  }, [filtered])

  const selectAll   = useCallback(() => setSelectedIds(new Set(filtered.map(t => t.id))), [filtered])
  const deselectAll = useCallback(() => { setSelectedIds(new Set()); lastSelectedIdx.current = -1 }, [])

  // ── Bulk actions ──────────────────────────────────────────────────────────

  const handleBulkMarkDone = () => {
    const ids = [...selectedIds]
    deselectAll()
    ids.forEach(id => bridge.markDone(id))
    onRefresh()
  }

  const handleBulkStatus = (status: TaskStatus) => {
    const ids = [...selectedIds]
    deselectAll()
    ids.forEach(id => bridge.updateTask({ id, status }))
    onRefresh()
  }

  const handleBulkPriority = (p: Priority) => {
    const ids = [...selectedIds]
    deselectAll()
    ids.forEach(id => bridge.updateTask({ id, priority: p }))
    onRefresh()
  }

  const handleBulkCategory = (categoryId: number) => {
    const ids = [...selectedIds]
    deselectAll()
    ids.forEach(id => bridge.setTaskCategories(id, [categoryId]))
    onRefresh()
  }

  const handleBulkArchive = () => {
    const ids = [...selectedIds]
    deselectAll()
    showUndoable(
      `Archived ${ids.length} task${ids.length > 1 ? 's' : ''}`,
      () => { ids.forEach(id => bridge.archiveTask(id)); onRefresh() },
    )
  }

  const handleBulkDelete = () => {
    const ids = [...selectedIds]
    deselectAll()
    showUndoable(
      `Deleted ${ids.length} task${ids.length > 1 ? 's' : ''}`,
      () => { ids.forEach(id => bridge.deleteTask(id)); onRefresh() },
      5000,
    )
  }

  // ── Keyboard handler ──────────────────────────────────────────────────────

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    // Ctrl+A — select all visible tasks
    if (e.ctrlKey && (e.key === 'a' || e.key === 'A')) {
      e.preventDefault()
      selectedIds.size === filtered.length ? deselectAll() : selectAll()
      return
    }

    if (filtered.length === 0) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setFocusedIdx(i => Math.min(i < 0 ? 0 : i + 1, filtered.length - 1))
        break
      case 'ArrowUp':
        e.preventDefault()
        setFocusedIdx(i => Math.max(i - 1, 0))
        break
      case 'Enter':
        if (focusedIdx >= 0) setEditTask(filtered[focusedIdx])
        break
      case ' ':
        if (focusedIdx >= 0) {
          e.preventDefault()
          if (e.shiftKey) {
            // Shift+Space — toggle selection of focused row
            toggleSelect(filtered[focusedIdx].id)
          } else {
            handleMarkDone(filtered[focusedIdx])
          }
        }
        break
      case 'Escape':
        if (selectedIds.size > 0) deselectAll()
        else setFocusedIdx(-1)
        break
    }
  }

  // ── Per-card handlers ─────────────────────────────────────────────────────

  const handleSave = (data: Partial<Task> & { recurrence?: { enabled: boolean; type: string; days: string[]; endDate: string } }) => {
    if (data.id) {
      bridge.updateTask(data as Partial<Task> & { id: number })
    } else if (data.recurrence?.enabled) {
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
  const handleArchive = (t: Task) => {
    showUndoable(`Archived "${t.title}"`, () => { bridge.archiveTask(t.id); onRefresh() })
  }
  const handleDelete = (t: Task) => {
    showUndoable(`Deleted "${t.title}"`, () => { bridge.deleteTask(t.id); onRefresh() }, 5000)
  }
  const handleRestore = (t: Task) => { bridge.restoreTask(t.id); onRefresh() }
  const handleSkip    = (t: Task) => { bridge.skipRecurringInstance(t.id); onRefresh() }

  const selectionMode = selectedIds.size > 0

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0 flex-wrap">
        <h1 className="text-lg font-bold text-fg mr-2">{title}</h1>

        <div className="relative flex-1 max-w-xs">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-fg-subtle" />
          <input className="input pl-8 py-1.5 text-xs" placeholder="Search…" value={search}
            onChange={e => setSearch(e.target.value)} />
        </div>

        <select className="input py-1.5 text-xs w-32" value={prioFilter}
          onChange={e => setPrioFilter(e.target.value as Priority | '')}>
          <option value="">All Priorities</option>
          {(Object.keys(PRIORITY_META) as Priority[]).map(p =>
            <option key={p} value={p}>{PRIORITY_META[p].label}</option>)}
        </select>

        <select className="input py-1.5 text-xs w-32" value={statusFilter}
          onChange={e => setStatusFilter(e.target.value as TaskStatus | '')}>
          <option value="">All Statuses</option>
          {(Object.keys(STATUS_META) as TaskStatus[]).map(s =>
            <option key={s} value={s}>{STATUS_META[s].label}</option>)}
        </select>

        <div className="flex-1" />
        {!showArchived && (
          <button className="btn-primary flex items-center gap-1.5 py-1.5" onClick={() => setShowNew(true)}>
            <Plus size={14} /> New Task
          </button>
        )}
      </div>

      {/* Hint bar — swaps to selection hint when items are selected */}
      <div className="px-6 py-1 flex items-center gap-4 shrink-0 border-b border-white/[0.03]">
        {selectionMode ? (
          <span className="text-[10px] text-fg-subtle/50">
            Shift+click to range-select · Shift+Space to toggle · Ctrl+A select all · Esc deselect
          </span>
        ) : (
          <span className="text-[10px] text-fg-subtle/50">
            ↑↓ navigate · Enter edit · Space done · Ctrl+D done · Ctrl+A select all
          </span>
        )}
        {!selectionMode && filtered.length > 1 && (
          <button onClick={selectAll} className="text-[10px] text-fg-subtle hover:text-fg transition-colors ml-auto">
            Select all
          </button>
        )}
        {selectionMode && (
          <button onClick={deselectAll} className="text-[10px] text-fg-subtle hover:text-fg transition-colors ml-auto flex items-center gap-1">
            <X size={10} /> Clear selection
          </button>
        )}
      </div>

      {/* Bulk action bar — only visible when tasks are selected */}
      {selectionMode && (
        <div className="px-4 py-2 bg-accent/[0.08] border-b border-accent/20 flex items-center gap-2 flex-wrap shrink-0 animate-fade-in">
          {/* Count + select controls */}
          <span className="text-xs font-bold text-accent shrink-0">
            {selectedIds.size} / {filtered.length} selected
          </span>
          <button onClick={selectAll}   className="text-[11px] text-fg-subtle hover:text-fg transition-colors">All</button>
          <button onClick={deselectAll} className="text-[11px] text-fg-subtle hover:text-fg transition-colors">None</button>

          <div className="w-px h-4 bg-white/10 mx-1 shrink-0" />

          {/* Quick action: Mark Done */}
          <button
            onClick={handleBulkMarkDone}
            className="btn-ghost text-xs py-1 px-2.5 flex items-center gap-1 text-success hover:bg-success/10"
            title="Mark all selected as Done"
          >
            <CheckCircle2 size={12} /> Done
          </button>

          {/* Status change */}
          <select
            className="input py-1 text-xs w-36"
            defaultValue=""
            onChange={e => { if (e.target.value) { handleBulkStatus(e.target.value as TaskStatus); e.target.value = '' } }}
          >
            <option value="" disabled>Set status…</option>
            {(Object.keys(STATUS_META) as TaskStatus[]).map(s =>
              <option key={s} value={s}>{STATUS_META[s].label}</option>)}
          </select>

          {/* Priority change */}
          <select
            className="input py-1 text-xs w-36"
            defaultValue=""
            onChange={e => { if (e.target.value) { handleBulkPriority(e.target.value as Priority); e.target.value = '' } }}
          >
            <option value="" disabled>Set priority…</option>
            {(Object.keys(PRIORITY_META) as Priority[]).map(p =>
              <option key={p} value={p}>{PRIORITY_META[p].label}</option>)}
          </select>

          {/* Category assignment */}
          {categories.length > 0 && (
            <select
              className="input py-1 text-xs w-36"
              defaultValue=""
              onChange={e => { if (e.target.value) { handleBulkCategory(Number(e.target.value)); e.target.value = '' } }}
            >
              <option value="" disabled><Tag size={10} /> Move to…</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          )}

          <div className="flex-1" />

          {/* Destructive actions — right-aligned */}
          <button
            onClick={handleBulkArchive}
            className="btn-ghost text-xs py-1 px-2.5 flex items-center gap-1 text-warning hover:bg-warning/10"
          >
            <Archive size={12} /> Archive
          </button>
          <button
            onClick={handleBulkDelete}
            className="btn-ghost text-xs py-1 px-2.5 flex items-center gap-1 text-danger hover:bg-danger/10"
          >
            <Trash2 size={12} /> Delete
          </button>
        </div>
      )}

      {/* Task list */}
      <div
        className="flex-1 overflow-y-auto px-4 py-3 space-y-2 outline-none"
        tabIndex={0}
        onKeyDown={handleKeyDown}
        onFocus={e => {
          if (e.target === e.currentTarget && focusedIdx === -1 && filtered.length > 0) setFocusedIdx(0)
        }}
      >
        {filtered.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-fg-subtle">
            <CheckCircle2 size={40} className="opacity-20" />
            <p className="text-sm">No tasks here. {!showArchived && 'Press "New Task" to add one.'}</p>
          </div>
        )}

        {filtered.map((t, idx) => (
          <div key={t.id} ref={el => { rowRefs.current[idx] = el }}>
            <TaskCard
              task={t}
              onEdit={() => setEditTask(t)}
              onCopy={() => handleCopy(t)}
              onMarkDone={() => handleMarkDone(t)}
              onArchive={() => showArchived ? handleRestore(t) : handleArchive(t)}
              onDelete={() => handleDelete(t)}
              onSkip={t.recurrenceRuleId ? () => handleSkip(t) : undefined}
              selected={selectedIds.has(t.id)}
              onSelect={toggleSelect}
              onShiftSelect={shiftSelect}
              focused={idx === focusedIdx}
              onFocus={() => setFocusedIdx(idx)}
              selectionMode={selectionMode}
            />
          </div>
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
