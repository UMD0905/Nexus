import { useState } from 'react'
import { X } from 'lucide-react'
import type { Task, Category, Goal, Priority, TaskStatus } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import DatePicker from './DatePicker'
import DurationPicker from './DurationPicker'

interface Props {
  task?: Partial<Task>
  categories: Category[]
  goals: Goal[]
  onSave: (data: Partial<Task>) => void
  onClose: () => void
}

export default function TaskDialog({ task, categories, goals, onSave, onClose }: Props) {
  const [form, setForm] = useState<Partial<Task>>({
    title: '', description: '', priority: 'MEDIUM', status: 'TODO',
    urgent: false, important: false, estimatedMinutes: undefined,
    goalId: undefined,
    ...task,
  })

  const set = (key: keyof Task, val: unknown) => setForm(f => ({ ...f, [key]: val }))

  const handleSave = () => {
    if (!form.title?.trim()) return
    // Encode "goal cleared" as -1 so the bridge knows to unlink
    const payload: Partial<Task> = { ...form }
    if (task?.id && task.goalId && !form.goalId) {
      payload.goalId = -1 as number
    }
    onSave(payload)
    onClose()
  }

  const activeGoals = goals.filter(g => g.status === 'ACTIVE')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-full max-w-lg bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/[0.07]">
          <h2 className="text-base font-semibold text-fg">{task?.id ? 'Edit Task' : 'New Task'}</h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={15} /></button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">
          <div>
            <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Title *</label>
            <input
              className="input"
              placeholder="What needs to be done?"
              value={form.title ?? ''}
              onChange={e => set('title', e.target.value)}
              autoFocus
            />
          </div>

          <div>
            <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Description</label>
            <textarea
              className="input resize-none"
              rows={2}
              placeholder="Optional details..."
              value={form.description ?? ''}
              onChange={e => set('description', e.target.value)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Priority</label>
              <select
                className="input"
                value={form.priority}
                onChange={e => set('priority', e.target.value as Priority)}
              >
                {(Object.keys(PRIORITY_META) as Priority[]).map(p => (
                  <option key={p} value={p}>{PRIORITY_META[p].label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Status</label>
              <select
                className="input"
                value={form.status}
                onChange={e => set('status', e.target.value as TaskStatus)}
              >
                {(Object.keys(STATUS_META) as TaskStatus[]).map(s => (
                  <option key={s} value={s}>{STATUS_META[s].label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Life Area</label>
              <select
                className="input"
                value={form.categoryId ?? ''}
                onChange={e => set('categoryId', e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">None</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Goal</label>
              <select
                className="input"
                value={form.goalId ?? ''}
                onChange={e => set('goalId', e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">No goal</option>
                {activeGoals.map(g => (
                  <option key={g.id} value={g.id}>{g.title}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Due Date</label>
              <DatePicker
                value={form.dueDate ? form.dueDate.split('T')[0] : undefined}
                onChange={d => set('dueDate', d ? d + 'T00:00:00' : undefined)}
              />
            </div>
            <div>
              <label className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider block mb-1.5">Est. Duration</label>
              <DurationPicker
                value={form.estimatedMinutes}
                onChange={m => set('estimatedMinutes', m)}
              />
            </div>
          </div>

          <div className="flex items-center gap-6">
            {[['urgent', 'Urgent'], ['important', 'Important']].map(([key, label]) => (
              <label key={key} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={!!form[key as keyof Task]}
                  onChange={e => set(key as keyof Task, e.target.checked)}
                  className="rounded accent-accent w-3.5 h-3.5"
                />
                <span className="text-sm text-fg-muted">{label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.07]">
          <button className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" onClick={handleSave}>
            {task?.id ? 'Save Changes' : 'Create Task'}
          </button>
        </div>
      </div>
    </div>
  )
}
