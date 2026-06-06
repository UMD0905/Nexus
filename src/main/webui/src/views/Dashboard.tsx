import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import {
  Download, Upload, RefreshCw, Flame, Snowflake, TrendingUp, Calendar,
  CheckCircle2, AlertTriangle, Timer, Activity, Plus, Minus,
  RotateCcw, BarChart2, CalendarDays, Edit2, Trash2, X,
} from 'lucide-react'
import type { DashboardStats, MonthlyStats, Streak, Category } from '../types'
import * as bridge from '../bridge'

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const PIE_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#a78bfa', '#06b6d4']

const STAT_DEFS = [
  { key: 'totalActive',       label: 'Active Tasks',    Icon: Activity,      color: '#6366f1' },
  { key: 'dueToday',          label: 'Due Today',       Icon: Calendar,      color: '#f59e0b' },
  { key: 'completedThisWeek', label: 'Done This Week',  Icon: CheckCircle2,  color: '#10b981' },
  { key: 'overdueTasks',      label: 'Overdue',         Icon: AlertTriangle, color: '#ef4444' },
  { key: 'pomodoroToday',     label: 'Pomodoros Today', Icon: Timer,         color: '#a78bfa' },
] as const

function tooltipStyle() {
  return {
    background: '#1a2235',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 8, fontSize: 12, color: '#e2e8f0',
  }
}

