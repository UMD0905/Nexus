import { useState } from 'react'
import type { Task, Category, Goal } from '../types'
import { PRIORITY_META } from '../types'
import * as bridge from '../bridge'
import TaskDialog from '../components/TaskDialog'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

type KanbanStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

const COLUMNS: { id: KanbanStatus; label: string; color: string }[] = [
  { id: 'TODO',        label: 'To Do',       color: '#6b7280' },
  { id: 'IN_PROGRESS', label: 'In Progress',  color: '#6366f1' },
  { id: 'DONE',        label: 'Done',         color: '#10b981' },
]

function TaskCard({ task, onEdit }: { task: Task; onEdit: () => void }) {
  const pm = PRIORITY_META[task.priority]
  return (
    <div
      onClick={onEdit}
      className="bg-[#0c1220] border border-white/[0.07] rounded-xl p-3 cursor-pointer hover:border-white/[0.18] transition-all group"
    >
      <div className="flex items-start gap-2 mb-2">
        <span className="text-xs font-semibold" style={{ color: pm.color }}>{pm.label}</span>
      </div>
      <p className="text-sm text-fg font-medium leading-snug">{task.title}</p>
      {task.description && (
        <p className="text-[10px] text-fg-subtle mt-1 line-clamp-2">{task.description}</p>
      )}
      <div className="flex flex-wrap gap-1 mt-2">
        {task.categories?.slice(0, 2).map(c => (
          <span key={c.id} className="text-[9px] px-1.5 py-0.5 rounded-full font-semibold"
            style={{ background: c.color + '22', color: c.color }}>
            {c.name}
          </span>
        ))}
        {task.dueDate && (
          <span className="text-[9px] text-fg-subtle ml-auto">
            {new Date(task.dueDate).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
          </span>
        )}
      </div>
    </div>
  )
}

export default function Kanban({ tasks, categories, goals, onRefresh }: Props) {
  const [editTask, setEditTask] = useState<Task | null>(null)

  const activeTasks = tasks.filter(t => !t.archived)

  const handleDrop = (e: React.DragEvent, targetStatus: KanbanStatus) => {
    e.preventDefault()
    const taskId = Number(e.dataTransfer.getData('taskId'))
    if (!taskId) return
    const task = activeTasks.find(t => t.id === taskId)
    if (!task || task.status === targetStatus) return

    if (targetStatus === 'DONE') {
      bridge.markDone(taskId)
    } else {
      bridge.updateTask({ id: taskId, status: targetStatus })
    }
    onRefresh()
  }

  const handleSave = (data: Partial<Task>) => {
    if (!editTask) return
    bridge.updateTask({ id: editTask.id, ...data })
    onRefresh()
  }

  return (
    <div className="h-full flex flex-col">
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0">
        <h1 className="text-xl font-bold text-fg">Kanban</h1>
        <p className="text-xs text-fg-subtle mt-0.5">Drag cards to change status · click to edit</p>
      </div>

      <div className="flex-1 overflow-hidden px-4 py-4 flex gap-4">
        {COLUMNS.map(col => {
          const colTasks = activeTasks.filter(t => t.status === col.id)
          return (
            <div
              key={col.id}
              className="flex-1 flex flex-col min-w-0 bg-white/[0.02] rounded-2xl border border-white/[0.05] overflow-hidden"
              onDragOver={e => e.preventDefault()}
              onDrop={e => handleDrop(e, col.id)}
            >
              {/* Column header */}
              <div className="px-4 py-3 border-b border-white/[0.05] flex items-center gap-2 shrink-0">
                <span className="w-2 h-2 rounded-full" style={{ background: col.color }} />
                <span className="text-xs font-bold text-fg-muted uppercase tracking-wider">{col.label}</span>
                <span className="ml-auto text-[10px] text-fg-subtle">{colTasks.length}</span>
              </div>

              {/* Cards */}
              <div className="flex-1 overflow-y-auto p-3 space-y-2">
                {colTasks.map(task => (
                  <div
                    key={task.id}
                    draggable
                    onDragStart={e => e.dataTransfer.setData('taskId', String(task.id))}
                  >
                    <TaskCard task={task} onEdit={() => setEditTask(task)} />
                  </div>
                ))}
                {colTasks.length === 0 && (
                  <p className="text-xs text-fg-subtle/40 text-center pt-4">Drop cards here</p>
                )}
              </div>
            </div>
          )
        })}
      </div>

      {editTask && (
        <TaskDialog
          task={editTask}
          categories={categories}
          goals={goals}
          onSave={handleSave}
          onClose={() => { setEditTask(null); onRefresh() }}
        />
      )}
    </div>
  )
}
