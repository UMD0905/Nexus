import type { Task, Category, Goal } from '../types'
import TaskList from './TaskList'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

/**
 * Inbox — unprocessed tasks from quick-add or any task explicitly landed here.
 * Process a task by changing its lifecycle to ANYTIME, TODAY, or SOMEDAY via the task dialog.
 */
export default function Inbox({ tasks, categories, goals, onRefresh }: Props) {
  return (
    <div className="h-full flex flex-col">
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0">
        <h1 className="text-xl font-bold text-fg">Inbox</h1>
        <p className="text-xs text-fg-subtle mt-0.5">
          {tasks.length} unprocessed · capture first, clarify here
        </p>
      </div>
      {tasks.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <p className="text-5xl mb-4">📥</p>
            <p className="text-fg font-semibold">Inbox zero!</p>
            <p className="text-sm text-fg-subtle mt-1">Quick-add tasks land here. Process them when ready.</p>
          </div>
        </div>
      ) : (
        <TaskList tasks={tasks} categories={categories} goals={goals} title="" onRefresh={onRefresh} />
      )}
    </div>
  )
}
