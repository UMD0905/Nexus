import React, { useState, useEffect, useRef } from 'react'
import { LayoutDashboard, CheckSquare, CalendarDays, CalendarRange, CalendarCheck, Target, Grid2X2, Timer, Archive, Plus, Circle, Settings, GripVertical, Inbox, Layers, Clock, CalendarClock, Kanban, ClipboardList, Pencil, Trash2, MoreHorizontal, FolderKanban, Flame, Wallet } from 'lucide-react'
import type { Category, Project, NavSection } from '../types'
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
  active: NavSection
  onNav: (s: NavSection) => void
  categories: Category[]
  projects: Project[]
  onAddCategory: () => void
  onEditCategory: (cat: Category) => void
  onDeleteCategory: (cat: Category) => void
  onAddProject: () => void
}

const NAV_MAIN = [
  { id: 'dashboard',  label: 'Dashboard',  Icon: LayoutDashboard },
  { id: 'all-tasks',  label: 'All Tasks',   Icon: CheckSquare },
  { id: 'today',      label: 'Today',       Icon: CalendarDays },
  { id: 'week',       label: 'This Week',   Icon: CalendarRange },
  { id: 'calendar',   label: 'Calendar',    Icon: CalendarCheck },
  { id: 'kanban',     label: 'Kanban',      Icon: Kanban },
] as const

const NAV_GTD = [
  { id: 'inbox',     label: 'Inbox',      Icon: Inbox },
  { id: 'anytime',   label: 'Anytime',    Icon: Layers },
  { id: 'someday',   label: 'Someday',    Icon: Clock },
  { id: 'scheduled', label: 'Scheduled',  Icon: CalendarClock },
] as const

const NAV_PLAN = [
  { id: 'goals',    label: 'Goals',     Icon: Target },
  { id: 'projects', label: 'Projects',  Icon: FolderKanban },
  { id: 'streaks',  label: 'Streaks',   Icon: Flame },
  { id: 'matrix',   label: 'Matrix',    Icon: Grid2X2 },
  { id: 'pomodoro', label: 'Pomodoro',  Icon: Timer },
  { id: 'review',   label: 'Review',    Icon: ClipboardList },
] as const

const NAV_UTIL = [
  { id: 'finance',  label: 'Finance',   Icon: Wallet },
  { id: 'archive',  label: 'Archive',   Icon: Archive },
  { id: 'settings', label: 'Settings',  Icon: Settings },
] as const

function isActive(active: NavSection, id: string) {
  return typeof active === 'string' && active === id
}
function isCatActive(active: NavSection, id: number) {
  return typeof active === 'object' && active.type === 'category' && active.category.id === id
}
function isProjActive(active: NavSection, id: number) {
  return typeof active === 'object' && active.type === 'project' && active.project.id === id
}

