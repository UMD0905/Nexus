import { useState, useRef, useEffect } from 'react'
import { Clock } from 'lucide-react'

interface Props {
  value?: number          // total minutes
  onChange: (minutes: number | undefined) => void
  placeholder?: string
}

const HOURS   = [0, 1, 2, 3, 4, 5, 6, 7, 8]
const MINUTES = [0, 5, 10, 15, 20, 25, 30, 45]

function fmt(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}m`
  if (m === 0) return `${h}h`
  return `${h}h ${m}m`
}

export default function DurationPicker({ value, onChange, placeholder = 'Set duration' }: Props) {
  const [open, setOpen]   = useState(false)
  const [selH, setSelH]   = useState(() => value != null ? Math.floor(value / 60) : 0)
  const [selM, setSelM]   = useState(() => value != null ? value % 60 : 0)
  const ref = useRef<HTMLDivElement>(null)

  // Sync state when value changes externally
  useEffect(() => {
    if (value != null) {
      setSelH(Math.floor(value / 60))
      setSelM(value % 60)
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

  const commit = (h: number, m: number) => {
    const total = h * 60 + m
    onChange(total === 0 ? undefined : total)
    if (total > 0) setOpen(false)
  }

  const clear = (e: React.MouseEvent) => {
    e.stopPropagation()
    setSelH(0)
    setSelM(0)
    onChange(undefined)
  }

  return (
    <div className="relative" ref={ref}>
      {/* Trigger */}
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="input flex items-center gap-2 text-left"
      >
        <Clock size={13} className="text-fg-subtle shrink-0" />
        <span className={`flex-1 truncate ${value ? 'text-fg' : 'text-fg-subtle'}`}>
          {value ? fmt(value) : placeholder}
        </span>
        {value && (
          <span
            onClick={clear}
            className="text-fg-subtle hover:text-danger transition-colors text-xs leading-none px-0.5"
            title="Clear duration"
          >✕</span>
        )}
      </button>

      {/* Dropdown */}
      {open && (
        <div className="absolute z-50 mt-1 left-0 bg-surface rounded-xl border border-border
          shadow-[0_8px_40px_rgba(0,0,0,0.35)] animate-fade-in p-3 select-none"
          style={{ minWidth: '220px' }}>

          <div className="flex gap-3">
            {/* Hours column */}
            <div className="flex-1">
              <p className="text-[9px] font-bold text-fg-subtle uppercase tracking-wider text-center mb-1.5">
                Hours
              </p>
              <div className="space-y-0.5">
                {HOURS.map(h => (
                  <button
                    key={h}
                    type="button"
                    onClick={() => { setSelH(h); commit(h, selM) }}
                    className={[
                      'w-full text-xs py-1 rounded-lg font-medium transition-all',
                      selH === h
                        ? 'bg-accent text-white font-bold'
                        : 'text-fg hover:bg-black/[0.06] dark:hover:bg-white/[0.06]',
                    ].join(' ')}
                  >
                    {h}h
                  </button>
                ))}
              </div>
            </div>

            {/* Divider */}
            <div className="w-px bg-border self-stretch" />

            {/* Minutes column */}
            <div className="flex-1">
              <p className="text-[9px] font-bold text-fg-subtle uppercase tracking-wider text-center mb-1.5">
                Minutes
              </p>
              <div className="space-y-0.5">
                {MINUTES.map(m => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => { setSelM(m); commit(selH, m) }}
                    className={[
                      'w-full text-xs py-1 rounded-lg font-medium transition-all',
                      selM === m
                        ? 'bg-accent text-white font-bold'
                        : 'text-fg hover:bg-black/[0.06] dark:hover:bg-white/[0.06]',
                    ].join(' ')}
                  >
                    {m}m
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Current selection preview */}
          {(selH > 0 || selM > 0) && (
            <div className="mt-2.5 pt-2 border-t border-border flex items-center justify-between">
              <span className="text-[10px] text-fg-subtle">Selected:</span>
              <span className="text-xs font-bold text-accent">{fmt(selH * 60 + selM)}</span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
