import { useState, useEffect, useCallback } from 'react'
import type { Task, Category, Goal, Project, TimeBlock, Notification, NavSection } from './types'
import * as bridge from './bridge'
import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import QuickAdd from './components/QuickAdd'
import Dashboard from './views/Dashboard'
import TaskList from './views/TaskList'
import Today from './views/Today'
import Week from './views/Week'
import Goals from './views/Goals'
import Pomodoro from './views/Pomodoro'
import Eisenhower from './views/Eisenhower'
import Calendar from './views/Calendar'
import Settings from './views/Settings'
import SearchPalette from './components/SearchPalette'
import Projects from './views/Projects'
import Inbox from './views/Inbox'
import Scheduled from './views/Scheduled'
import Someday from './views/Someday'
import Anytime from './views/Anytime'
import Review from './views/Review'
import Kanban from './views/Kanban'

interface AppData {
  tasks: Task[]
  archivedTasks: Task[]
  categories: Category[]
  goals: Goal[]
  projects: Project[]
  timeBlocks: TimeBlock[]
  notifications: Notification[]
}

const COLOR_PRESETS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#a78bfa', '#f472b6', '#34d399']

/** Reusable dialog for creating or editing a life area */
function CategoryDialog({ initial, onSave, onClose }: {
  initial?: { name: string; color: string }
  onSave: (name: string, color: string) => void
  onClose: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [color, setColor] = useState(initial?.color ?? '#6366f1')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-80 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6">
        <h2 className="text-sm font-bold text-fg mb-4">{initial ? 'Edit Life Area' : 'New Life Area'}</h2>
        <input className="input mb-3" placeholder="Name (e.g. Health, Career…)" value={name}
          onChange={e => setName(e.target.value)} autoFocus />
        <div className="mb-4">
          <p className="text-[10px] text-fg-subtle mb-2">Color</p>
          <div className="flex gap-2 flex-wrap">
            {COLOR_PRESETS.map(c => (
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
            {initial ? 'Save' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}

/** Dialog for creating or editing a project */
function ProjectDialog({ initial, categories, onSave, onClose }: {
  initial?: Partial<Project>
  categories: Category[]
  onSave: (data: Partial<Project>) => void
  onClose: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [catId, setCatId] = useState<number | ''>(initial?.categoryId ?? '')
  const [color, setColor] = useState(initial?.color ?? '#6366f1')
  const [dueDate, setDueDate] = useState(initial?.dueDate?.split('T')[0] ?? '')
  const [status, setStatus] = useState<Project['status']>(initial?.status ?? 'ACTIVE')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }}>
      <div className="w-96 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6 space-y-3">
        <h2 className="text-sm font-bold text-fg">{initial?.id ? 'Edit Project' : 'New Project'}</h2>
        <input className="input" placeholder="Project name *" value={name}
          onChange={e => setName(e.target.value)} autoFocus />
        <textarea className="input resize-none" rows={2} placeholder="Description (optional)"
          value={description} onChange={e => setDescription(e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="form-label">Life Area</label>
            <select className="input" value={catId} onChange={e => setCatId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">None</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label">Status</label>
            <select className="input" value={status} onChange={e => setStatus(e.target.value as Project['status'])}>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>
        </div>
        <div>
          <label className="form-label">Due Date</label>
          <input type="date" className="input" value={dueDate} onChange={e => setDueDate(e.target.value)} />
        </div>
        <div>
          <label className="form-label">Color</label>
          <div className="flex gap-2 flex-wrap">
            {COLOR_PRESETS.map(c => (
              <button key={c} className="w-6 h-6 rounded-full border-2 transition-transform hover:scale-110"
                style={{ background: c, borderColor: color === c ? 'white' : 'transparent' }}
                onClick={() => setColor(c)} />
            ))}
          </div>
        </div>
        <div className="flex gap-2 justify-end pt-1">
          <button className="btn-ghost text-sm" onClick={onClose}>Cancel</button>
          <button className="btn-primary text-sm" onClick={() => {
            if (!name.trim()) return
            onSave({ name, description, categoryId: catId || undefined, color, dueDate: dueDate || undefined, status })
            onClose()
          }}>
            {initial?.id ? 'Save' : 'Create'}
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
    document.documentElement.classList.add('dark')
    return true
  })
  const [showNotifs, setShowNotifs] = useState(false)
  const [showCatDialog, setShowCatDialog] = useState(false)
  const [editCat, setEditCat] = useState<{ id: number; name: string; color: string } | null>(null)
  const [showProjectDialog, setShowProjectDialog] = useState(false)
  const [editProject, setEditProject] = useState<Partial<Project> | null>(null)
  const [showQuickAdd, setShowQuickAdd] = useState(false)
  const [showSearch, setShowSearch] = useState(false)

  const [data, setData] = useState<AppData>({
    tasks: [], archivedTasks: [], categories: [], goals: [],
    projects: [], timeBlocks: [], notifications: [],
  })

  const refresh = useCallback(() => {
    setData({
      tasks:         bridge.getTasks(),
      archivedTasks: bridge.getArchivedTasks(),
      categories:    bridge.getCategories(),
      goals:         bridge.getGoals(),
      projects:      bridge.getProjects(),
      timeBlocks:    bridge.getTimeBlocks(),
      notifications: bridge.getNotifications(),
    })
  }, [])

  useEffect(() => { refresh() }, [refresh])

  // Listen for bridge events (e.g. tray quick-add)
  useEffect(() => {
    (window as Window).onBridgeEvent = (eventJson: string) => {
      try {
        const evt = JSON.parse(eventJson)
        if (evt.type === 'QUICK_ADD_OPEN') setShowQuickAdd(true)
      } catch { /* ignore */ }
    }
  }, [])

  // Ctrl+K → Quick Add  |  Ctrl+Shift+F → Search palette
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'k') { e.preventDefault(); setShowQuickAdd(true) }
      if (e.ctrlKey && e.shiftKey && e.key === 'F') { e.preventDefault(); setShowSearch(true) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark)
    document.body.style.background = isDark ? '#090d18' : '#f0f4f8'
  }, [isDark])

  const handleAddCategory = (name: string, color: string) => {
    bridge.createCategory(name, color)
    refresh()
  }

  const handleEditCategory = (name: string, color: string) => {
    if (!editCat) return
    bridge.updateCategory({ id: editCat.id, name, color })
    refresh()
  }

  const handleDeleteCategory = (cat: { id: number; name: string }) => {
    if (!confirm(`Delete "${cat.name}"? Linked tasks will be unassigned.`)) return
    bridge.deleteCategory(cat.id)
    // If currently viewing this category, navigate away
    if (typeof nav === 'object' && nav.type === 'category' && nav.category.id === cat.id) {
      setNav('all-tasks')
    }
    refresh()
  }

  const handleSaveProject = (data: Partial<Project>) => {
    if (editProject?.id) {
      bridge.updateProject({ ...data, id: editProject.id })
    } else {
      bridge.createProject(data)
    }
    refresh()
  }

  const navKey = typeof nav === 'string' ? nav
    : nav.type === 'category' ? `cat-${nav.category.id}`
    : `proj-${nav.project.id}`

  const renderView = () => {
    // Category view
    if (typeof nav === 'object' && nav.type === 'category') {
      const cat = nav.category
      return (
        <TaskList
          tasks={data.tasks.filter(t => t.categoryIds?.includes(cat.id) || t.categoryId === cat.id)}
          categories={data.categories}
          goals={data.goals}
          title={cat.name}
          onRefresh={refresh}
        />
      )
    }

    // Project view
    if (typeof nav === 'object' && nav.type === 'project') {
      const proj = nav.project
      return (
        <TaskList
          tasks={data.tasks.filter(t => t.projectId === proj.id)}
          categories={data.categories}
          goals={data.goals}
          title={proj.name}
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
        return <Today tasks={data.tasks} timeBlocks={data.timeBlocks} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'week':
        return <Week tasks={data.tasks} />
      case 'goals':
        return <Goals goals={data.goals} categories={data.categories} onRefresh={refresh} />
      case 'projects':
        return <Projects projects={data.projects} categories={data.categories} tasks={data.tasks} onRefresh={refresh}
          onEditProject={p => { setEditProject(p); setShowProjectDialog(true) }} />
      case 'calendar':
        return <Calendar tasks={data.tasks} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'matrix':
        return <Eisenhower tasks={data.tasks} />
      case 'pomodoro':
        return <Pomodoro />
      case 'settings':
        return <Settings />
      case 'inbox':
        return <Inbox tasks={data.tasks.filter(t => t.lifecycle === 'INBOX')} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'anytime':
        return <Anytime tasks={data.tasks.filter(t => t.lifecycle === 'ANYTIME')} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'someday':
        return <Someday tasks={data.tasks.filter(t => t.lifecycle === 'SOMEDAY')} categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'scheduled':
        return <Scheduled categories={data.categories} goals={data.goals} onRefresh={refresh} />
      case 'review':
        return <Review tasks={data.tasks} archivedTasks={data.archivedTasks} goals={data.goals} onRefresh={refresh} />
      case 'kanban':
        return <Kanban tasks={data.tasks} categories={data.categories} goals={data.goals} onRefresh={refresh} />
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
        onQuickAdd={() => setShowQuickAdd(true)}
      />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          active={nav}
          onNav={s => setNav(s)}
          categories={data.categories}
          projects={data.projects}
          onAddCategory={() => setShowCatDialog(true)}
          onEditCategory={cat => { setEditCat({ id: cat.id, name: cat.name, color: cat.color }); /* show edit dialog */ }}
          onDeleteCategory={handleDeleteCategory}
          onAddProject={() => { setEditProject(null); setShowProjectDialog(true) }}
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

      {/* New category dialog */}
      {showCatDialog && (
        <CategoryDialog
          onSave={handleAddCategory}
          onClose={() => setShowCatDialog(false)}
        />
      )}

      {/* Edit category dialog */}
      {editCat && (
        <CategoryDialog
          initial={{ name: editCat.name, color: editCat.color }}
          onSave={handleEditCategory}
          onClose={() => setEditCat(null)}
        />
      )}

      {/* Project dialog */}
      {showProjectDialog && (
        <ProjectDialog
          initial={editProject ?? undefined}
          categories={data.categories}
          onSave={handleSaveProject}
          onClose={() => { setShowProjectDialog(false); setEditProject(null) }}
        />
      )}

      {showQuickAdd && (
        <QuickAdd onClose={() => { setShowQuickAdd(false); refresh() }} />
      )}

      {showSearch && (
        <SearchPalette
          tasks={data.tasks}
          archivedTasks={data.archivedTasks}
          goals={data.goals}
          onClose={() => setShowSearch(false)}
          onNavigate={s => { setNav(s); setShowSearch(false) }}
        />
      )}
    </div>
  )
}
