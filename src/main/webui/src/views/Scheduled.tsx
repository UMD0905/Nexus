import { useState, useEffect } from 'react'
import type { Task, Category, Goal } from '../types'
import * as bridge from '../bridge'

interface Props {
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

/** Scheduled — tasks with a future defer_until date. */
export default function Scheduled({ categories: _categories, goals: _goals, onRefresh: _onRefresh }: Props) {
  const [tasks, setTasks] = useState<Task[]>([])

  useEffect(() => {
    // Request deferred tasks via the filter
    const all = bridge.getTasks({ showDeferred: true })
    setTasks(all)
  }, [])

  const sortedTasks = [...tasks].sort((a, b) => {
    if (!a.deferUntil) return 1
    if (!b.deferUntil) return -1
    return a.deferUntil.localeCompare(b.deferUntil)
  })

  return (
    <div className="h-full flex flex-col">
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0">
        <h1 className="text-xl font-bold text-fg">Scheduled</h1>
        <p className="text-xs text-fg-subtle mt-0.5">
          {tasks.length} deferred · will appear in main views after their defer date
        </p>
      </div>
      {sortedTasks.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="text-fg-subtle text-sm">No scheduled tasks — use "Defer until" in a task to send it here.</p>
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto px-8 py-4 space-y-2">
          {sortedTasks.map(t => (
            <div key={t.id} className="bg-surface border border-white/[0.07] rounded-xl px-4 py-3">
              <div className="flex items-center gap-3">
                <div className="flex-1">
                  <p className="text-sm font-medium text-fg">{t.title}</p>
                  {t.deferUntil && (
                    <p className="text-[10px] text-fg-subtle mt-0.5">
                      Deferred until {new Date(t.deferUntil).toLocaleDateString('en', {
                        weekday: 'short', month: 'short', day: 'numeric', year: 'numeric',
                      })}
                    </p>
                  )}
                </div>
                {t.categories?.map(c => (
                  <span key={c.id} className="text-[10px] px-2 py-0.5 rounded-full font-semibold"
                    style={{ background: c.color + '22', color: c.color }}>
                    {c.name}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
