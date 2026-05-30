import { useState } from 'react'
import { Plus, X } from 'lucide-react'
import type { Tag } from '../types'

interface Props {
  selectedIds: number[]
  allTags: Tag[]
  onChange: (ids: number[]) => void
  onCreateTag?: (name: string, color: string) => void
}

const PRESET_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#a78bfa', '#06b6d4', '#f97316', '#ec4899']

export default function TagPicker({ selectedIds, allTags, onChange, onCreateTag }: Props) {
  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName]       = useState('')
  const [newColor, setNewColor]     = useState(PRESET_COLORS[0])

  const toggle = (id: number) => {
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter(i => i !== id))
    } else {
      onChange([...selectedIds, id])
    }
  }

  const handleCreate = () => {
    const name = newName.trim()
    if (!name) return
    onCreateTag?.(name, newColor)
    setNewName('')
    setNewColor(PRESET_COLORS[0])
    setShowCreate(false)
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-1.5">
        {allTags.map(tag => {
          const selected = selectedIds.includes(tag.id)
          return (
            <button
              key={tag.id}
              type="button"
              onClick={() => toggle(tag.id)}
              className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border transition-all"
              style={{
                color:       tag.color,
                background:  selected ? tag.color + '28' : 'transparent',
                borderColor: selected ? tag.color : tag.color + '50',
              }}
            >
              {selected && <X size={9} />}
              {tag.name}
            </button>
          )
        })}

        {onCreateTag && (
          <button
            type="button"
            onClick={() => setShowCreate(v => !v)}
            className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs border border-white/[0.12] text-fg-subtle hover:text-fg hover:border-white/25 transition-all"
          >
            <Plus size={10} /> New
          </button>
        )}
      </div>

      {showCreate && (
        <div className="flex items-center gap-2 pt-1">
          <input
            className="input text-xs flex-1 py-1"
            placeholder="Tag name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleCreate() }}
            autoFocus
          />
          <div className="flex gap-1">
            {PRESET_COLORS.map(c => (
              <button
                key={c}
                type="button"
                onClick={() => setNewColor(c)}
                className="w-4 h-4 rounded-full border-2 transition-all"
                style={{
                  background:   c,
                  borderColor:  newColor === c ? '#fff' : 'transparent',
                }}
              />
            ))}
          </div>
          <button
            type="button"
            onClick={handleCreate}
            className="btn-primary text-xs py-1 px-2"
          >
            Add
          </button>
        </div>
      )}
    </div>
  )
}
