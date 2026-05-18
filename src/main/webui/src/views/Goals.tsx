import { useState } from 'react'
import { Plus, Target, CheckCircle2, Circle, ChevronDown, ChevronRight, Trash2, Pencil, X, Check } from 'lucide-react'
import type { Goal, Category } from '../types'
import * as bridge from '../bridge'
import DatePicker from '../components/DatePicker'

interface Props {
  goals: Goal[]
  categories: Category[]
  onRefresh: () => void
}

interface EditForm {
  title: string
  description: string
  categoryId: string
  targetDate: string | undefined
}

function GoalCard({ goal, categories, onRefresh }: { goal: Goal; categories: Category[]; onRefresh: () => void }) {
  const [expanded, setExpanded] = useState(false)
  const [editing,  setEditing]  = useState(false)
  const [form, setForm] = useState<EditForm>({
    title:       goal.title,
    description: goal.description ?? '',
    categoryId:  goal.categoryId  ? String(goal.categoryId) : '',
    targetDate:  goal.targetDate  ?? undefined,
  })

  const pct   = Math.round(goal.progress * 100)
  const isDone = goal.completed
  const color  = goal.category?.color ?? '#6366f1'

  const dueStr = goal.targetDate
    ? new Date(goal.targetDate + 'T12:00:00').toLocaleDateString('en', { month: 'short', day: 'numeric', year: 'numeric' })
    : null

  const doneTasks  = goal.tasks.filter(t => t.status === 'DONE').length
  const totalTasks = goal.tasks.length

  const startEdit = (e: React.MouseEvent) => {
    e.stopPropagation()
    setForm({
      title:       goal.title,
      description: goal.description ?? '',
      categoryId:  goal.categoryId ? String(goal.categoryId) : '',
      targetDate:  goal.targetDate ?? undefined,
    })
    setEditing(true)
    setExpanded(true)
  }

  const cancelEdit = (e: React.MouseEvent) => {
    e.stopPropagation()
    setEditing(false)
  }

  const saveEdit = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!form.title.trim()) return
    bridge.updateGoal({
      id:          goal.id,
      title:       form.title.trim(),
      description: form.description || undefined,
      categoryId:  form.categoryId ? Number(form.categoryId) : undefined,
      targetDate:  form.targetDate ?? undefined,
    } as any)
    setEditing(false)
    onRefresh()
  }

  return (
    <div className="card overflow-hidden animate-fade-in" style={{ borderLeft: `3px solid ${color}` }}>
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <div
        className="px-5 py-4 cursor-pointer select-none"
        onClick={() => !editing && setExpanded(e => !e)}
      >
        <div className="flex items-start gap-3">
          <div className="mt-0.5 shrink-0">
            {isDone
              ? <CheckCircle2 size={18} className="text-success" />
              : <Target size={18} style={{ color }} />}
          </div>

          <div className="flex-1 min-w-0">
            {editing ? (
              /* ── Edit mode ─────────────────────────────────────────────── */
              <div className="space-y-2" onClick={e => e.stopPropagation()}>
                <input
                  className="input text-sm w-full"
                  value={form.title}
                  onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                  placeholder="Goal title"
                  autoFocus
                />
                <textarea
                  className="input resize-none text-sm w-full"
                  rows={2}
                  value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="Description (optional)"
                />
                <div className="grid grid-cols-2 gap-2">
                  <select
                    className="input text-sm"
                    value={form.categoryId}
                    onChange={e => setForm(f => ({ ...f, categoryId: e.target.value }))}
                  >
                    <option value="">No category</option>
                    {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  <DatePicker
                    value={form.targetDate}
                    onChange={d => setForm(f => ({ ...f, targetDate: d }))}
                    placeholder="Target date"
                  />
                </div>
                <div className="flex gap-2 justify-end pt-1">
                  <button
                    className="btn-ghost text-xs py-1 px-3 flex items-center gap-1"
                    onClick={cancelEdit}>
                    <X size={12} /> Cancel
                  </button>
                  <button
                    className="btn-primary text-xs py-1 px-3 flex items-center gap-1"
                    onClick={saveEdit}>
                    <Check size={12} /> Save
                  </button>
                </div>
              </div>
            ) : (
              /* ── View mode ─────────────────────────────────────────────── */
              <>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`text-sm font-semibold text-fg ${isDone ? 'line-through text-fg-muted' : ''}`}>
                    {goal.title}
                  </span>
                  {goal.category && (
                    <span className="text-[10px] px-2 py-0.5 rounded-full font-medium"
                      style={{ color, background: color + '18' }}>
                      {goal.category.name}
                    </span>
                  )}
                </div>

                {/* Progress bar */}
                <div className="mt-2 mb-1">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-[10px] text-fg-subtle">
                      {doneTasks}/{totalTasks} tasks complete
                    </span>
                    <span className="text-[10px] font-bold" style={{ color }}>{pct}%</span>
                  </div>
                  <div className="h-1.5 bg-white/[0.06] rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-500"
                      style={{ width: `${pct}%`, background: `linear-gradient(to right, ${color}99, ${color})` }}
                    />
                  </div>
                </div>

                <div className="flex items-center gap-3 mt-1">
                  {dueStr && <span className="text-[10px] text-fg-subtle">Due {dueStr}</span>}
                  <span className="text-[10px] px-1.5 py-0.5 rounded font-medium"
                    style={{
                      color:       goal.status === 'COMPLETED' ? '#10b981' : goal.status === 'ABANDONED' ? '#6b7280' : '#6366f1',
                      background:  goal.status === 'COMPLETED' ? '#10b98120' : goal.status === 'ABANDONED' ? '#6b728020' : '#6366f120',
                    }}>
                    {goal.status}
                  </span>
                </div>
              </>
            )}
          </div>

          {/* Right-side controls */}
          {!editing && (
            <div className="flex items-center gap-1 shrink-0">
              {!isDone && (
                <button
                  onClick={startEdit}
                  className="p-1.5 rounded-lg text-fg-subtle hover:text-accent hover:bg-accent/10 transition-colors"
                  title="Edit goal">
                  <Pencil size={13} />
                </button>
              )}
              <div className="text-fg-subtle p-1">
                {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Linked tasks ───────────────────────────────────────────────────── */}
      {expanded && !editing && goal.tasks.length > 0 && (
        <div className="border-t border-white/[0.05] px-5 py-3 space-y-2">
          <p className="text-[9px] font-bold text-fg-subtle uppercase tracking-wider mb-2">Linked Tasks</p>
          {goal.tasks.map(t => (
            <div key={t.id} className="flex items-center gap-2">
              <div className="shrink-0">
                {t.status === 'DONE'
                  ? <CheckCircle2 size={14} className="text-success" />
                  : <Circle size={14} className="text-fg-subtle" />}
              </div>
              <span className={`text-xs ${t.status === 'DONE' ? 'line-through text-fg-muted' : 'text-fg'}`}>
                {t.title}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* ── Footer actions ─────────────────────────────────────────────────── */}
      {expanded && !editing && (
        <div className="border-t border-white/[0.05] px-5 py-3 flex items-center gap-3">
          {!isDone && (
            <>
              <button
                className="btn-ghost text-xs py-1 px-3 text-success"
                onClick={() => { bridge.updateGoalStatus(goal.id, 'COMPLETED'); onRefresh() }}>
                Mark Complete
              </button>
              <button
                className="btn-ghost text-xs py-1 px-3 text-fg-subtle"
                onClick={() => { bridge.updateGoalStatus(goal.id, 'ABANDONED'); onRefresh() }}>
                Abandon
              </button>
            </>
          )}
          <div className="flex-1" />
          <button
            className="btn-ghost text-xs py-1 px-2 text-danger flex items-center gap-1"
            onClick={() => {
              if (confirm(`Delete goal "${goal.title}"?`)) {
                bridge.deleteGoal(goal.id)
                onRefresh()
              }
            }}>
            <Trash2 size={12} /> Delete
          </button>
        </div>
      )}
    </div>
  )
}

export default function Goals({ goals, categories, onRefresh }: Props) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ title: '', categoryId: '', targetDate: undefined as string | undefined, description: '' })

  const handleCreate = () => {
    if (!form.title.trim()) return
    bridge.createGoal({
      title:       form.title,
      description: form.description || undefined,
      categoryId:  form.categoryId ? Number(form.categoryId) : undefined,
      targetDate:  form.targetDate ?? undefined,
    })
    setForm({ title: '', categoryId: '', targetDate: undefined, description: '' })
    setShowForm(false)
    onRefresh()
  }

  const active   = goals.filter(g => g.status === 'ACTIVE')
  const finished = goals.filter(g => g.status !== 'ACTIVE')

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Goals</h1>
        <button className="btn-primary flex items-center gap-1.5 py-1.5 text-xs" onClick={() => setShowForm(v => !v)}>
          <Plus size={14} /> New Goal
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">
        {/* Create form */}
        {showForm && (
          <div className="card px-5 py-4 animate-fade-in">
            <p className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-3">New Goal</p>
            <div className="space-y-3">
              <input className="input text-sm" placeholder="Goal title" value={form.title}
                onChange={e => setForm(f => ({ ...f, title: e.target.value }))} autoFocus />
              <textarea className="input resize-none text-sm" rows={2} placeholder="Description (optional)"
                value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
              <div className="grid grid-cols-2 gap-3">
                <select className="input text-sm" value={form.categoryId}
                  onChange={e => setForm(f => ({ ...f, categoryId: e.target.value }))}>
                  <option value="">No category</option>
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <DatePicker
                  value={form.targetDate}
                  onChange={d => setForm(f => ({ ...f, targetDate: d }))}
                  placeholder="Target date"
                />
              </div>
              <div className="flex gap-2 justify-end">
                <button className="btn-ghost text-sm" onClick={() => setShowForm(false)}>Cancel</button>
                <button className="btn-primary text-sm" onClick={handleCreate}>Create Goal</button>
              </div>
            </div>
          </div>
        )}

        {/* Active goals */}
        {active.length === 0 && !showForm && (
          <div className="flex flex-col items-center justify-center py-16 gap-3 text-fg-subtle">
            <Target size={40} className="opacity-20" />
            <p className="text-sm">No active goals. Press "New Goal" to add one.</p>
          </div>
        )}
        <div className="space-y-3">
          {active.map(g => <GoalCard key={g.id} goal={g} categories={categories} onRefresh={onRefresh} />)}
        </div>

        {/* Completed/Abandoned goals */}
        {finished.length > 0 && (
          <div>
            <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider mb-3">Finished</p>
            <div className="space-y-3 opacity-60">
              {finished.map(g => <GoalCard key={g.id} goal={g} categories={categories} onRefresh={onRefresh} />)}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
