import { useState, useEffect, useRef } from 'react'
import * as chrono from 'chrono-node'
import { Zap, X } from 'lucide-react'
import * as bridge from '../bridge'
import type { Category } from '../types'

interface ParsedTask {
  title: string
  dueDate: Date | null
  priority: string | null
  categoryName: string | null
  tag: string | null
}

const PRIORITY_RE   = /!(\s*)(critical|high|medium|low)\b/gi
const CATEGORY_RE   = /@([\w\-]+)/gi
const TAG_RE        = /#([\w\-]+)/gi

function parseInput(input: string, categories: Category[]): ParsedTask {
  let text = input

  // 1. Extract priority
  let priority: string | null = null
  text = text.replace(PRIORITY_RE, (_, _sp, p) => {
    priority = p.toUpperCase()
    return ''
  })

  // 2. Extract @CategoryName
  let categoryName: string | null = null
  text = text.replace(CATEGORY_RE, (_, name) => {
    const match = categories.find(c => c.name.toLowerCase() === name.toLowerCase())
    if (match) { categoryName = match.name; return '' }
    return `@${name}` // leave it if not found
  })

  // 3. Extract #tagname
  let tag: string | null = null
  text = text.replace(TAG_RE, (_, t) => {
    tag = t
    return ''
  })

  // 4. Parse date with chrono-node
  const results = chrono.parse(text)
  let dueDate: Date | null = null
  if (results.length > 0) {
    dueDate = results[0].start.date()
    // Remove the matched date text from the title
    const dateText = results[0].text
    text = text.replace(dateText, '')
  }

  // 5. Remaining text → title
  const title = text.replace(/\s{2,}/g, ' ').trim()

  return { title, dueDate, priority, categoryName, tag }
}

interface Props {
  onClose: () => void
}

export default function QuickAdd({ onClose }: Props) {
  const [input, setInput]       = useState('')
  const [categories, setCategories] = useState<Category[]>([])
  const [parsed, setParsed]     = useState<ParsedTask>({ title: '', dueDate: null, priority: null, categoryName: null, tag: null })
  const [toast, setToast]       = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Load categories once
  useEffect(() => {
    setCategories(bridge.getCategories())
  }, [])

  // Re-parse whenever input or categories change
  useEffect(() => {
    setParsed(parseInput(input, categories))
  }, [input, categories])

  // Escape to close
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  // Auto-focus input
  useEffect(() => { inputRef.current?.focus() }, [])

  const handleSubmit = () => {
    if (!parsed.title.trim()) return

    const category = parsed.categoryName
      ? categories.find(c => c.name === parsed.categoryName)
      : null

    const taskData: Record<string, unknown> = {
      title:      parsed.title,
      priority:   parsed.priority ?? 'MEDIUM',
      categoryId: category?.id ?? null,
      lifecycle:  'INBOX',   // Quick-add always lands in Inbox
    }
    if (parsed.dueDate) {
      taskData.dueDate = parsed.dueDate.toISOString().split('T')[0]
    }

    const saved = bridge.createTask(taskData)
    if (saved) {
      const title = saved.title ?? parsed.title
      setToast(`Task created: ${title}`)
      setTimeout(() => {
        setToast(null)
        onClose()
      }, 1200)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') { e.preventDefault(); handleSubmit() }
    if (e.key === 'Escape') { e.preventDefault(); onClose() }
  }

  const dueFmt = parsed.dueDate
    ? parsed.dueDate.toLocaleDateString('en', { weekday: 'short', month: 'short', day: 'numeric' })
    : null

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center pt-24"
      style={{ background: 'rgba(0,0,0,0.75)', backdropFilter: 'blur(4px)' }}
      onClick={onClose}
    >
      <div
        className="w-[560px] bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.7)] animate-fade-in"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center gap-2 px-4 py-3 border-b border-white/[0.06]">
          <Zap size={14} className="text-accent" />
          <span className="text-xs font-bold text-fg flex-1">Quick Add Task</span>
          <button onClick={onClose} className="text-fg-subtle hover:text-fg transition-colors">
            <X size={14} />
          </button>
        </div>

        {/* Input */}
        <div className="px-4 pt-4 pb-2">
          <input
            ref={inputRef}
            className="input w-full text-sm"
            placeholder='e.g. "Review PRs !high @Work #design tomorrow at 3pm"'
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <p className="text-[10px] text-fg-subtle mt-1.5 leading-relaxed">
            Use <span className="text-fg-muted font-mono">!priority</span> &nbsp;
            <span className="text-fg-muted font-mono">@Category</span> &nbsp;
            <span className="text-fg-muted font-mono">#tag</span> and natural date like "tomorrow" or "next Friday"
          </p>
        </div>

        {/* Parsed preview */}
        {input.trim() && (
          <div className="mx-4 mb-3 px-3 py-2 rounded-lg bg-white/[0.04] border border-white/[0.06]">
            <p className="text-[10px] text-fg-subtle uppercase tracking-wider mb-1.5 font-bold">Parsed as</p>
            <div className="flex flex-wrap gap-2">
              {parsed.title ? (
                <span className="text-xs text-fg font-semibold">{parsed.title || <em className="text-fg-subtle">no title</em>}</span>
              ) : (
                <span className="text-xs text-fg-subtle italic">no title</span>
              )}
              {dueFmt && (
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-accent/20 text-accent">
                  {dueFmt}
                </span>
              )}
              {parsed.priority && (
                <span className={`text-[10px] px-1.5 py-0.5 rounded font-semibold ${
                  parsed.priority === 'CRITICAL' ? 'bg-red-500/20 text-red-400' :
                  parsed.priority === 'HIGH'     ? 'bg-orange-500/20 text-orange-400' :
                  parsed.priority === 'MEDIUM'   ? 'bg-yellow-500/20 text-yellow-400' :
                                                   'bg-blue-500/20 text-blue-400'
                }`}>
                  {parsed.priority}
                </span>
              )}
              {parsed.categoryName && (
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400">
                  @{parsed.categoryName}
                </span>
              )}
              {parsed.tag && (
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-400">
                  #{parsed.tag}
                </span>
              )}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button className="btn-ghost text-xs" onClick={onClose}>Cancel</button>
          <button
            className="btn-primary text-xs flex items-center gap-1.5"
            onClick={handleSubmit}
            disabled={!parsed.title.trim()}
          >
            <Zap size={12} /> Add Task
          </button>
        </div>

        {/* Toast */}
        {toast && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 rounded-lg bg-success/20 border border-success/30 text-success text-xs font-semibold animate-fade-in whitespace-nowrap">
            {toast}
          </div>
        )}
      </div>
    </div>
  )
}
