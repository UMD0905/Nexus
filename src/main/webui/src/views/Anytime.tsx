import type { Task, Category, Goal } from '../types'
import TaskList from './TaskList'

interface Props {
  tasks: Task[]
  categories: Category[]
  goals: Goal[]
  onRefresh: () => void
}

/** Anytime — processed tasks with no specific date. */
export default function Anytime({ tasks, categories, goals, onRefresh }: Props) {
  return (
    <TaskList
      tasks={tasks}
      categories={categories}
      goals={goals}
      title="Anytime"
      onRefresh={onRefresh}
    />
  )
}
