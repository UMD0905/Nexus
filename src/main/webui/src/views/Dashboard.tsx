import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import { Download, RefreshCw, Flame, Snowflake, TrendingUp, Calendar, CheckCircle2, AlertTriangle, Timer, Activity } from 'lucide-react'
import type { DashboardStats } from '../types'
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
  const dark = document.documentElement.classList.contains('dark')
  return {
    background: dark ? '#1a2235' : '#ffffff',
    border: `1px solid ${dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.1)'}`,
    borderRadius: 8,
    fontSize: 12,
    color: dark ? '#e2e8f0' : '#0f172a',
  }
}

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null)

  const load = () => setStats(bridge.getDashboardStats())
  useEffect(() => { load() }, [])

  if (!stats) return <div className="flex items-center justify-center h-full text-fg-muted text-sm">Loading…</div>

  const barData = DAYS.map((d, i) => ({ day: d, completed: stats.weeklyCompletions[i] ?? 0, isToday: i === new Date().getDay() - 1 }))
  const pieData = Object.entries(stats.categoryBreakdown).map(([name, value]) => ({ name, value }))
  const todayIdx = new Date().getDay() === 0 ? 6 : new Date().getDay() - 1

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Dashboard</h1>
        <button onClick={load} className="btn-ghost flex items-center gap-1.5 text-xs py-1.5">
          <RefreshCw size={13} /> Refresh
        </button>
        <button className="btn-primary flex items-center gap-1.5 py-1.5 text-xs"
          onClick={() => { const p = prompt('Export to folder:'); if (p) bridge.exportData(p) }}>
          <Download size={13} /> Export JSON
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">
        {/* Stat cards */}
        <div className="grid grid-cols-5 gap-3">
          {STAT_DEFS.map(({ key, label, Icon, color }) => (
            <div key={key} className="card px-4 py-4 relative overflow-hidden animate-fade-in"
              style={{ borderLeft: `3px solid ${color}` }}>
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-3xl font-bold text-fg" style={{ textShadow: `0 0 20px ${color}40` }}>
                    {stats[key as keyof typeof stats] as number}
                  </p>
                  <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider mt-1">{label}</p>
                </div>
                <Icon size={18} style={{ color }} className="opacity-60 mt-0.5" />
              </div>
              <div className="absolute bottom-0 left-0 right-0 h-[2px] opacity-30" style={{ background: `linear-gradient(to right, ${color}, transparent)` }} />
            </div>
          ))}
        </div>

        {/* Charts */}
        <div className="grid grid-cols-2 gap-5">
          {/* Weekly bar chart */}
          <div className="card p-5">
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-4 flex items-center gap-2">
              <TrendingUp size={13} className="text-accent" /> Completions This Week
            </h3>
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={barData} barSize={22}>
                <XAxis dataKey="day" tick={{ fill: '#6b7280', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#6b7280', fontSize: 10 }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip
                  contentStyle={tooltipStyle()}
                  cursor={{ fill: document.documentElement.classList.contains('dark') ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)' }}
                />
                <Bar dataKey="completed" radius={[4, 4, 0, 0]}>
                  {barData.map((_entry, i) => (
                    <Cell key={i} fill={i === todayIdx ? '#10b981' : '#6366f1'}
                      style={{ filter: i === todayIdx ? 'drop-shadow(0 0 8px rgba(16,185,129,0.5))' : 'drop-shadow(0 0 4px rgba(99,102,241,0.3))' }} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Pie chart */}
          <div className="card p-5">
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-4">Tasks by Life Area</h3>
            {pieData.length === 0 ? (
              <div className="flex items-center justify-center h-44 text-fg-subtle text-sm">No data yet</div>
            ) : (
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={72} paddingAngle={3} dataKey="value">
                    {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} stroke="none" />)}
                  </Pie>
                  <Tooltip contentStyle={{ background: '#1a2235', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 8, fontSize: 12 }} />
                  <Legend wrapperStyle={{ fontSize: 11, color: '#94a3b8' }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Streaks */}
        {stats.streaks.length > 0 && (
          <div>
            <h3 className="text-xs font-bold text-fg-subtle uppercase tracking-wider mb-3 flex items-center gap-2">
              <Flame size={13} className="text-warning" /> Streaks
            </h3>
            <div className="flex gap-4 flex-wrap">
              {stats.streaks.map(s => {
                const active = s.active
                return (
                  <div key={s.id} className="card px-5 py-4 w-44 animate-fade-in"
                    style={{ borderLeft: `3px solid ${active ? '#f59e0b' : '#374151'}`,
                             boxShadow: active ? '0 4px 24px rgba(245,158,11,0.12)' : undefined }}>
                    <div className="flex items-center gap-2 mb-2">
                      {active ? <Flame size={16} className="text-warning" /> : <Snowflake size={16} className="text-fg-subtle" />}
                      <span className="text-xs font-semibold text-fg truncate">{s.title}</span>
                    </div>
                    <p className="text-2xl font-bold" style={{ color: active ? '#f59e0b' : '#4b5563',
                      textShadow: active ? '0 0 16px rgba(245,158,11,0.4)' : undefined }}>
                      {s.currentStreak}d
                    </p>
                    <p className="text-[10px] text-fg-subtle mt-0.5">Best: {s.longestStreak}d</p>
                    {s.lastCompletedDate && <p className="text-[10px] text-fg-subtle">Last: {s.lastCompletedDate}</p>}
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
