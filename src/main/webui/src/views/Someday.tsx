import type { Task, Category, Goal } from '../types'
import TaskList from './TaskList'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

/** Someday — indefinitely deferred tasks; hidden from main views. */
export default function Someday({ tasks, categories, goals, onRefresh }: Props) {
  return (
    <div className="h-full flex flex-col">
      <div className="px-8 pt-6 pb-3 border-b border-white/[0.05] shrink-0">
        <h1 className="text-xl font-bold text-fg">Someday</h1>
        <p className="text-xs text-fg-subtle mt-0.5">
          {tasks.length} on hold · review monthly to promote or drop
        </p>
      </div>
      {tasks.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="text-fg-subtle text-sm">Nothing filed here — park low-priority ideas for later.</p>
        </div>
      ) : (
        <TaskList tasks={tasks} categories={categories} goals={goals} title="" onRefresh={onRefresh} />
      )}
    </div>
  )
}
