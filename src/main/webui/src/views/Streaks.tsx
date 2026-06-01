import { useState, useEffect } from 'react'
import { Flame, Snowflake, Edit2, Trash2, X, RotateCcw } from 'lucide-react'
import type { Streak, Category } from '../types'
import * as bridge from '../bridge'

interface Props {
  categories: Category[]
}

function EditDialog({ streak, categories, onSave, onClose }: {
  streak: Streak
  categories: Category[]
  onSave: (data: Partial<Streak> & { id: number }) => void
  onClose: () => void
}) {
  const [title, setTitle]           = useState(streak.title)
  const [categoryId, setCategoryId] = useState<number | ''>(streak.categoryId ?? '')
  const [current, setCurrent]       = useState(streak.currentStreak)
  const [longest, setLongest]       = useState(streak.longestStreak)

  const handleSave = () => {
    if (!title.trim()) return
    onSave({
      id: streak.id,
      title: title.trim(),
      categoryId: categoryId === '' ? undefined : Number(categoryId),
      currentStreak: Math.max(0, current),
      longestStreak: Math.max(longest, current),
    })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-80 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-fg flex items-center gap-2">
            <Flame size={14} className="text-warning" /> Edit Streak
          </h2>
          <button onClick={onClose} className="text-fg-subtle hover:text-fg transition-colors">
            <X size={14} />
          </button>
        </div>

        <div>
          <label className="form-label">Title</label>
          <input
            className="input"
            value={title}
            onChange={e => setTitle(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSave() }}
            autoFocus
          />
        </div>

        {categories.length > 0 && (
          <div>
            <label className="form-label">Life Area</label>
            <select className="input" value={categoryId}
              onChange={e => setCategoryId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">None</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="form-label">Current streak</label>
            <input type="number" min={0} className="input text-right"
              value={current} onChange={e => setCurrent(Math.max(0, Number(e.target.value)))} />
          </div>
          <div>
            <label className="form-label">Best streak</label>
            <input type="number" min={0} className="input text-right"
              value={longest} onChange={e => setLongest(Math.max(0, Number(e.target.value)))} />
          </div>
        </div>

        <p className="text-[10px] text-fg-subtle">
          Setting current to 0 resets the streak's active state and clears the last-completed date.
        </p>

        <div className="flex gap-2 justify-end pt-1">
          <button className="btn-ghost text-sm" onClick={onClose}>Cancel</button>
          <button className="btn-primary text-sm" onClick={handleSave}
            disabled={!title.trim()}>Save changes</button>
        </div>
      </div>
    </div>
  )
}

export default function Streaks({ categories }: Props) {
  const [streaks, setStreaks] = useState<Streak[]>([])
  const [editTarget, setEditTarget] = useState<Streak | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null)

  const load = () => setStreaks(bridge.getStreaks())
  useEffect(() => { load() }, [])

  const handleSave = (data: Partial<Streak> & { id: number }) => {
    bridge.updateStreak(data)
    setEditTarget(null)
    load()
  }

  const handleDelete = (id: number) => {
    bridge.deleteStreak(id)
    setConfirmDeleteId(null)
    load()
  }

  const handleResetCurrent = (s: Streak) => {
    bridge.updateStreak({ id: s.id, title: s.title, currentStreak: 0, longestStreak: s.longestStreak })
    load()
  }

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <Flame size={16} className="text-warning" />
        <h1 className="text-lg font-bold text-fg flex-1">Streaks</h1>
        <span className="text-xs text-fg-subtle">{streaks.length} streak{streaks.length !== 1 ? 's' : ''}</span>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-6 py-6">
        {streaks.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-fg-subtle">
            <Flame size={40} className="opacity-20" />
            <p className="text-sm">No streaks yet.</p>
            <p className="text-xs text-fg-subtle/60 text-center max-w-xs">
              Streaks are created automatically when you mark recurring tasks as done.
            </p>
          </div>
        ) : (
          <div className="max-w-2xl space-y-2">
            {streaks.map(s => {
              const active = s.active
              return (
                <div
                  key={s.id}
                  className="card px-5 py-4 flex items-center gap-4"
                  style={{ borderLeft: `3px solid ${active ? '#f59e0b' : '#374151'}` }}
                >
                  {/* Flame / snowflake icon */}
                  <div className="shrink-0">
                    {active
                      ? <Flame size={20} className="text-warning" style={{ filter: 'drop-shadow(0 0 6px rgba(245,158,11,0.5))' }} />
                      : <Snowflake size={20} className="text-fg-subtle/40" />}
                  </div>

                  {/* Title + category */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-fg truncate">{s.title}</p>
                    {s.category && (
                      <p className="text-[11px] mt-0.5" style={{ color: s.category.color }}>
                        {s.category.name}
                      </p>
                    )}
                    {s.lastCompletedDate && (
                      <p className="text-[10px] text-fg-subtle mt-0.5">Last: {s.lastCompletedDate}</p>
                    )}
                  </div>

                  {/* Streak counters */}
                  <div className="flex items-center gap-6 shrink-0">
                    <div className="text-center">
                      <p className="text-xl font-bold" style={{
                        color: active ? '#f59e0b' : '#4b5563',
                        textShadow: active ? '0 0 12px rgba(245,158,11,0.4)' : undefined,
                      }}>
                        {s.currentStreak}d
                      </p>
                      <p className="text-[9px] text-fg-subtle uppercase tracking-wider">Current</p>
                    </div>
                    <div className="text-center">
                      <p className="text-xl font-bold text-fg-subtle">{s.longestStreak}d</p>
                      <p className="text-[9px] text-fg-subtle uppercase tracking-wider">Best</p>
                    </div>
                  </div>

                  {/* Action buttons — always visible */}
                  <div className="flex items-center gap-1 shrink-0">
                    {s.currentStreak > 0 && (
                      <button
                        onClick={() => handleResetCurrent(s)}
                        title="Reset current streak to 0"
                        className="p-2 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-warning transition-all"
                      >
                        <RotateCcw size={14} />
                      </button>
                    )}
                    <button
                      onClick={() => setEditTarget(s)}
                      title="Edit streak"
                      className="p-2 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-accent transition-all"
                    >
                      <Edit2 size={14} />
                    </button>
                    <button
                      onClick={() => setConfirmDeleteId(s.id)}
                      title="Delete streak"
                      className="p-2 rounded-lg hover:bg-white/[0.06] text-fg-subtle hover:text-danger transition-all"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Edit dialog */}
      {editTarget && (
        <EditDialog
          streak={editTarget}
          categories={categories}
          onSave={handleSave}
          onClose={() => setEditTarget(null)}
        />
      )}

      {/* Delete confirmation dialog */}
      {confirmDeleteId !== null && (() => {
        const s = streaks.find(x => x.id === confirmDeleteId)!
        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
            style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
            <div className="w-72 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6 space-y-4">
              <h2 className="text-sm font-bold text-fg">Delete streak?</h2>
              <p className="text-xs text-fg-subtle">
                "<span className="text-fg font-semibold">{s.title}</span>" will be permanently deleted.
                This does not affect the recurring task itself.
              </p>
              <div className="flex gap-2 justify-end">
                <button className="btn-ghost text-sm" onClick={() => setConfirmDeleteId(null)}>
                  Cancel
                </button>
                <button
                  className="btn-ghost text-sm text-danger hover:bg-danger/10"
                  onClick={() => handleDelete(confirmDeleteId)}
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )
      })()}
    </div>
  )
}