function SortableCatRow({ cat, sel, onNav, onEdit, onDelete }: {
  cat: Category
  sel: boolean
  onNav: (cat: Category) => void
  onEdit: (cat: Category) => void
  onDelete: (cat: Category) => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: cat.id })
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Close menu on outside click
  useEffect(() => {
    if (!menuOpen) return
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [menuOpen])

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div ref={setNodeRef} style={style} className="flex items-center group relative">
      <button
        {...attributes}
        {...listeners}
        className="pl-2 pr-1 py-2 text-fg-subtle/20 hover:text-fg-subtle/50 cursor-grab active:cursor-grabbing transition-colors shrink-0 opacity-0 group-hover:opacity-100"
        tabIndex={-1}
      >
        <GripVertical size={11} />
      </button>
      <button
        onClick={() => onNav(cat)}
        className={`flex-1 flex items-center gap-3 pr-1 py-2 rounded-lg text-sm transition-all text-left ${
          sel
            ? 'bg-accent/10 text-fg font-semibold border-l-[3px] pl-[9px]'
            : 'text-fg-muted hover:bg-white/[0.04] hover:text-fg border-l-[3px] border-transparent pl-3'
        }`}
        style={{ borderLeftColor: sel ? cat.color : 'transparent' }}
      >
        <Circle size={8} fill={cat.color} color={cat.color} />
        <span className="flex-1 truncate">{cat.name}</span>
      </button>
      {/* Context menu trigger */}
      <button
        onClick={() => setMenuOpen(v => !v)}
        className="mr-1 p-1 rounded opacity-0 group-hover:opacity-100 hover:bg-white/[0.08] text-fg-subtle transition-all"
        tabIndex={-1}
        title="More actions"
      >
        <MoreHorizontal size={12} />
      </button>
      {menuOpen && (
        <div ref={menuRef}
          className="absolute right-0 top-7 z-50 w-32 bg-surface border border-white/[0.09] rounded-lg shadow-xl overflow-hidden"
          style={{ boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
          <button
            onClick={() => { setMenuOpen(false); onEdit(cat) }}
            className="w-full flex items-center gap-2 px-3 py-2 text-xs text-fg-muted hover:bg-white/[0.06] hover:text-fg"
          >
            <Pencil size={11} /> Edit
          </button>
          <button
            onClick={() => { setMenuOpen(false); onDelete(cat) }}
            className="w-full flex items-center gap-2 px-3 py-2 text-xs text-red-400 hover:bg-red-500/[0.1]"
          >
            <Trash2 size={11} /> Delete
          </button>
        </div>
      )}
    </div>
  )
}

function NavBtn({ id, label, Icon, active, onNav }: {
  id: string; label: string; Icon: React.ElementType; active: NavSection; onNav: (s: NavSection) => void
}) {
  const sel = isActive(active, id)
  return (
    <button
      onClick={() => onNav(id as NavSection)}
      className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-all text-left group ${
        sel
          ? 'bg-accent/10 text-accent font-semibold border-l-[3px] border-accent pl-[9px]'
          : 'text-fg-muted hover:bg-white/[0.04] hover:text-fg border-l-[3px] border-transparent'
      }`}
    >
      <Icon size={15} className={sel ? 'text-accent' : 'text-fg-subtle group-hover:text-fg-muted'} />
      {label}
    </button>
  )
}

export default function Sidebar({ active, onNav, categories, projects, onAddCategory, onEditCategory, onDeleteCategory, onAddProject }: Props) {
  const [sorted, setSorted] = useState<Category[]>(categories)

  // Sync when parent refreshes categories
  useEffect(() => { setSorted(categories) }, [categories])

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  const handleDragEnd = (event: DragEndEvent) => {
    const { active: dragged, over } = event
    if (!over || dragged.id === over.id) return
    setSorted(prev => {
      const oldIdx = prev.findIndex(c => c.id === dragged.id)
      const newIdx = prev.findIndex(c => c.id === over.id)
      const reordered = arrayMove(prev, oldIdx, newIdx)
      bridge.reorderCategories(reordered.map(c => c.id))
      return reordered
    })
  }

  return (
    <aside className="w-56 h-full flex flex-col border-r border-white/[0.06] bg-gradient-to-b from-[#0c1220] to-canvas select-none shrink-0">
      {/* Logo */}
      <div className="px-5 py-5 border-b border-white/[0.05]">
        <span className="text-xl font-bold text-accent" style={{ textShadow: '0 0 20px rgba(99,102,241,0.5)' }}>
          Nexus
        </span>
        <p className="text-xs text-fg-subtle mt-0.5">Productivity Hub</p>
      </div>

      {/* Nav */}
      <div className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5 scrollbar-thin">
        <p className="text-[9px] font-bold tracking-widest text-fg-subtle px-3 py-2 uppercase">Workspace</p>
        {NAV_MAIN.map(({ id, label, Icon }) => (
          <NavBtn key={id} id={id} label={label} Icon={Icon} active={active} onNav={onNav} />
        ))}

        {/* GTD buckets */}
        <p className="text-[9px] font-bold tracking-widest text-fg-subtle px-3 pt-4 pb-1 uppercase">Capture</p>
        {NAV_GTD.map(({ id, label, Icon }) => (
          <NavBtn key={id} id={id} label={label} Icon={Icon} active={active} onNav={onNav} />
        ))}

        {/* Planning */}
        <p className="text-[9px] font-bold tracking-widest text-fg-subtle px-3 pt-4 pb-1 uppercase">Plan</p>
        {NAV_PLAN.map(({ id, label, Icon }) => (
          <NavBtn key={id} id={id} label={label} Icon={Icon} active={active} onNav={onNav} />
        ))}

        {/* Life Areas */}
        <div className="pt-3">
          <div className="flex items-center px-3 py-1.5">
            <p className="text-[9px] font-bold tracking-widest text-fg-subtle uppercase flex-1">Life Areas</p>
            <button
              onClick={onAddCategory}
              className="text-fg-subtle hover:text-accent transition-colors p-0.5 rounded"
              title="Add life area"
            >
              <Plus size={13} />
            </button>
          </div>
          {sorted.length === 0 && (
            <p className="text-xs text-fg-subtle px-3 py-1">No areas yet</p>
          )}
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
            <SortableContext items={sorted.map(c => c.id)} strategy={verticalListSortingStrategy}>
              {sorted.map(cat => (
                <SortableCatRow
                  key={cat.id}
                  cat={cat}
                  sel={isCatActive(active, cat.id)}
                  onNav={cat => onNav({ type: 'category', category: cat })}
                  onEdit={onEditCategory}
                  onDelete={onDeleteCategory}
                />
              ))}
            </SortableContext>
          </DndContext>

          {/* Projects under each life area */}
          {sorted.map(cat => {
            const catProjects = projects.filter(p => p.categoryId === cat.id && p.status === 'ACTIVE')
            if (catProjects.length === 0) return null
            return (
              <div key={`proj-${cat.id}`} className="ml-4 mt-1 space-y-0.5">
                {catProjects.map(p => {
                  const sel = isProjActive(active, p.id)
                  return (
                    <button
                      key={p.id}
                      onClick={() => onNav({ type: 'project', project: p })}
                      className={`w-full flex items-center gap-2 px-2 py-1 rounded-lg text-xs transition-all text-left ${
                        sel
                          ? 'bg-accent/10 text-accent font-semibold'
                          : 'text-fg-subtle hover:bg-white/[0.04] hover:text-fg'
                      }`}
                    >
                      <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: p.color }} />
                      <span className="truncate">{p.name}</span>
                    </button>
                  )
                })}
              </div>
            )
          })}
        </div>

        {/* Util */}
        <div className="pt-2">
          {NAV_UTIL.map(({ id, label, Icon }) => (
            <NavBtn key={id} id={id} label={label} Icon={Icon} active={active} onNav={onNav} />
          ))}
        </div>
      </div>

      {/* Add project button + Footer */}
      <div className="px-3 py-3 border-t border-white/[0.05] space-y-2">
        <button
          onClick={onAddProject}
          className="w-full flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs text-fg-subtle hover:text-accent hover:bg-accent/[0.08] transition-all"
        >
          <Plus size={12} /> New Project
        </button>
        <p className="text-[10px] text-fg-subtle/50 px-1">v1.0.0 · H2 · JavaFX</p>
      </div>
    </aside>
  )
}
