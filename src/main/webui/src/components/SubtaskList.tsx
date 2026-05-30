import { useState, useEffect } from 'react'
import { CheckSquare, Square, Trash2, Plus, GripVertical } from 'lucide-react'
import type { Subtask } from '../types'
import * as bridge from '../bridge'
import {
  DndContext, closestCenter, PointerSensor, KeyboardSensor,
  useSensor, useSensors, type DragEndEvent,
} from '@dnd-kit/core'
import {
  SortableContext, sortableKeyboardCoordinates, useSortable,
  verticalListSortingStrategy, arrayMove,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'

interface Props {
  taskId: number
  onUpdate?: () => void
}

function SortableRow({ s, onToggle, onDelete }: {
  s: Subtask
  onToggle: () => void
  onDelete: () => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: s.id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div ref={setNodeRef} style={style} className="flex items-center gap-2 group py-0.5">
      <button
        {...attributes}
        {...listeners}
        className="shrink-0 text-fg-subtle/30 hover:text-fg-subtle/70 cursor-grab active:cursor-grabbing transition-colors"
        tabIndex={-1}
      >
        <GripVertical size={11} />
      </button>
      <button
        onClick={onToggle}
        className="shrink-0 text-fg-subtle hover:text-accent transition-colors"
      >
        {s.done
          ? <CheckSquare size={14} className="text-success" />
          : <Square size={14} />}
      </button>
      <span className={`flex-1 text-xs ${s.done ? 'line-through text-fg-muted' : 'text-fg'}`}>
        {s.title}
      </span>
      <button
        onClick={onDelete}
        className="shrink-0 opacity-0 group-hover:opacity-100 text-fg-subtle hover:text-danger transition-all"
      >
        <Trash2 size={11} />
      </button>
    </div>
  )
}

export default function SubtaskList({ taskId, onUpdate }: Props) {
  const [subtasks, setSubtasks] = useState<Subtask[]>([])
  const [newTitle, setNewTitle] = useState('')

  const load = () => setSubtasks(bridge.getSubtasks(taskId))

  useEffect(() => { load() }, [taskId])

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id) return

    setSubtasks(prev => {
      const oldIdx = prev.findIndex(s => s.id === active.id)
      const newIdx = prev.findIndex(s => s.id === over.id)
      const reordered = arrayMove(prev, oldIdx, newIdx)
      bridge.reorderSubtasks(taskId, reordered.map(s => s.id))
      return reordered
    })
  }

  const handleToggle = (id: number) => {
    bridge.toggleSubtask(id)
    load()
    onUpdate?.()
  }

  const handleDelete = (id: number) => {
    bridge.deleteSubtask(id)
    load()
    onUpdate?.()
  }

  const handleAdd = () => {
    const title = newTitle.trim()
    if (!title) return
    bridge.createSubtask({ taskId, title })
    setNewTitle('')
    load()
    onUpdate?.()
  }

  return (
    <div className="space-y-1">
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
        <SortableContext items={subtasks.map(s => s.id)} strategy={verticalListSortingStrategy}>
          {subtasks.map(s => (
            <SortableRow
              key={s.id}
              s={s}
              onToggle={() => handleToggle(s.id)}
              onDelete={() => handleDelete(s.id)}
            />
          ))}
        </SortableContext>
      </DndContext>

      <div className="flex items-center gap-2 pt-1">
        <Plus size={13} className="text-fg-subtle shrink-0" />
        <input
          className="flex-1 bg-transparent text-xs text-fg placeholder:text-fg-subtle outline-none border-b border-white/[0.08] focus:border-accent/50 pb-0.5 transition-colors"
          placeholder="Add subtask…"
          value={newTitle}
          onChange={e => setNewTitle(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') handleAdd() }}
          onBlur={() => { if (newTitle.trim()) handleAdd() }}
        />
      </div>
    </div>
  )
}