function StreakEditDialog({ streak, categories, onSave, onClose }: {
  streak: Streak
  categories: Category[]
  onSave: (data: Partial<Streak> & { id: number }) => void
  onClose: () => void
}) {
  const [title, setTitle]               = useState(streak.title)
  const [categoryId, setCategoryId]     = useState<number | ''>(streak.categoryId ?? '')
  const [currentStreak, setCurrentStreak] = useState(streak.currentStreak)
  const [longestStreak, setLongestStreak] = useState(streak.longestStreak)

  const handleSave = () => {
    if (!title.trim()) return
    onSave({
      id: streak.id,
      title: title.trim(),
      categoryId: categoryId === '' ? undefined : categoryId,
      currentStreak,
      longestStreak: Math.max(longestStreak, currentStreak),
    })
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.7)' }}>
      <div className="w-80 bg-surface rounded-2xl border border-white/[0.09] shadow-[0_24px_64px_rgba(0,0,0,0.6)] animate-fade-in p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-fg flex items-center gap-2">
            <Flame size={14} className="text-warning" /> Edit Streak
          </h2>
          <button onClick={onClose} className="text-fg-subtle hover:text-fg transition-colors">
            <X size={14} />
          </button>
        </div>

        <div className="space-y-3">
          <div>
            <label className="form-label">Title</label>
            <input
              className="input"
              value={title}
              onChange={e => setTitle(e.target.value)}
              autoFocus
              onKeyDown={e => e.key === 'Enter' && handleSave()}
            />
          </div>

          {categories.length > 0 && (
            <div>
              <label className="form-label">Life Area</label>
              <select className="input" value={categoryId}
                onChange={e => setCategoryId(e.target.value ? Number(e.target.value) : '')}>
                <option value="">None</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="form-label">Current streak</label>
              <input type="number" min={0} className="input text-right"
                value={currentStreak} onChange={e => setCurrentStreak(Math.max(0, Number(e.target.value)))} />
            </div>
            <div>
              <label className="form-label">Best streak</label>
              <input type="number" min={0} className="input text-right"
                value={longestStreak} onChange={e => setLongestStreak(Math.max(0, Number(e.target.value)))} />
            </div>
          </div>

          <p className="text-[10px] text-fg-subtle">
            Setting current to 0 will reset the streak's active state.
          </p>
        </div>

        <div className="flex gap-2 justify-end pt-1">
          <button className="btn-ghost text-sm" onClick={onClose}>Cancel</button>
          <button className="btn-primary text-sm" onClick={handleSave}
            disabled={!title.trim()}>Save</button>
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const [stats, setStats]           = useState<DashboardStats | null>(null)
  const [monthly, setMonthly]       = useState<MonthlyStats[]>([])
  const [showMonthly, setShowMonthly] = useState(true)
  const [editStreak, setEditStreak] = useState<Streak | null>(null)

  // categories aren't in DashboardStats — pull them from bridge directly
  const [categories, setCategories] = useState<Category[]>([])

  const load = () => {
    setStats(bridge.getDashboardStats())
    setMonthly(bridge.getMonthlyStats())
    setCategories(bridge.getCategories())
  }

  useEffect(() => { load() }, [])

  if (!stats) return (
    <div className="flex items-center justify-center h-full text-fg-muted text-sm">Loading…</div>
  )

  const barData = DAYS.map((d, i) => ({ day: d, completed: stats.weeklyCompletions[i] ?? 0 }))
  const pieData = Object.entries(stats.categoryBreakdown).map(([name, value]) => ({ name, value }))
  const todayIdx = new Date().getDay() === 0 ? 6 : new Date().getDay() - 1

  const handleAdjust = (key: string, delta: number) => {
    bridge.adjustStat(key, delta)
    load()
  }

  const handleReset = () => {
    if (confirm('Clear all manual stat adjustments? Stats will be recalculated from your actual task data.')) {
      bridge.resetStatAdjustments()
      load()
    }
  }

  // Find the best month for the "most productive" highlight
  const bestMonthIdx = monthly.length > 0
    ? monthly.reduce((best, m, i, arr) => m.completed > arr[best].completed ? i : best, 0)
    : -1

  return (
    <>
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Dashboard</h1>
        <button onClick={handleReset} title="Clear manual stat adjustments"
          className="btn-ghost flex items-center gap-1.5 text-xs py-1.5 text-fg-subtle hover:text-danger">
          <RotateCcw size={13} /> Reset stats
        </button>
        <button onClick={load} className="btn-ghost flex items-center gap-1.5 text-xs py-1.5">
          <RefreshCw size={13} /> Refresh
        </button>
        <button className="btn-ghost flex items-center gap-1.5 py-1.5 text-xs"
          onClick={() => {
            const p = bridge.chooseFile('Select backup file', 'json')
            if (p) {
              const r = bridge.importData(p)
              if (r) {
                const errLine = r.errors?.length ? `\n\nFirst errors:\n${r.errors.slice(0, 5).join('\n')}` : ''
                alert(`Import complete — ${r.imported} imported, ${r.skipped} skipped.${errLine}`)
              }
              load()
            }
          }}>
          <Upload size={13} /> Import JSON
        </button>
        <button className="btn-ghost flex items-center gap-1.5 py-1.5 text-xs"
          onClick={() => { const p = bridge.chooseFolder('Export iCal to folder'); if (p) bridge.exportIcal(p) }}>
          <CalendarDays size={13} /> Export iCal
        </button>
        <button className="btn-primary flex items-center gap-1.5 py-1.5 text-xs"
          onClick={() => { const p = bridge.chooseFolder('Export to folder'); if (p) bridge.exportData(p) }}>
          <Download size={13} /> Export JSON
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">

        {/* ── Focus time card ────────────────────────────────────────────── */}
        {stats.focusTimeThisWeek > 0 && (
          <div className="card px-5 py-4 flex items-center gap-4 border-l-[3px]" style={{ borderLeftColor: '#06b6d4' }}>
            <Timer size={22} style={{ color: '#06b6d4' }} className="shrink-0 opacity-80" />
            <div>
              <p className="text-2xl font-bold text-fg">
                {Math.floor(stats.focusTimeThisWeek / 60)}h {stats.focusTimeThisWeek % 60}m
              </p>
              <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider mt-0.5">
                Focus Time This Week
              </p>
            </div>
          </div>
        )}

        {/* ── Stat cards with manual +/- ────────────────────────────────── */}
        <div className="grid grid-cols-5 gap-3">
          {STAT_DEFS.map(({ key, label, Icon, color }) => {
            const val = stats[key as keyof typeof stats] as number
            const adj = stats.statAdjustments?.[key] ?? 0
            return (
              <div key={key} className="card px-4 py-4 relative overflow-hidden group"
                style={{ borderLeft: `3px solid ${color}` }}>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="text-3xl font-bold text-fg">
                      {val}
                    </p>
                    <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider mt-1">{label}</p>
                    {adj !== 0 && (
                      <p className="text-[9px] mt-0.5" style={{ color: adj > 0 ? '#10b981' : '#ef4444' }}>
                        {adj > 0 ? `+${adj}` : adj} manual
                      </p>
                    )}
                  </div>
                  <Icon size={18} style={{ color }} className="opacity-60 mt-0.5" />
                </div>

                {/* Manual adjustment buttons — shown on hover */}
                <div className="absolute bottom-2 right-2 flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={() => handleAdjust(key, -1)}
                    className="p-0.5 rounded hover:bg-white/10 text-fg-subtle hover:text-danger transition-colors"
                    title="Decrease by 1">
                    <Minus size={10} />
                  </button>
                  <button onClick={() => handleAdjust(key, 1)}
                    className="p-0.5 rounded hover:bg-white/10 text-fg-subtle hover:text-success transition-colors"
                    title="Increase by 1">
                    <Plus size={10} />
                  </button>
                </div>

                <div className="absolute bottom-0 left-0 right-0 h-[2px] opacity-30"
                  style={{ background: `linear-gradient(to right, ${color}, transparent)` }} />
              </div>
            )
          })}
        </div>

        {/* ── Charts row ─────────────────────────────────────────────────── */}
        <div className="grid grid-cols-2 gap-5">
          {/* Weekly bar */}
          <div className="card p-5">
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-4 flex items-center gap-2">
              <TrendingUp size={13} className="text-accent" /> Completions This Week
            </h3>
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={barData} barSize={22}>
                <XAxis dataKey="day" tick={{ fill: '#6b7280', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#6b7280', fontSize: 10 }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip contentStyle={tooltipStyle()} cursor={{ fill: 'rgba(255,255,255,0.04)' }} />
                <Bar dataKey="completed" radius={[4, 4, 0, 0]} isAnimationActive={false}>
                  {barData.map((_, i) => (
                    <Cell key={i} fill={i === todayIdx ? '#10b981' : '#6366f1'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Category pie */}
          <div className="card p-5">
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-4">
              Tasks by Life Area
            </h3>
            {pieData.length === 0 ? (
              <div className="flex items-center justify-center h-44 text-fg-subtle text-sm">No data yet</div>
            ) : (
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={72}
                    paddingAngle={3} dataKey="value" isAnimationActive={false}>
                    {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} stroke="none" />)}
                  </Pie>
                  <Tooltip contentStyle={tooltipStyle()} />
                  <Legend wrapperStyle={{ fontSize: 11, color: '#94a3b8' }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* ── Monthly overview ────────────────────────────────────────────── */}
        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <BarChart2 size={13} className="text-accent" />
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider flex-1">
              Monthly Overview — Last 12 Months
            </h3>
            <button onClick={() => setShowMonthly(v => !v)}
              className="text-[10px] text-fg-subtle hover:text-fg transition-colors">
              {showMonthly ? 'Hide' : 'Show'}
            </button>
          </div>

          {showMonthly && (
            <>
              {/* Best month badge */}
              {bestMonthIdx >= 0 && monthly[bestMonthIdx].completed > 0 && (
                <p className="text-[10px] text-fg-subtle mb-3">
                  Most productive:&nbsp;
                  <span className="text-success font-semibold">{monthly[bestMonthIdx].monthName}</span>
                  &nbsp;— {monthly[bestMonthIdx].completed} tasks completed
                </p>
              )}

              <ResponsiveContainer width="100%" height={180}>
                <BarChart data={monthly} barSize={18}>
                  <XAxis dataKey="monthName" tick={{ fill: '#6b7280', fontSize: 9 }}
                    axisLine={false} tickLine={false} interval={0}
                    tickFormatter={v => v.split(' ')[0]} />
                  <YAxis tick={{ fill: '#6b7280', fontSize: 10 }} axisLine={false}
                    tickLine={false} allowDecimals={false} />
                  <Tooltip contentStyle={tooltipStyle()} formatter={(v) => [v ?? 0, 'Completed']} />
                  <Bar dataKey="completed" radius={[4, 4, 0, 0]} isAnimationActive={false}>
                    {monthly.map((_, i) => (
                      <Cell key={i} fill={i === bestMonthIdx ? '#10b981' : '#6366f1'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </>
          )}
        </div>

        {/* ── Streaks ─────────────────────────────────────────────────────── */}
        {stats.streaks.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-3 flex items-center gap-2">
              <Flame size={13} className="text-warning" /> Streaks
            </h3>
            <div className="flex gap-4 flex-wrap">
              {stats.streaks.map(s => {
                const active = s.active
                return (
                  <div key={s.id}
                    className="card px-5 py-4 w-44 relative group"
                    style={{
                      borderLeft: `3px solid ${active ? '#f59e0b' : '#374151'}`,
                    }}>

                    {/* Edit / Delete — appear on hover */}
                    <div className="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => setEditStreak(s)}
                        title="Edit streak"
                        className="p-1 rounded-md hover:bg-white/[0.08] text-fg-subtle hover:text-fg transition-all"
                      >
                        <Edit2 size={11} />
                      </button>
                      <button
                        onClick={() => {
                          if (confirm(`Delete streak "${s.title}"? This cannot be undone.`)) {
                            bridge.deleteStreak(s.id)
                            load()
                          }
                        }}
                        title="Delete streak"
                        className="p-1 rounded-md hover:bg-white/[0.08] text-fg-subtle hover:text-danger transition-all"
                      >
                        <Trash2 size={11} />
                      </button>
                    </div>

                    <div className="flex items-center gap-2 mb-2">
                      {active
                        ? <Flame size={16} className="text-warning" />
                        : <Snowflake size={16} className="text-fg-subtle" />}
                      <span className="text-xs font-semibold text-fg truncate pr-8">{s.title}</span>
                    </div>
                    <p className="text-2xl font-bold" style={{
                      color: active ? '#f59e0b' : '#4b5563',
                    }}>
                      {s.currentStreak}d
                    </p>
                    <p className="text-[10px] text-fg-subtle mt-0.5">Best: {s.longestStreak}d</p>
                    {s.lastCompletedDate && (
                      <p className="text-[10px] text-fg-subtle">Last: {s.lastCompletedDate}</p>
                    )}
                    {s.category && (
                      <p className="text-[10px] mt-1 truncate" style={{ color: s.category.color }}>
                        {s.category.name}
                      </p>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )}

      </div>
    </div>

    {editStreak && (
      <StreakEditDialog
        streak={editStreak}
        categories={categories}
        onSave={data => { bridge.updateStreak(data); load() }}
        onClose={() => setEditStreak(null)}
      />
    )}
    </>
  )
}
