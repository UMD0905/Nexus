import { useState, useEffect, useCallback } from 'react'
import type { Task, Category, Goal, TimeBlock, Notification, NavSection } from './types'
import * as bridge from './bridge'
import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import Dashboard from './views/Dashboard'
import TaskList from './views/TaskList'
import Today from './views/Today'
import Week from './views/Week'
import Goals from './views/Goals'
import Pomodoro from './views/Pomodoro'
import Eisenhower from './views/Eisenhower'
import Calendar from './views/Calendar'

interface AppData {
  tasks: Task[]
  archivedTasks: Task[]
  categories: Category[]
  goals: Goal[]
  timeBlocks: TimeBlock[]
  notifications: Notification[]
}

function CategoryDialog({ onSave, onClose }: {
  onSave: (name: string, color: string) => void
  onClose: () => void
}) {
  const [name, setName] = useState('')
  const [color, setColor] = useState('#6366f1')
  const PRESETS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#a78bfa', '#f472b6', '#34d399']

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-80 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6">
        <h2 className="text-sm font-bold text-fg mb-4">New Life Area</h2>
        <input className="input mb-3" placeholder="Name (e.g. Health, Career…)" value={name}
          onChange={e => setName(e.target.value)} autoFocus />
        <div className="mb-4">
          <p className="text-[10px] text-fg-subtle mb-2">Color</p>
          <div className="flex gap-2 flex-wrap">
            {PRESETS.map(c => (
              <button key={c}
                className="w-6 h-6 rounded-full border-2 transition-transform hover:scale-110"
                style={{ background: c, borderColor: color === c ? 'white' : 'transparent' }}
                onClick={() => setColor(c)} />
            ))}
          </div>
        </div>
        <div className="flex gap-2 justify-end">
          <button className="btn-ghost text-sm" onClick={onClose}>Cancel</button>
          <button className="btn-primary text-sm"
            onClick={() => { if (name.trim()) { onSave(name.trim(), color); onClose() } }}>
            Create
          </button>
        </div>
      </div>
    </div>
  )
}

function NotificationPanel({ notifications, onClose }: { notifications: Notification[]; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-40" onClick={onClose}>
      <div
        className="absolute top-12 right-4 w-80 bg-surface rounded-xl border border-white/[0.09]
          shadow-[0_16px_48px_rgba(0,0,0,0.5)] animate-fade-in overflow-hidden"
        onClick={e => e.stopPropagation()}>
        <div className="px-4 py-3 border-b border-white/[0.06] flex items-center justify-between">
          <p className="text-xs font-bold text-fg">Notifications</p>
          <span className="text-[10px] text-fg-subtle">{notifications.filter(n => !n.read).length} unread</span>
        </div>
        <div className="max-h-80 overflow-y-auto divide-y divide-white/[0.04]">
          {notifications.length === 0 && (
            <p className="text-xs text-fg-subtle text-center py-8">No notifications</p>
          )}
          {notifications.map(n => (
            <div key={n.id} className={`px-4 py-3 ${!n.read ? 'bg-accent/[0.04]' : ''}`}>
              <p className="text-xs font-semibold text-fg">{n.title}</p>
              {n.body && <p className="text-[10px] text-fg-subtle mt-0.5">{n.body}</p>}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function App() {
  const [nav, setNav] = useState<NavSection>('dashboard')
  const [isDark, setIsDark] = useState(() => {
    // Apply dark class immediately to avoid flash of unstyled content
    document.documentElement.classList.add('dark')
    return true
  })
  const [showNotifs, setShowNotifs] = useState(false)
  const [showCatDialog, setShowCatDialog] = useState(false)

  const [data, setData] = useState<AppData>({
    tasks: [], archivedTasks: [], categories: [], goals: [],
    timeBlocks: [], notifications: [],
  })

  const refresh = useCallback(() => {
    setData({
      tasks:         bridge.getTasks(),
      archivedTasks: bridge.getArchivedTasks(),
      categories:    bridge.getCategories(),
      goals:         bridge.getGoals(),
      timeBlocks:    bridge.getTimeBlocks(),
      notifications: bridge.getNotifications(),
    })
  }, [])

  useEffect(() => { refresh() }, [refresh])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark)
    document.body.style.background = isDark ? '#090d18' : '#f0f4f8'
  }, [isDark])

  const handleAddCategory = (name: string, color: string) => {
    bridge.createCategory(name, color)
    refresh()
  }

  const navKey = typeof nav === 'string' ? nav : `cat-${(nav as { category: Category }).category.id}`

  const renderView = () => {
    if (typeof nav === 'object' && nav.type === 'category') {
      return (
        <TaskList
          tasks={data.tasks.filter(t => t.categoryId === nav.category.id)}
          categories={data.categories}
          goals={data.goals}
          title={nav.category.name}
          onRefresh={refresh}
        />
      )
    }

    switch (nav) {
      case 'dashboard':
        return <Dashboard />
      case 'all-tasks':
        return <TaskList tasks={data.tasks} categories={data.categories} goals={data.goals} title="All Tasks" onRefresh={refresh} />
      case 'today':
        return <Today tasks={data.tasks} timeBlocks={data.timeBlocks} onRefresh={refresh} />
      case 'week':
        return <Week tasks={data.tasks} />
      case 'goals':
        return <Goals goals={data.goals} categories={data.categories} onRefresh={refresh} />
      case 'calendar':
        return <Calendar tasks={data.tasks} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'matrix':
        return <Eisenhower tasks={data.tasks} />
      case 'pomodoro':
        return <Pomodoro />
      case 'archive':
        return (
          <TaskList
            tasks={data.archivedTasks}
            categories={data.categories}
            goals={data.goals}
            title="Archive"
            onRefresh={refresh}
            showArchived
          />
        )
      default:
        return null
    }
  }

  return (
    <div className="flex flex-col h-screen overflow-hidden text-fg bg-canvas">
      <TopBar
        notifications={data.notifications}
        onBellClick={() => setShowNotifs(v => !v)}
        isDark={isDark}
        onToggleTheme={() => setIsDark(d => !d)}
      />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          active={nav}
          onNav={s => setNav(s)}
          categories={data.categories}
          onAddCategory={() => setShowCatDialog(true)}
        />

        <main className="flex-1 overflow-hidden relative">
          <div className="absolute inset-0 animate-fade-in" key={navKey}>
            {renderView()}
          </div>
        </main>
      </div>

      {showNotifs && (
        <NotificationPanel
          notifications={data.notifications}
          onClose={() => setShowNotifs(false)}
        />
      )}

      {showCatDialog && (
        <CategoryDialog
          onSave={handleAddCategory}
          onClose={() => setShowCatDialog(false)}
        />
      )}
    </div>
  )
}
