import { useState, useEffect, lazy, Suspense } from 'react'
import { X, Repeat, Eye, EyeOff } from 'lucide-react'

// Lazy-load react-markdown to keep initial bundle small
const ReactMarkdown = lazy(() => import('react-markdown'))
import type { Task, Category, Goal, Priority, TaskStatus, RecurrenceType, Tag, Project, Lifecycle, RecurrenceMode } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import DatePicker from './DatePicker'
import DurationPicker from './DurationPicker'
import SubtaskList from './SubtaskList'
import TagPicker from './TagPicker'
import * as bridge from '../bridge'

interface RecurrenceForm {
  enabled: boolean
  type: RecurrenceType
  mode: RecurrenceMode
  days: string[]       // ['MON','TUE',...] for WEEKLY
  endDate: string      // 'yyyy-MM-dd' or ''
}

const ALL_DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const
const DAY_LABELS: Record<string, string> = {
  MON: 'Mo', TUE: 'Tu', WED: 'We', THU: 'Th', FRI: 'Fr', SAT: 'Sa', SUN: 'Su',
}

interface Props {
  task?: Partial<Task>
  categories: Category[]
  goals: Goal[]
  projects?: Project[]
  onSave: (data: Partial<Task> & { recurrence?: RecurrenceForm }) => void
  onClose: () => void
}

