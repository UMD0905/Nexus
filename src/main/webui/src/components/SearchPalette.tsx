import { useState, useEffect, useRef, useMemo } from 'react'
import { Search, X, CheckSquare, Target, Archive } from 'lucide-react'
import type { Task, Goal, NavSection } from '../types'
import { PRIORITY_META } from '../types'

interface Props {
  tasks: Task[]
  archivedTasks: Task[]
  goals: Goal[]
  onClose: () => void
  onNavigate: (nav: NavSection) => void
}

type ResultItem =
  | { kind: 'task';  item: Task;  nav: NavSection }
  | { kind: 'goal';  item: Goal;  nav: NavSection }

export default function SearchPalette({ tasks, archivedTasks, goals, onClose, onNavigate }: Props) {
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const [activeIdx, setActiveIdx] = useState(0)

  useEffect(() => {
    inputRef.current?.focus()
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  const results = useMemo<ResultItem[]>(() => {
    const q = query.trim().toLowerCase()
    if (!q) return []

    const taskResults: ResultItem[] = [
      ...tasks.map(t => ({ kind: 'task' as const, item: t, nav: 'all-tasks' as NavSection })),
      ...archivedTasks.map(t => ({ kind: 'task' as const, item: t, nav: 'archive' as NavSection })),
    ].filter(({ item }) =>
      item.title.toLowerCase().includes(q) ||
      item.description?.toLowerCase().includes(q) ||
      item.tags?.some(tag => tag.name.toLowerCase().includes(q))
    ).slice(0, 8)

    const goalResults: ResultItem[] = goals
      .filter(g =>
        g.title.toLowerCase().includes(q) ||
        g.description?.toLowerCase().includes(q)
      )
      .map(g => ({ kind: 'goal' as const, item: g, nav: 'goals' as NavSection }))
      .slice(0, 4)

    return [...taskResults, ...goalResults]
  }, [query, tasks, archivedTasks, goals])

  useEffect(() => { setActiveIdx(0) }, [results])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActiveIdx(i => Math.min(i + 1, results.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIdx(i => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && results[activeIdx]) {
      onNavigate(results[activeIdx].nav)
    }
  }

  return (
    <div
      className="fixed inset-0 z-[200] flex items-start justify-center pt-24 px-4"
      style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-xl bg-surface rounded-2xl border border-white/[0.1] shadow-[0_32px_80px_rgba(0,0,0,0.7)] animate-fade-in overflow-hidden"
        onClick={e => e.stopPropagation()}
      >
        {/* Search input */}
        <div className="flex items-center gap-3 px-4 py-3.5 border-b border-white/[0.07]">
          <Search size={16} className="text-fg-subtle shrink-0" />
          <input
            ref={inputRef}
            className="flex-1 bg-transparent text-sm text-fg placeholder:text-fg-subtle outline-none"
            placeholder="Search tasks, goals, tags…"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          {query && (
            <button onClick={() => setQuery('')} className="text-fg-subtle hover:text-fg transition-colors">
              <X size={14} />
            </button>
          )}
          <kbd className="text-[10px] text-fg-subtle border border-white/[0.1] rounded px-1.5 py-0.5">Esc</kbd>
        </div>

        {/* Results */}
        {query.trim() && (
          <div className="py-2 max-h-80 overflow-y-auto">
            {results.length === 0 ? (
              <p className="text-xs text-fg-subtle text-center py-6">No results for "{query}"</p>
            ) : (
              results.map((r, i) => {
                const isActive = i === activeIdx
                if (r.kind === 'task') {
                  const t = r.item
                  const p = PRIORITY_META[t.priority]
                  return (
                    <button
                      key={`task-${t.id}`}
                      onClick={() => onNavigate(r.nav)}
                      onMouseEnter={() => setActiveIdx(i)}
                      className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors ${
                        isActive ? 'bg-accent/10' : 'hover:bg-white/[0.04]'
                      }`}
                    >
                      {t.archived
                        ? <Archive size={14} className="text-fg-subtle/60 shrink-0" />
                        : <CheckSquare size={14} className="text-fg-subtle shrink-0" />
                      }
                      <span className="flex-1 text-xs text-fg truncate">{t.title}</span>
                      <span className="text-[10px] font-semibold shrink-0" style={{ color: p.color }}>
                        {p.label}
                      </span>
                      {t.archived && (
                        <span className="text-[9px] text-fg-subtle/50 shrink-0">Archive</span>
                      )}
                    </button>
                  )
                } else {
                  const g = r.item
                  return (
                    <button
                      key={`goal-${g.id}`}
                      onClick={() => onNavigate(r.nav)}
                      onMouseEnter={() => setActiveIdx(i)}
                      className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors ${
                        isActive ? 'bg-accent/10' : 'hover:bg-white/[0.04]'
                      }`}
                    >
                      <Target size={14} className="text-accent/70 shrink-0" />
                      <span className="flex-1 text-xs text-fg truncate">{g.title}</span>
                      <span className="text-[9px] text-fg-subtle/60 shrink-0">Goal</span>
                    </button>
                  )
                }
              })
            )}
          </div>
        )}

        {/* Footer hint */}
        <div className="flex items-center gap-4 px-4 py-2 border-t border-white/[0.05]">
          <span className="text-[10px] text-fg-subtle/50 flex items-center gap-1">
            <kbd className="border border-white/[0.1] rounded px-1">↑↓</kbd> navigate
          </span>
          <span className="text-[10px] text-fg-subtle/50 flex items-center gap-1">
            <kbd className="border border-white/[0.1] rounded px-1">↵</kbd> go to section
          </span>
          <span className="text-[10px] text-fg-subtle/40 ml-auto">Ctrl+Shift+F</span>
        </div>
      </div>
    </div>
  )
}
