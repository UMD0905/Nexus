import { useState, useEffect, useRef } from 'react'
import { Save, HardDriveDownload, Info, FileArchive } from 'lucide-react'
import * as bridge from '../bridge'
import type { AppInfo } from '../bridge'

type SettingsMap = Record<string, string>

function useSettings() {
  const [settings, setSettings] = useState<SettingsMap>({})
  const [saved, setSaved] = useState(false)
  const saveTimer = useRef<number | null>(null)

  useEffect(() => {
    const loaded = bridge.getSettings()
    if (Object.keys(loaded).length > 0) {
      setSettings(loaded)
    } else {
      // Provide defaults for dev/no-bridge mode
      setSettings({
        default_priority:     'MEDIUM',
        default_reminder_min: '30',
        pomodoro_work_min:    '25',
        pomodoro_short_min:   '5',
        pomodoro_long_min:    '15',
        week_start_day:       'MONDAY',
        auto_backup_enabled:  'false',
        auto_backup_time:     '02:00',
        auto_backup_dir:      '',
      })
    }
  }, [])

  function save(key: string, value: string) {
    setSettings(prev => ({ ...prev, [key]: value }))
    bridge.setSetting(key, value)

    // Flash "Saved" indicator
    if (saveTimer.current) clearTimeout(saveTimer.current)
    setSaved(true)
    saveTimer.current = window.setTimeout(() => setSaved(false), 1800)
  }

  return { settings, save, saved }
}

