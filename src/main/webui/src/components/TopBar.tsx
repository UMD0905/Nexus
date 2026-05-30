import { useRef } from 'react'
import { Bell, Sun, Moon, Minus, Square, X, Zap } from 'lucide-react'
import type { Notification } from '../types'
import * as bridge from '../bridge'

interface Props {
  notifications: Notification[]
  onBellClick: () => void
  isDark: boolean
  onToggleTheme: () => void
  onQuickAdd?: () => void
}

export default function TopBar({ notifications, onBellClick, isDark, onToggleTheme, onQuickAdd }: Props) {
  const unread = notifications.filter(n => !n.read).length
  const dragging = useRef(false)

  const handleDragStart = (e: React.MouseEvent) => {
    // Only left button, not on a button/input
    if (e.button !== 0) return
    if ((e.target as HTMLElement).closest('button,input,select')) return
    dragging.current = true
    bridge.startDrag(e.screenX, e.screenY)
    e.preventDefault()
  }

  const handleDragMove = (e: React.MouseEvent) => {
    if (!dragging.current) return
    bridge.dragWindow(e.screenX, e.screenY)
  }

  const handleDragEnd = () => { dragging.current = false }

  return (
    <header className="h-11 flex items-center shrink-0 border-b border-white/[0.06] select-none"
      style={{ background: isDark ? '#0a1020' : '#f8fafc' }}>

      {/* App name — fills space and acts as drag region */}
      <div
        className="flex items-center gap-2.5 px-5 flex-1 cursor-grab active:cursor-grabbing h-full"
        onMouseDown={handleDragStart}
        onMouseMove={handleDragMove}
        onMouseUp={handleDragEnd}
        onMouseLeave={handleDragEnd}
        onDoubleClick={() => bridge.toggleMaximize()}
      >
        <span className="text-[13px] font-bold text-accent" style={{ textShadow: isDark ? '0 0 16px rgba(99,102,241,0.5)' : 'none' }}>
          Nexus
        </span>
        <span className="text-[11px] text-fg-subtle opacity-50">Productivity Hub</span>
      </div>

      {/* Right controls */}
      <div className="flex items-center">
        {/* Quick Add */}
        {onQuickAdd && (
          <button onClick={onQuickAdd}
            className="flex items-center gap-1.5 px-3 h-7 mr-2 rounded-lg bg-accent/20 hover:bg-accent/30 transition-colors text-accent text-xs font-semibold"
            title="Quick add task (Ctrl+N)">
            <Zap size={12} /> Quick Add
          </button>
        )}
        {/* Bell */}
        <button onClick={onBellClick}
          className="relative w-9 h-9 flex items-center justify-center hover:bg-white/[0.06] transition-colors rounded-lg mx-1">
          <Bell size={15} className="text-fg-subtle" />
          {unread > 0 && (
            <span className="absolute top-1.5 right-1.5 w-3.5 h-3.5 bg-accent text-white text-[8px] font-bold rounded-full flex items-center justify-center">
              {unread > 9 ? '9+' : unread}
            </span>
          )}
        </button>

        {/* Theme toggle */}
        <button onClick={onToggleTheme}
          className="w-9 h-9 flex items-center justify-center hover:bg-white/[0.06] transition-colors rounded-lg"
          title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}>
          {isDark ? <Sun size={14} className="text-fg-subtle" /> : <Moon size={14} className="text-fg-subtle" />}
        </button>

        {/* Divider */}
        <div className="w-px h-5 bg-white/[0.08] mx-2" />

        {/* Window controls */}
        <button onClick={() => bridge.minimizeWindow()}
          className="w-9 h-11 flex items-center justify-center hover:bg-white/[0.06] transition-colors"
          title="Minimize">
          <Minus size={13} className="text-fg-subtle" />
        </button>
        <button onClick={() => bridge.maximizeWindow()}
          className="w-9 h-11 flex items-center justify-center hover:bg-white/[0.06] transition-colors"
          title="Maximize / Restore">
          <Square size={11} className="text-fg-subtle" />
        </button>
        <button onClick={() => bridge.closeWindow()}
          className="w-9 h-11 flex items-center justify-center hover:bg-red-500 hover:text-white transition-colors group"
          title="Close">
          <X size={14} className="text-fg-subtle group-hover:text-white" />
        </button>
      </div>
    </header>
  )
}
