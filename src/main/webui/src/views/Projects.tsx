import { useState } from 'react'
import { MoreHorizontal, Pencil, Trash2, CheckCircle, Clock, FolderOpen } from 'lucide-react'
import type { Project, Category, Task } from '../types'
import * as bridge from '../bridge'

interface Props {
  projects: Project[]
  categories: Category[]
  tasks: Task[]
  onRefresh: () => void
  onEditProject: (project: Project) => void
}

const STATUS_COLOR: Record<string, string> = {
  ACTIVE:    '#6366f1',
  COMPLETED: '#10b981',
  ARCHIVED:  '#6b7280',
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE:    'Active',
  COMPLETED: 'Completed',
  ARCHIVED:  'Archived',
}

function ProjectCard({ project, tasks, categories, onEdit, onDelete }: {
  project: Project
  tasks: Task[]
  categories: Category[]
  onEdit: () => void
  onDelete: () => void
}) {
  const [menuOpen, setMenuOpen] = useState(false)
  const projTasks = tasks.filter(t => t.projectId === project.id)
  const done = projTasks.filter(t => t.status === 'DONE').length
  const progress = projTasks.length > 0 ? Math.round((done / projTasks.length) * 100) : 0
  const cat = categories.find(c => c.id === project.categoryId)

  return (
    <div className="bg-surface border border-white/[0.07] rounded-2xl p-5 hover:border-white/[0.15] transition-all group relative">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="w-2 h-2 rounded-full shrink-0" style={{ background: project.color }} />
            <h3 className="text-sm font-semibold text-fg truncate">{project.name}</h3>
          </div>
          {project.description && (
            <p className="text-xs text-fg-subtle line-clamp-2">{project.description}</p>
          )}
        </div>
        <div className="relative shrink-0 ml-2">
          <button
            onClick={() => setMenuOpen(v => !v)}
            className="p-1 rounded opacity-0 group-hover:opacity-100 hover:bg-white/[0.08] text-fg-subtle transition-all"
          >
            <MoreHorizontal size={14} />
          </button>
          {menuOpen && (
            <div className="absolute right-0 top-7 z-50 w-32 bg-surface border border-white/[0.09] rounded-lg shadow-xl overflow-hidden">
              <button onClick={() => { setMenuOpen(false); onEdit() }}
                className="w-full flex items-center gap-2 px-3 py-2 text-xs text-fg-muted hover:bg-white/[0.06]">
                <Pencil size={11} /> Edit
              </button>
              <button onClick={() => { setMenuOpen(false); onDelete() }}
                className="w-full flex items-center gap-2 px-3 py-2 text-xs text-red-400 hover:bg-red-500/[0.1]">
                <Trash2 size={11} /> Delete
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Meta */}
      <div className="flex items-center gap-3 text-[10px] text-fg-subtle mb-3 flex-wrap">
        <span className="px-2 py-0.5 rounded-full font-semibold"
          style={{ background: STATUS_COLOR[project.status] + '22', color: STATUS_COLOR[project.status] }}>
          {STATUS_LABEL[project.status]}
        </span>
        {cat && (
          <span className="flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full" style={{ background: cat.color }} />
            {cat.name}
          </span>
        )}
        {project.dueDate && (
          <span className="flex items-center gap-1">
            <Clock size={10} /> Due {new Date(project.dueDate).toLocaleDateString('en', { month: 'short', day: 'numeric' })}
          </span>
        )}
      </div>

      {/* Progress */}
      {projTasks.length > 0 && (
        <div>
          <div className="flex items-center justify-between text-[10px] text-fg-subtle mb-1">
            <span className="flex items-center gap-1"><CheckCircle size={10} /> {done}/{projTasks.length} tasks</span>
            <span>{progress}%</span>
          </div>
          <div className="h-1 bg-white/[0.06] rounded-full overflow-hidden">
            <div className="h-full rounded-full transition-all duration-500"
              style={{ width: `${progress}%`, background: project.color }} />
          </div>
        </div>
      )}
      {projTasks.length === 0 && (
        <p className="text-[10px] text-fg-subtle/50 flex items-center gap-1">
          <FolderOpen size={10} /> No tasks yet
        </p>
      )}
    </div>
  )
}

export default function Projects({ projects, categories, tasks, onRefresh, onEditProject }: Props) {
  const active    = projects.filter(p => p.status === 'ACTIVE')
  const completed = projects.filter(p => p.status === 'COMPLETED')
  const archived  = projects.filter(p => p.status === 'ARCHIVED')

  const handleDelete = (project: Project) => {
    if (!confirm(`Delete "${project.name}"? Linked tasks will remain but lose their project.`)) return
    bridge.deleteProject(project.id)
    onRefresh()
  }

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-fg">Projects</h1>
          <p className="text-xs text-fg-subtle mt-0.5">{active.length} active</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-8 py-5 space-y-6">
        {/* Active */}
        {active.length > 0 && (
          <section>
            <h2 className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">Active</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {active.map(p => (
                <ProjectCard
                  key={p.id}
                  project={p}
                  tasks={tasks}
                  categories={categories}
                  onEdit={() => onEditProject(p)}
                  onDelete={() => handleDelete(p)}
                />
              ))}
            </div>
          </section>
        )}

        {projects.length === 0 && (
          <div className="flex-1 flex flex-col items-center justify-center py-16">
            <p className="text-4xl mb-4">📁</p>
            <p className="text-fg font-semibold">No projects yet</p>
            <p className="text-sm text-fg-subtle mt-1">Create a project to group related tasks.</p>
          </div>
        )}

        {/* Completed */}
        {completed.length > 0 && (
          <section>
            <h2 className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">Completed</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {completed.map(p => (
                <ProjectCard key={p.id} project={p} tasks={tasks} categories={categories}
                  onEdit={() => onEditProject(p)} onDelete={() => handleDelete(p)} />
              ))}
            </div>
          </section>
        )}

        {/* Archived */}
        {archived.length > 0 && (
          <section>
            <h2 className="text-[10px] font-bold tracking-widest text-fg-subtle uppercase mb-3">Archived</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {archived.map(p => (
                <ProjectCard key={p.id} project={p} tasks={tasks} categories={categories}
                  onEdit={() => onEditProject(p)} onDelete={() => handleDelete(p)} />
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