export default function TaskDialog({ task, categories, goals, projects, onSave, onClose }: Props) {
  const [allProjects, setAllProjects] = useState<Project[]>(projects ?? [])
  const [form, setForm] = useState<Partial<Task>>({
    title: '', description: '', priority: 'MEDIUM', status: 'TODO',
    lifecycle: 'ANYTIME',
    urgent: false, important: false, estimatedMinutes: undefined,
    goalId: undefined,
    ...task,
  })
  const [editingDescription, setEditingDescription] = useState(!task?.id)  // new tasks start in edit mode

  useEffect(() => {
    if (!projects) {
      const ps = bridge.getProjects()
      if (ps.length > 0) setAllProjects(ps)
    }
  }, [])

  const existingDate  = task?.dueDate ? task.dueDate.split('T')[0] : undefined
  const existingTime  = task?.dueDate && task.dueDate.includes('T')
    ? task.dueDate.split('T')[1]?.substring(0, 5) : ''
  const [dueDate,  setDueDate]  = useState<string>(existingDate ?? '')
  const [dueTime,  setDueTime]  = useState<string>(existingTime ?? '')
  const [startTime, setStartTime] = useState<string>(task?.startTime ?? '')

  const existingDeferDate = task?.deferUntil ? task.deferUntil.split('T')[0] : ''
  const [deferDate, setDeferDate] = useState<string>(existingDeferDate)

  const [recurrence, setRecurrence] = useState<RecurrenceForm>({
    enabled:  false,
    type:     'WEEKLY',
    mode:     'FIXED',
    days:     [],
    endDate:  '',
  })

  const [showEditRecurrence, setShowEditRecurrence] = useState(false)
  const [editRec, setEditRec] = useState<{ type: RecurrenceType; mode: RecurrenceMode; days: string[]; endDate: string }>({
    type: 'WEEKLY', mode: 'FIXED', days: [], endDate: '',
  })

  const [selectedCatIds, setSelectedCatIds] = useState<number[]>(
    task?.categoryIds?.length ? task.categoryIds
    : task?.categoryId ? [task.categoryId]
    : []
  )
  const toggleCategory = (id: number) =>
    setSelectedCatIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])

  const [allTags, setAllTags] = useState<Tag[]>(task?.tags ?? [])
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>(task?.tags?.map(t => t.id) ?? [])

  useEffect(() => {
    const tags = bridge.getTags()
    if (tags.length > 0) setAllTags(tags)
  }, [])

  const isEdit = !!task?.id
  const set = (key: keyof Task, val: unknown) => setForm(f => ({ ...f, [key]: val }))

  const handleCreateTag = (name: string, color: string) => {
    const saved = bridge.createTag({ name, color })
    if (saved) {
      setAllTags(prev => [...prev, saved])
      setSelectedTagIds(prev => [...prev, saved.id])
    }
  }

  const toggleDay = (day: string) => {
    setRecurrence(r => ({
      ...r,
      days: r.days.includes(day) ? r.days.filter(d => d !== day) : [...r.days, day],
    }))
  }

  const handleSave = () => {
    if (!form.title?.trim()) return

    if (!isEdit && recurrence.enabled && recurrence.type === 'WEEKLY' && recurrence.days.length === 0) {
      alert('Please select at least one day of the week.')
      return
    }

    const finalDate = dueDate || (form.dueDate ? form.dueDate.split('T')[0] : '')
    const finalTime = dueTime || '00:00'
    const finalDueDate = finalDate ? `${finalDate}T${finalTime}:00` : undefined

    const payload: Partial<Task> & {
      recurrence?: RecurrenceForm; dueTime?: string; startTime?: string
      deferUntil?: string; recurrenceMode?: string
    } = {
      ...form,
      dueDate:     finalDueDate,
      startTime:   startTime || undefined,
      dueTime:     dueTime || undefined,
      categoryId:  selectedCatIds[0] ?? undefined,
      categoryIds: selectedCatIds,
      deferUntil:  deferDate ? `${deferDate}T00:00:00` : undefined,
    }

    if (isEdit && task?.goalId && !form.goalId) {
      payload.goalId = -1 as number
    }

    if (!isEdit && recurrence.enabled) {
      payload.recurrence = recurrence
    }

    onSave(payload)

    if (isEdit && task?.id) {
      bridge.setTaskCategories(task.id, selectedCatIds)
      bridge.setTaskTags(task.id, selectedTagIds)
      if (showEditRecurrence && task.recurrenceRuleId) {
        bridge.updateRecurrenceRule({
          ruleId:     task.recurrenceRuleId,
          type:       editRec.type,
          daysOfWeek: editRec.days.join(','),
          endDate:    editRec.endDate,
          mode:       editRec.mode,
        })
      }
    }

    onClose()
  }

  const activeGoals = goals.filter(g => g.status === 'ACTIVE')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-full max-w-lg bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/[0.07] shrink-0">
          <h2 className="text-base font-semibold text-fg">{isEdit ? 'Edit Task' : 'New Task'}</h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={15} /></button>
        </div>

        {/* Body — scrollable */}
        <div className="px-6 py-5 space-y-4 overflow-y-auto flex-1">

          {/* Title */}
          <div>
            <label className="form-label">Title *</label>
            <input className="input" placeholder="What needs to be done?"
              value={form.title ?? ''} onChange={e => set('title', e.target.value)} autoFocus />
          </div>

          {/* Description — toggle between markdown preview and plain-text edit */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="form-label mb-0">Description</label>
              {isEdit && form.description && (
                <button type="button" onClick={() => setEditingDescription(v => !v)}
                  className="flex items-center gap-1 text-[10px] text-fg-subtle hover:text-fg transition-colors">
                  {editingDescription ? <><EyeOff size={10} /> Preview</> : <><Eye size={10} /> Edit</>}
                </button>
              )}
            </div>
            {editingDescription || !form.description ? (
              <textarea className="input resize-none" rows={3} placeholder="Optional — supports **markdown** and `code`"
                value={form.description ?? ''} onChange={e => set('description', e.target.value)} />
            ) : (
              <div
                className="min-h-[4rem] px-3 py-2 rounded-xl bg-white/[0.03] border border-white/[0.07] text-sm text-fg prose prose-invert prose-sm max-w-none cursor-text"
                onClick={() => setEditingDescription(true)}
              >
                <Suspense fallback={<p className="text-fg-subtle text-xs">Loading…</p>}>
                  <ReactMarkdown>{form.description}</ReactMarkdown>
                </Suspense>
              </div>
            )}
          </div>

          {/* Priority + Status */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Priority</label>
              <select className="input" value={form.priority}
                onChange={e => set('priority', e.target.value as Priority)}>
                {(Object.keys(PRIORITY_META) as Priority[]).map(p => (
                  <option key={p} value={p}>{PRIORITY_META[p].label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">Status</label>
              <select className="input" value={form.status}
                onChange={e => set('status', e.target.value as TaskStatus)}>
                {(Object.keys(STATUS_META) as TaskStatus[]).map(s => (
                  <option key={s} value={s}>{STATUS_META[s].label}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Life Areas (multi-select) + Goal */}
          <div className="space-y-3">
            <div>
              <label className="form-label">Life Areas</label>
              <div className="flex flex-wrap gap-1.5">
                {categories.map(c => {
                  const active = selectedCatIds.includes(c.id)
                  return (
                    <button key={c.id} type="button" onClick={() => toggleCategory(c.id)}
                      className="px-2.5 py-1 rounded-full text-xs font-semibold border transition-all"
                      style={{
                        background: active ? c.color + '28' : 'transparent',
                        color: active ? c.color : '#6b7280',
                        borderColor: active ? c.color + '80' : 'rgba(255,255,255,0.08)',
                      }}>
                      {c.name}
                    </button>
                  )
                })}
              </div>
              {selectedCatIds.length === 0 && (
                <p className="text-[9px] text-fg-subtle mt-1">None selected</p>
              )}
            </div>
            <div>
              <label className="form-label">Goal</label>
              <select className="input" value={form.goalId ?? ''}
                onChange={e => set('goalId', e.target.value ? Number(e.target.value) : undefined)}>
                <option value="">No goal</option>
                {activeGoals.map(g => <option key={g.id} value={g.id}>{g.title}</option>)}
              </select>
            </div>
          </div>

          {/* Date + Times */}
          <div>
            <label className="form-label">Schedule</label>
            <div className="grid grid-cols-3 gap-2 items-end">
              <div>
                <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1">Date</span>
                <DatePicker value={dueDate || undefined} onChange={d => setDueDate(d ?? '')} />
              </div>
              <div>
                <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1">Start time</span>
                <input type="time" className="input text-sm" value={startTime}
                  onChange={e => setStartTime(e.target.value)} />
              </div>
              <div>
                <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1">End / due time</span>
                <input type="time" className="input text-sm" value={dueTime}
                  onChange={e => setDueTime(e.target.value)} />
              </div>
            </div>
          </div>

          {/* Est. duration */}
          <div>
            <label className="form-label">Est. Duration</label>
            <DurationPicker value={form.estimatedMinutes} onChange={m => set('estimatedMinutes', m)} />
          </div>

          {/* Defer until + Lifecycle */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="form-label">Defer Until</label>
              <DatePicker value={deferDate || undefined} onChange={d => setDeferDate(d ?? '')} />
            </div>
            <div>
              <label className="form-label">Bucket</label>
              <select className="input" value={form.lifecycle ?? 'ANYTIME'}
                onChange={e => set('lifecycle', e.target.value as Lifecycle)}>
                <option value="INBOX">📥 Inbox</option>
                <option value="ANYTIME">✅ Anytime</option>
                <option value="TODAY">⚡ Today</option>
                <option value="SOMEDAY">🗂 Someday</option>
              </select>
            </div>
          </div>

          {/* Project picker */}
          {allProjects.length > 0 && (
            <div>
              <label className="form-label">Project</label>
              <select className="input" value={form.projectId ?? ''}
                onChange={e => set('projectId', e.target.value ? Number(e.target.value) : undefined)}>
                <option value="">No project</option>
                {allProjects.filter(p => p.status === 'ACTIVE').map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
          )}

          {/* Eisenhower checkboxes */}
          <div className="flex items-center gap-6">
            {[['urgent', 'Urgent'], ['important', 'Important']].map(([key, label]) => (
              <label key={key} className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={!!form[key as keyof Task]}
                  onChange={e => set(key as keyof Task, e.target.checked)}
                  className="rounded accent-accent w-3.5 h-3.5" />
                <span className="text-sm text-fg-muted">{label}</span>
              </label>
            ))}
          </div>

          {/* ── Tags ───────────────────────────────────────────────────────── */}
          <div>
            <label className="form-label">Tags</label>
            <TagPicker
              selectedIds={selectedTagIds}
              allTags={allTags}
              onChange={setSelectedTagIds}
              onCreateTag={handleCreateTag}
            />
          </div>

          {/* ── Subtasks (edit mode only) ────────────────────────────────── */}
          {isEdit && task?.id && (
            <div>
              <label className="form-label">Subtasks</label>
              <SubtaskList taskId={task.id} />
            </div>
          )}

          {/* ── Edit recurrence (edit mode + recurring task) ─────────────── */}
          {isEdit && task?.recurrenceRuleId && (
            <div className="border border-white/[0.07] rounded-xl p-4 space-y-3">
              <label className="flex items-center gap-2.5 cursor-pointer">
                <input type="checkbox" checked={showEditRecurrence}
                  onChange={e => setShowEditRecurrence(e.target.checked)}
                  className="rounded accent-accent w-3.5 h-3.5" />
                <span className="flex items-center gap-1.5 text-sm font-medium text-fg">
                  <Repeat size={13} className="text-accent" /> Edit recurrence pattern
                </span>
              </label>

              {showEditRecurrence && (
                <div className="space-y-3 pt-1">
                  <div className="grid grid-cols-3 gap-2">
                    {(['DAILY', 'WEEKDAYS', 'WEEKLY'] as RecurrenceType[]).map(t => (
                      <button key={t} type="button" onClick={() => setEditRec(r => ({ ...r, type: t }))}
                        className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                          editRec.type === t
                            ? 'bg-accent/20 border-accent text-accent'
                            : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                        }`}>
                        {t === 'DAILY' ? 'Every day' : t === 'WEEKDAYS' ? 'Weekdays' : 'Weekly'}
                      </button>
                    ))}
                  </div>

                  {editRec.type === 'WEEKLY' && (
                    <div className="flex gap-1.5 flex-wrap">
                      {ALL_DAYS.map(d => (
                        <button key={d} type="button"
                          onClick={() => setEditRec(r => ({
                            ...r,
                            days: r.days.includes(d) ? r.days.filter(x => x !== d) : [...r.days, d],
                          }))}
                          className={`w-9 h-9 rounded-lg text-xs font-bold border transition-all ${
                            editRec.days.includes(d)
                              ? 'bg-accent/20 border-accent text-accent'
                              : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                          }`}>
                          {DAY_LABELS[d]}
                        </button>
                      ))}
                    </div>
                  )}

                  <div>
                    <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1">
                      Ends on (leave blank to repeat forever)
                    </span>
                    <DatePicker value={editRec.endDate || undefined}
                      onChange={d => setEditRec(r => ({ ...r, endDate: d ?? '' }))} />
                  </div>

                  <div>
                    <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1.5">
                      Schedule mode
                    </span>
                    <div className="grid grid-cols-2 gap-2">
                      {(['FIXED', 'AFTER_COMPLETION'] as RecurrenceMode[]).map(m => (
                        <button key={m} type="button"
                          onClick={() => setEditRec(r => ({ ...r, mode: m }))}
                          className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                            editRec.mode === m
                              ? 'bg-accent/20 border-accent text-accent'
                              : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                          }`}>
                          {m === 'FIXED' ? 'Fixed' : 'After completion'}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ── Recurrence (new tasks only) ─────────────────────────────────── */}
          {!isEdit && (
            <div className="border border-white/[0.07] rounded-xl p-4 space-y-3">
              <label className="flex items-center gap-2.5 cursor-pointer">
                <input type="checkbox" checked={recurrence.enabled}
                  onChange={e => setRecurrence(r => ({ ...r, enabled: e.target.checked }))}
                  className="rounded accent-accent w-3.5 h-3.5" />
                <span className="flex items-center gap-1.5 text-sm font-medium text-fg">
                  <Repeat size={13} className="text-accent" /> Repeat this task
                </span>
              </label>

              {recurrence.enabled && (
                <div className="space-y-3 pt-1">
                  {/* Recurrence type */}
                  <div className="grid grid-cols-3 gap-2">
                    {(['DAILY', 'WEEKDAYS', 'WEEKLY'] as RecurrenceType[]).map(t => (
                      <button key={t} onClick={() => setRecurrence(r => ({ ...r, type: t }))}
                        className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                          recurrence.type === t
                            ? 'bg-accent/20 border-accent text-accent'
                            : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                        }`}>
                        {t === 'DAILY' ? 'Every day' : t === 'WEEKDAYS' ? 'Weekdays' : 'Weekly'}
                      </button>
                    ))}
                  </div>

                  {/* Day-of-week selector (only for WEEKLY) */}
                  {recurrence.type === 'WEEKLY' && (
                    <div>
                      <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1.5">
                        Days of week
                      </span>
                      <div className="flex gap-1.5 flex-wrap">
                        {ALL_DAYS.map(d => (
                          <button key={d} onClick={() => toggleDay(d)}
                            className={`w-9 h-9 rounded-lg text-xs font-bold border transition-all ${
                              recurrence.days.includes(d)
                                ? 'bg-accent/20 border-accent text-accent'
                                : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                            }`}>
                            {DAY_LABELS[d]}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* End date */}
                  <div>
                    <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1">
                      Ends on (optional — leave blank to repeat forever)
                    </span>
                    <DatePicker value={recurrence.endDate || undefined}
                      onChange={d => setRecurrence(r => ({ ...r, endDate: d ?? '' }))} />
                  </div>

                  {/* Recurrence mode */}
                  <div>
                    <span className="text-[9px] text-fg-subtle uppercase tracking-wide block mb-1.5">
                      Schedule mode
                    </span>
                    <div className="grid grid-cols-2 gap-2">
                      {(['FIXED', 'AFTER_COMPLETION'] as RecurrenceMode[]).map(m => (
                        <button key={m} type="button"
                          onClick={() => setRecurrence(r => ({ ...r, mode: m }))}
                          className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                            recurrence.mode === m
                              ? 'bg-accent/20 border-accent text-accent'
                              : 'border-white/[0.07] text-fg-subtle hover:border-white/20'
                          }`}>
                          {m === 'FIXED' ? 'Fixed schedule' : 'After completion'}
                        </button>
                      ))}
                    </div>
                    {recurrence.mode === 'AFTER_COMPLETION' && (
                      <p className="text-[9px] text-fg-subtle mt-1.5">
                        Next instance generated only when you mark this one done.
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/[0.07] shrink-0">
          <button className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" onClick={handleSave}>
            {isEdit ? 'Save Changes' : 'Create Task'}
          </button>
        </div>
      </div>
    </div>
  )
}