export default function Settings() {
  const { settings, save, saved } = useSettings()
  const [appInfo, setAppInfo]         = useState<AppInfo | null>(null)
  const [exporting, setExporting]     = useState(false)
  const [exportPath, setExportPath]   = useState<string | null>(null)

  useEffect(() => {
    const info = bridge.getAppInfo()
    if (info) setAppInfo(info)
  }, [])

  const str  = (key: string) => settings[key] ?? ''
  const num  = (key: string, fallback: number) => parseInt(settings[key] ?? String(fallback), 10) || fallback
  const bool = (key: string) => settings[key] === 'true'

  return (
    <div className="flex flex-col h-full bg-canvas">
      {/* Toolbar */}
      <div className="px-6 py-3.5 border-b border-white/[0.06] bg-[#0e1524] flex items-center gap-3 shrink-0">
        <h1 className="text-lg font-bold text-fg flex-1">Settings</h1>
        {saved && (
          <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 animate-fade-in">
            <Save size={13} />
            Saved
          </span>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6 max-w-2xl">

        {/* Tasks */}
        <section className="card p-5 space-y-4">
          <h2 className="text-sm font-bold text-fg border-b border-white/[0.06] pb-2">Tasks</h2>

          <div className="flex items-center justify-between gap-4">
            <div>
              <label className="text-xs font-semibold text-fg block">Default Priority</label>
              <p className="text-[11px] text-fg-subtle mt-0.5">Priority assigned to new tasks</p>
            </div>
            <select
              className="input text-sm w-36 py-1.5"
              value={str('default_priority')}
              onChange={e => save('default_priority', e.target.value)}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div>
              <label className="text-xs font-semibold text-fg block">Default Reminder</label>
              <p className="text-[11px] text-fg-subtle mt-0.5">Minutes before due date to remind</p>
            </div>
            <input
              type="number"
              min={1}
              max={1440}
              className="input text-sm w-24 py-1.5 text-right"
              value={num('default_reminder_min', 30)}
              onChange={e => save('default_reminder_min', e.target.value)}
            />
          </div>
        </section>

        {/* Pomodoro */}
        <section className="card p-5 space-y-4">
          <h2 className="text-sm font-bold text-fg border-b border-white/[0.06] pb-2">Pomodoro</h2>

          {[
            { key: 'pomodoro_work_min',  label: 'Work Duration',  desc: 'Focus session length (minutes)', min: 1,  max: 90,  fallback: 25 },
            { key: 'pomodoro_short_min', label: 'Short Break',    desc: 'Short break length (minutes)',   min: 1,  max: 30,  fallback: 5  },
            { key: 'pomodoro_long_min',  label: 'Long Break',     desc: 'Long break length (minutes)',    min: 5,  max: 60,  fallback: 15 },
          ].map(({ key, label, desc, min, max, fallback }) => (
            <div key={key} className="flex items-center justify-between gap-4">
              <div>
                <label className="text-xs font-semibold text-fg block">{label}</label>
                <p className="text-[11px] text-fg-subtle mt-0.5">{desc}</p>
              </div>
              <input
                type="number"
                min={min}
                max={max}
                className="input text-sm w-20 py-1.5 text-right"
                value={num(key, fallback)}
                onChange={e => save(key, e.target.value)}
              />
            </div>
          ))}
        </section>

        {/* Calendar */}
        <section className="card p-5 space-y-4">
          <h2 className="text-sm font-bold text-fg border-b border-white/[0.06] pb-2">Calendar</h2>

          <div className="flex items-center justify-between gap-4">
            <div>
              <label className="text-xs font-semibold text-fg block">Week Starts On</label>
              <p className="text-[11px] text-fg-subtle mt-0.5">First day of the week in calendar views</p>
            </div>
            <select
              className="input text-sm w-36 py-1.5"
              value={str('week_start_day')}
              onChange={e => save('week_start_day', e.target.value)}
            >
              <option value="MONDAY">Monday</option>
              <option value="SUNDAY">Sunday</option>
            </select>
          </div>
        </section>

        {/* Backup */}
        <section className="card p-5 space-y-4">
          <h2 className="text-sm font-bold text-fg border-b border-white/[0.06] pb-2">Backup</h2>

          <div className="flex items-center justify-between gap-4">
            <div>
              <label className="text-xs font-semibold text-fg block">Auto-Backup</label>
              <p className="text-[11px] text-fg-subtle mt-0.5">Automatically export data each night</p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={bool('auto_backup_enabled')}
              onClick={() => save('auto_backup_enabled', bool('auto_backup_enabled') ? 'false' : 'true')}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                bool('auto_backup_enabled') ? 'bg-accent' : 'bg-white/[0.12]'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                  bool('auto_backup_enabled') ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div>
              <label className="text-xs font-semibold text-fg block">Backup Time</label>
              <p className="text-[11px] text-fg-subtle mt-0.5">Time of day to run the backup</p>
            </div>
            <input
              type="time"
              className="input text-sm w-28 py-1.5"
              value={str('auto_backup_time') || '02:00'}
              onChange={e => save('auto_backup_time', e.target.value)}
              disabled={!bool('auto_backup_enabled')}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-fg">Backup Directory</label>
            <p className="text-[11px] text-fg-subtle">Full path where backup files are saved</p>
            <div className="flex gap-2">
              <input
                type="text"
                className="input text-sm py-1.5 flex-1"
                placeholder="e.g. C:\Users\You\Documents\NexusBackups"
                value={str('auto_backup_dir')}
                onChange={e => save('auto_backup_dir', e.target.value)}
                disabled={!bool('auto_backup_enabled')}
              />
              <button
                className="btn-ghost flex items-center gap-1.5 text-xs py-1.5 px-3 shrink-0"
                onClick={() => {
                  bridge.backupNow()
                  alert('Backup triggered — check your backup directory shortly.')
                }}
                title="Run a backup right now"
              >
                <HardDriveDownload size={13} /> Backup Now
              </button>
            </div>
          </div>
        </section>

        {/* About / Diagnostics */}
        <section className="card p-5 space-y-4">
          <h2 className="text-sm font-bold text-fg border-b border-white/[0.06] pb-2 flex items-center gap-2">
            <Info size={13} className="text-accent" /> About &amp; Diagnostics
          </h2>

          {appInfo ? (
            <div className="grid grid-cols-2 gap-x-6 gap-y-2.5">
              {[
                { label: 'App Version',      value: appInfo.version       },
                { label: 'Schema Version',   value: appInfo.schemaVersion },
                { label: 'Java',             value: appInfo.java          },
                { label: 'OS',               value: appInfo.os            },
                { label: 'DB Size',          value: appInfo.dbSize        },
                { label: 'Tasks',            value: String(appInfo.taskCount)     },
                { label: 'Goals',            value: String(appInfo.goalCount)     },
                { label: 'Categories',       value: String(appInfo.categoryCount) },
              ].map(({ label, value }) => (
                <div key={label}>
                  <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider">{label}</p>
                  <p className="text-xs text-fg mt-0.5 truncate" title={value}>{value}</p>
                </div>
              ))}
              <div className="col-span-2">
                <p className="text-[10px] font-bold text-fg-subtle uppercase tracking-wider">DB Path</p>
                <p className="text-xs text-fg mt-0.5 break-all">{appInfo.dbPath}</p>
              </div>
            </div>
          ) : (
            <p className="text-xs text-fg-subtle">Loading…</p>
          )}

          <div className="pt-1 border-t border-white/[0.06] flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold text-fg">Export Diagnostics</p>
              <p className="text-[11px] text-fg-subtle mt-0.5">
                Zips the last 7 days of logs, settings keys (no values), and schema version to your Downloads folder.
                Nothing leaves your machine automatically.
              </p>
              {exportPath && (
                <p className="text-[10px] text-emerald-400 mt-1 break-all">Saved: {exportPath}</p>
              )}
            </div>
            <button
              className="btn-ghost flex items-center gap-1.5 text-xs py-1.5 px-3 shrink-0"
              disabled={exporting}
              onClick={async () => {
                setExporting(true)
                setExportPath(null)
                try {
                  const path = bridge.exportDiagnostics()
                  if (path) setExportPath(path)
                } finally {
                  setExporting(false)
                }
              }}
              title="Export diagnostics zip"
            >
              <FileArchive size={13} />
              {exporting ? 'Exporting…' : 'Export zip'}
            </button>
          </div>
        </section>

      </div>
    </div>
  )
}
