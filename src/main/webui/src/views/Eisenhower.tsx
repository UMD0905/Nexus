import type { Task } from '../types'
import { PRIORITY_META, STATUS_META } from '../types'
import { Clock, Zap, Inbox, Trash2 } from 'lucide-react'

interface Props { tasks: Task[] }

type Quadrant = 'Q1' | 'Q2' | 'Q3' | 'Q4'

const QUADRANTS: { id: Quadrant; label: string; sub: string; Icon: React.ElementType; color: string; bg: string }[] = [
  { id: 'Q1', label: 'Do First',    sub: 'Urgent & Important',     Icon: Zap,    color: '#ef4444', bg: '#ef444408' },
  { id: 'Q2', label: 'Schedule',    sub: 'Not Urgent & Important', Icon: Clock,  color: '#6366f1', bg: '#6366f108' },
  { id: 'Q3', label: 'Delegate',    sub: 'Urgent & Not Important', Icon: Inbox,  color: '#f59e0b', bg: '#f59e0b08' },
  { id: 'Q4', label: 'Eliminate',   sub: 'Not Urgent & Not Important', Icon: Trash2, color: '#6b7280', bg: '#6b728008' },
]

function getQuadrant(t: Task): Quadrant {
  if (t.urgent && t.important)  return 'Q1'
  if (!t.urgent && t.important) return 'Q2'
  if (t.urgent && !t.important) return 'Q3'
  return 'Q4'
}

function MiniCard({ task }: { task: Task }) {
  const p = PRIORITY_META[task.priority]
  const s = STATUS_META[task.status]
  const isDone = task.status === 'DONE'

  return (
    <div className={`px-3 py-2.5 rounded-lg border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.04]
      transition-all animate-fade-in ${isDone ? 'opacity-40' : ''}`}
      style={{ borderLeft: `3px solid ${p.color}` }}>
      <div className="flex items-start gap-2">
        <div className="flex-1 min-w-0">
          <p className={`text-xs font-medium leading-snug ${isDone ? 'line-through text-fg-muted' : 'text-fg'}`}>
            {task.title}
          </p>
          <div className="flex items-center gap-1.5 mt-1 flex-wrap">
            <span className="badge text-[9px]" style={{ color: s.color, background: s.bg }}>{s.label}</span>
            {task.category && (
              <span className="text-[9px]" style={{ color: task.category.color }}>
                {task.category.name}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default function Eisenhower({ tasks }: Props) {
  const activeTasks = tasks.filter(t => !t.archived)

  const byQuadrant = QUADRANTS.reduce((acc, q) => {
    acc[q.id] = activeTasks.filter(t => getQuadrant(t) === q.id)
    return acc
  }, {} as Record<Quadrant, Task[]>)

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Eisenhower Matrix</h1>
        <span className="text-xs text-fg-subtle">{activeTasks.length} tasks mapped</span>
      </div>

      {/* Axis labels */}
      <div className="relative flex-1 flex flex-col overflow-hidden p-4 gap-4">
        {/* Horizontal axis label (Urgent) */}
        <div className="flex items-center gap-2 px-20 shrink-0">
          <div className="flex-1 text-center">
            <span className="text-[10px] font-bold text-fg-subtle uppercase tracking-widest">← Not Urgent</span>
          </div>
          <div className="w-4 text-center">
            <span className="text-[10px] font-bold text-fg-subtle">|</span>
          </div>
          <div className="flex-1 text-center">
            <span className="text-[10px] font-bold text-fg-subtle uppercase tracking-widest">Urgent →</span>
          </div>
        </div>

        {/* 2x2 grid */}
        <div className="flex-1 grid grid-cols-2 grid-rows-2 gap-3 overflow-hidden">
          {/* Render in order: Q2 (top-left = not-urgent/important), Q1 (top-right = urgent/important) */}
          {/* Q3 (bottom-left = not-urgent/not-important), Q4 (bottom-right = urgent/not-important) */}
          {[
            QUADRANTS[1], // Q2 top-left
            QUADRANTS[0], // Q1 top-right
            QUADRANTS[3], // Q4 bottom-left
            QUADRANTS[2], // Q3 bottom-right
          ].map(q => {
            const qtasks = byQuadrant[q.id]
            return (
              <div key={q.id}
                className="rounded-xl border border-white/[0.06] flex flex-col overflow-hidden"
                style={{ background: q.bg }}>
                {/* Header */}
                <div className="flex items-center gap-2 px-4 py-3 border-b border-white/[0.05]">
                  <q.Icon size={13} style={{ color: q.color }} />
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-bold" style={{ color: q.color }}>{q.label}</p>
                    <p className="text-[9px] text-fg-subtle">{q.sub}</p>
                  </div>
                  <span className="text-[10px] font-bold px-1.5 py-0.5 rounded"
                    style={{ color: q.color, background: q.color + '20' }}>
                    {qtasks.length}
                  </span>
                </div>

                {/* Tasks */}
                <div className="flex-1 overflow-y-auto px-3 py-2.5 space-y-1.5">
                  {qtasks.length === 0 && (
                    <p className="text-[10px] text-fg-subtle text-center mt-4 opacity-40">No tasks here</p>
                  )}
                  {qtasks.map(t => <MiniCard key={t.id} task={t} />)}
                </div>
              </div>
            )
          })}
        </div>

        {/* Vertical axis labels */}
        <div className="absolute left-1 top-14 bottom-8 flex flex-col items-center justify-between pointer-events-none">
          <span className="text-[9px] font-bold text-fg-subtle uppercase tracking-widest"
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
            Important
          </span>
          <span className="text-[9px] font-bold text-fg-subtle uppercase tracking-widest"
            style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
            Not Important
          </span>
        </div>
      </div>
    </div>
  )
}
