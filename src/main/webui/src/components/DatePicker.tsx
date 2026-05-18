import { useState, useRef, useEffect } from 'react'
import { ChevronLeft, ChevronRight, Calendar as CalIcon } from 'lucide-react'

interface Props {
  value?: string                          // YYYY-MM-DD
  onChange: (date: string | undefined) => void
  placeholder?: string
}

const MONTHS = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December',
]
const DOW = ['Mo','Tu','We','Th','Fr','Sa','Su']

function pad(n: number) { return String(n).padStart(2, '0') }

function toDateStr(year: number, month: number, day: number) {
  return `${year}-${pad(month + 1)}-${pad(day)}`
}

export default function DatePicker({ value, onChange, placeholder = 'Pick a date' }: Props) {
  const [open, setOpen]           = useState(false)
  const [viewYear, setViewYear]   = useState(() => value ? new Date(value).getFullYear() : new Date().getFullYear())
  const [viewMonth, setViewMonth] = useState(() => value ? new Date(value).getMonth()    : new Date().getMonth())
  const ref = useRef<HTMLDivElement>(null)

  // Sync view when value changes externally
  useEffect(() => {
    if (value) {
      const d = new Date(value)
      setViewYear(d.getFullYear())
      setViewMonth(d.getMonth())
    }
  }, [value])

  // Close on outside click
  useEffect(() => {
    if (!open) return
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  // ── Build calendar grid ─────────────────────────────────────────────────
  type Cell = { year: number; month: number; day: number; current: boolean }

  const cells: Cell[] = []
  const firstDow    = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7  // 0 = Mon
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const prevDays    = new Date(viewYear, viewMonth, 0).getDate()

  const prevY = viewMonth === 0 ? viewYear - 1 : viewYear
  const prevM = viewMonth === 0 ? 11            : viewMonth - 1
  const nextY = viewMonth === 11 ? viewYear + 1 : viewYear
  const nextM = viewMonth === 11 ? 0             : viewMonth + 1

  for (let i = firstDow - 1; i >= 0; i--)
    cells.push({ year: prevY, month: prevM, day: prevDays - i, current: false })

  for (let d = 1; d <= daysInMonth; d++)
    cells.push({ year: viewYear, month: viewMonth, day: d, current: true })

  const fill = (7 - (cells.length % 7)) % 7
  for (let d = 1; d <= fill; d++)
    cells.push({ year: nextY, month: nextM, day: d, current: false })

  // ── Helpers ─────────────────────────────────────────────────────────────
  const todayStr    = toDateStr(new Date().getFullYear(), new Date().getMonth(), new Date().getDate())
  const selectedStr = value ?? ''

  const isSelected = (c: Cell) => toDateStr(c.year, c.month, c.day) === selectedStr
  const isToday    = (c: Cell) => toDateStr(c.year, c.month, c.day) === todayStr

  const prevMonth = () => { setViewYear(prevY); setViewMonth(prevM) }
  const nextMonth = () => { setViewYear(nextY); setViewMonth(nextM) }

  const pick = (c: Cell) => { onChange(toDateStr(c.year, c.month, c.day)); setOpen(false) }
  const pickToday = () => { onChange(todayStr); setOpen(false) }
  const clear = (e: React.MouseEvent) => { e.stopPropagation(); onChange(undefined) }

  const displayLabel = value
    ? new Date(value + 'T12:00:00').toLocaleDateString('en', { month: 'short', day: 'numeric', year: 'numeric' })
    : ''

  return (
    <div className="relative" ref={ref}>
      {/* ── Trigger ─────────────────────────────────────────────────────── */}
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="input flex items-center gap-2 text-left"
      >
        <CalIcon size={13} className="text-fg-subtle shrink-0" />
        <span className={`flex-1 truncate ${displayLabel ? 'text-fg' : 'text-fg-subtle'}`}>
          {displayLabel || placeholder}
        </span>
        {value && (
          <span
            onClick={clear}
            className="text-fg-subtle hover:text-danger transition-colors text-xs leading-none px-0.5"
            title="Clear date"
          >✕</span>
        )}
      </button>

      {/* ── Dropdown ────────────────────────────────────────────────────── */}
      {open && (
        <div className="absolute z-50 mt-1 left-0 w-64 bg-surface rounded-xl border border-border
          shadow-[0_8px_40px_rgba(0,0,0,0.35)] animate-fade-in p-3 select-none">

          {/* Month nav */}
          <div className="flex items-center justify-between mb-2">
            <button onClick={prevMonth}
              className="p-1.5 rounded-lg hover:bg-black/[0.06] dark:hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
              <ChevronLeft size={14} />
            </button>
            <span className="text-xs font-bold text-fg">{MONTHS[viewMonth]} {viewYear}</span>
            <button onClick={nextMonth}
              className="p-1.5 rounded-lg hover:bg-black/[0.06] dark:hover:bg-white/[0.06] text-fg-subtle hover:text-fg transition-colors">
              <ChevronRight size={14} />
            </button>
          </div>

          {/* Weekday headers */}
          <div className="grid grid-cols-7 mb-1">
            {DOW.map(d => (
              <div key={d} className="text-center text-[9px] font-bold text-fg-subtle uppercase py-0.5">{d}</div>
            ))}
          </div>

          {/* Day cells */}
          <div className="grid grid-cols-7 gap-0.5">
            {cells.map((cell, i) => {
              const sel = isSelected(cell)
              const tod = isToday(cell)
              return (
                <button key={i} onClick={() => pick(cell)}
                  className={[
                    'h-7 w-full rounded-lg text-[11px] font-medium transition-all',
                    sel ? 'bg-accent text-white font-bold shadow-accent' : '',
                    !sel && tod ? 'ring-2 ring-accent text-accent font-bold' : '',
                    !sel && !tod && cell.current ? 'text-fg hover:bg-black/[0.06] dark:hover:bg-white/[0.06]' : '',
                    !cell.current ? 'text-fg-subtle opacity-40 hover:opacity-60' : '',
                  ].join(' ')}>
                  {cell.day}
                </button>
              )
            })}
          </div>

          {/* Today shortcut */}
          <div className="mt-2 pt-2 border-t border-border">
            <button onClick={pickToday}
              className="w-full text-[10px] font-semibold text-accent py-1 rounded-lg hover:bg-accent/10 transition-colors">
              Today
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
