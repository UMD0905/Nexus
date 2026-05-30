import { useState, useEffect, useRef } from 'react'
import { Play, Pause, RotateCcw, SkipForward, Coffee, Brain, Volume2, VolumeX, Link } from 'lucide-react'
import * as bridge from '../bridge'
import type { Task } from '../types'

type Phase = 'work' | 'short' | 'long'

const PHASE_META: Record<Phase, { label: string; color: string; Icon: React.ElementType }> = {
  work:  { label: 'Focus',       color: '#6366f1', Icon: Brain   },
  short: { label: 'Short Break', color: '#10b981', Icon: Coffee  },
  long:  { label: 'Long Break',  color: '#06b6d4', Icon: Coffee  },
}

function pad(n: number) { return String(n).padStart(2, '0') }

function playAlarm(type: 'work' | 'break') {
  try { (window as any).nexusBridge?.planning?.playAlarm(type) }
  catch { /* bridge not available in browser preview */ }
}

export default function Pomodoro() {
  const [phase, setPhase]         = useState<Phase>('work')
  const [seconds, setSeconds]     = useState(25 * 60)
  const [running, setRunning]     = useState(false)
  const [session, setSession]     = useState(0)
  const [target, setTarget]       = useState(25)
  const [shortBrk, setShortBrk]   = useState(5)
  const [longBrk, setLongBrk]     = useState(15)
  const [muted, setMuted]         = useState(false)
  const [tasks, setTasks]         = useState<Task[]>([])
  const [linkedTaskId, setLinkedTaskId] = useState<number>(0)
  const [sessionId, setSessionId] = useState<number>(0)

  const intervalRef = useRef<number | null>(null)
  const mutedRef    = useRef(false)

  useEffect(() => { mutedRef.current = muted }, [muted])

  // Load settings + tasks on mount
  useEffect(() => {
    const s = bridge.getSettings()
    const workMin  = parseInt(s['pomodoro_work_min']  ?? '25', 10) || 25
    const shortMin = parseInt(s['pomodoro_short_min'] ?? '5',  10) || 5
    const longMin  = parseInt(s['pomodoro_long_min']  ?? '15', 10) || 15
    setTarget(workMin)
    setShortBrk(shortMin)
    setLongBrk(longMin)
    setSeconds(workMin * 60)

    // Load active tasks for picker
    const activeTasks = bridge.getTasks({ status: 'TODO' })
      .concat(bridge.getTasks({ status: 'IN_PROGRESS' }))
    setTasks(activeTasks)
  }, [])

  const total = phase === 'work' ? target * 60 : phase === 'short' ? shortBrk * 60 : longBrk * 60
  const pct = ((total - seconds) / total) * 100
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60

  const startSession = () => {
    if (phase === 'work') {
      const id = bridge.startPomodoroSession(linkedTaskId, target)
      setSessionId(id)
    }
    setRunning(true)
  }

  const pauseSession = () => {
    setRunning(false)
  }

  useEffect(() => {
    if (running) {
      intervalRef.current = window.setInterval(() => {
        setSeconds(s => {
          if (s <= 1) {
            clearInterval(intervalRef.current!)
            setRunning(false)
            if (phase === 'work') {
              if (sessionId > 0) bridge.completePomodoroSession(sessionId)
              setSessionId(0)
              setSession(n => n + 1)
              if (!mutedRef.current) playAlarm('work')
            } else {
              if (!mutedRef.current) playAlarm('break')
            }
            return 0
          }
          return s - 1
        })
      }, 1000)
    } else {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  }, [running, phase, sessionId])

  const switchPhase = (p: Phase) => {
    if (running && sessionId > 0) {
      bridge.abandonPomodoroSession(sessionId)
      setSessionId(0)
    }
    setRunning(false)
    setPhase(p)
    setSeconds(p === 'work' ? target * 60 : p === 'short' ? shortBrk * 60 : longBrk * 60)
  }

  const reset = () => {
    if (running && sessionId > 0) {
      bridge.abandonPomodoroSession(sessionId)
      setSessionId(0)
    }
    setRunning(false)
    setSeconds(total)
  }

  const skip = () => {
    const next: Phase = phase === 'work'
      ? session > 0 && session % 4 === 3 ? 'long' : 'short'
      : 'work'
    switchPhase(next)
  }

  const { color, Icon } = PHASE_META[phase]
  const r = 90
  const circumference = 2 * Math.PI * r
  const dashOffset = circumference * (1 - pct / 100)

  const linkedTask = tasks.find(t => t.id === linkedTaskId)

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Pomodoro</h1>
        <span className="text-xs text-fg-subtle">{session} sessions today</span>
        <button
          onClick={() => setMuted(m => !m)}
          title={muted ? 'Unmute alarm' : 'Mute alarm'}
          className="p-1.5 rounded hover:bg-white/[0.08] transition-colors">
          {muted
            ? <VolumeX size={16} className="text-fg-subtle" />
            : <Volume2 size={16} className="text-fg-subtle" />}
        </button>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Main timer */}
        <div className="flex-1 flex flex-col items-center justify-center gap-8 p-8">
          {/* Phase tabs */}
          <div className="flex gap-2">
            {(['work', 'short', 'long'] as Phase[]).map(p => (
              <button key={p}
                className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-all ${
                  phase === p ? 'text-white shadow-lg' : 'bg-white/[0.05] text-fg-subtle hover:text-fg'
                }`}
                style={phase === p ? { background: PHASE_META[p].color } : {}}
                onClick={() => switchPhase(p)}>
                {PHASE_META[p].label}
              </button>
            ))}
          </div>

          {/* Task picker */}
          {phase === 'work' && (
            <div className="flex items-center gap-2 w-full max-w-xs">
              <Link size={13} className="text-fg-subtle shrink-0" />
              <select
                className="input text-xs py-1.5 flex-1"
                value={linkedTaskId}
                onChange={e => setLinkedTaskId(Number(e.target.value))}
                disabled={running}
              >
                <option value={0}>— No task (free focus) —</option>
                {tasks.map(t => (
                  <option key={t.id} value={t.id}>{t.title}</option>
                ))}
              </select>
            </div>
          )}

          {/* Circular timer */}
          <div className="relative">
            <svg width="220" height="220" className="-rotate-90">
              <circle cx="110" cy="110" r={r} fill="none" style={{ stroke: 'var(--color-track)' }} strokeWidth="8" />
              <circle
                cx="110" cy="110" r={r}
                fill="none"
                stroke={color}
                strokeWidth="8"
                strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={dashOffset}
                style={{ transition: 'stroke-dashoffset 0.5s ease', filter: `drop-shadow(0 0 12px ${color}80)` }}
              />
            </svg>

            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <Icon size={20} style={{ color }} className="mb-1 opacity-80" />
              <span className="text-5xl font-bold text-fg tabular-nums" style={{ textShadow: `0 0 40px ${color}40` }}>
                {pad(mins)}:{pad(secs)}
              </span>
              <span className="text-xs text-fg-subtle mt-1">{PHASE_META[phase].label}</span>
              {linkedTask && phase === 'work' && (
                <span className="text-[10px] text-accent/70 mt-1 max-w-[140px] truncate text-center">
                  🍅 {linkedTask.title}
                </span>
              )}
            </div>
          </div>

          {/* Controls */}
          <div className="flex items-center gap-4">
            <button onClick={reset}
              className="btn-ghost p-3 rounded-full hover:bg-white/[0.08]">
              <RotateCcw size={18} className="text-fg-subtle" />
            </button>

            <button
              onClick={() => running ? pauseSession() : startSession()}
              className="w-16 h-16 rounded-full flex items-center justify-center text-white font-bold shadow-lg transition-all hover:scale-105 active:scale-95"
              style={{ background: color, boxShadow: `0 0 32px ${color}60` }}>
              {running ? <Pause size={22} /> : <Play size={22} className="ml-0.5" />}
            </button>

            <button onClick={skip}
              className="btn-ghost p-3 rounded-full hover:bg-white/[0.08]">
              <SkipForward size={18} className="text-fg-subtle" />
            </button>
          </div>

          {/* Dot indicators (4 per cycle) */}
          <div className="flex gap-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i}
                className="w-2 h-2 rounded-full transition-all"
                style={{ background: i < (session % 4) ? color : 'var(--color-inactive)' }}
              />
            ))}
          </div>
        </div>

        {/* Settings panel */}
        <div className="w-64 border-l border-white/[0.06] bg-[#0a1020] shrink-0 p-5 space-y-5">
          <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider">Timer Settings</p>

          {[
            { label: 'Focus (min)',       val: target,   set: setTarget,   min: 1, max: 60 },
            { label: 'Short Break (min)', val: shortBrk, set: setShortBrk, min: 1, max: 30 },
            { label: 'Long Break (min)',  val: longBrk,  set: setLongBrk,  min: 5, max: 60 },
          ].map(({ label, val, set, min, max }) => (
            <div key={label}>
              <label className="text-[10px] text-fg-subtle block mb-1.5">{label}</label>
              <div className="flex items-center gap-2">
                <input
                  type="range" min={min} max={max} value={val}
                  onChange={e => { set(Number(e.target.value)); if (!running) reset() }}
                  className="flex-1 accent-accent h-1"
                />
                <span className="text-xs text-fg w-6 text-right font-mono">{val}</span>
              </div>
            </div>
          ))}

          <div className="pt-4 border-t border-white/[0.05]">
            <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider mb-3">Session Stats</p>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-fg-subtle">Completed today</span>
                <span className="text-xs font-bold text-fg">{session}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-fg-subtle">Focus time</span>
                <span className="text-xs font-bold text-fg">{session * target}m</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-fg-subtle">Until long break</span>
                <span className="text-xs font-bold" style={{ color }}>
                  {4 - (session % 4)} left
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
