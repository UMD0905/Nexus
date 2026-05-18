import { LayoutDashboard, CheckSquare, CalendarDays, CalendarRange, CalendarCheck, Target, Grid2X2, Timer, Archive, Plus, Circle } from 'lucide-react'
import type { Category, NavSection } from '../types'

interface Props {
  active: NavSection
  onNav: (s: NavSection) => void
  categories: Category[]
  onAddCategory: () => void
}

const NAV = [
  { id: 'dashboard',  label: 'Dashboard',  Icon: LayoutDashboard },
  { id: 'all-tasks',  label: 'All Tasks',   Icon: CheckSquare },
  { id: 'today',      label: 'Today',       Icon: CalendarDays },
  { id: 'week',       label: 'This Week',   Icon: CalendarRange },
  { id: 'calendar',   label: 'Calendar',    Icon: CalendarCheck },
  { id: 'goals',      label: 'Goals',       Icon: Target },
  { id: 'matrix',     label: 'Matrix',      Icon: Grid2X2 },
  { id: 'pomodoro',   label: 'Pomodoro',    Icon: Timer },
  { id: 'archive',    label: 'Archive',     Icon: Archive },
] as const

function isActive(active: NavSection, id: string) {
  return typeof active === 'string' && active === id
}
function isCatActive(active: NavSection, id: number) {
  return typeof active === 'object' && active.type === 'category' && active.category.id === id
}

export default function Sidebar({ active, onNav, categories, onAddCategory }: Props) {
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
      <div className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5">
        <p className="text-[9px] font-bold tracking-widest text-fg-subtle px-3 py-2 uppercase">Navigation</p>
        {NAV.map(({ id, label, Icon }) => {
          const sel = isActive(active, id)
          return (
            <button
              key={id}
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
        })}

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
          {categories.length === 0 && (
            <p className="text-xs text-fg-subtle px-3 py-1">No areas yet</p>
          )}
          {categories.map(cat => {
            const sel = isCatActive(active, cat.id)
            return (
              <button
                key={cat.id}
                onClick={() => onNav({ type: 'category', category: cat })}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-all text-left ${
                  sel
                    ? 'bg-accent/10 text-fg font-semibold border-l-[3px] pl-[9px]'
                    : 'text-fg-muted hover:bg-white/[0.04] hover:text-fg border-l-[3px] border-transparent'
                }`}
                style={{ borderLeftColor: sel ? cat.color : 'transparent' }}
              >
                <Circle size={8} fill={cat.color} color={cat.color} />
                {cat.name}
              </button>
            )
          })}
        </div>
      </div>

      {/* Footer */}
      <div className="px-5 py-3 border-t border-white/[0.05]">
        <p className="text-[10px] text-fg-subtle">v1.0.0 · H2 · JavaFX</p>
      </div>
    </aside>
  )
}
