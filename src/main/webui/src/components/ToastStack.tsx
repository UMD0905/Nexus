import type { Toast } from './useToast'
import { X } from 'lucide-react'

interface Props {
  toasts: Toast[]
  onDismiss: (id: number) => void
}

const BG: Record<Toast['type'], string> = {
  info:    'bg-[#1a2235] border-white/[0.1] text-fg',
  success: 'bg-emerald-900/40 border-emerald-500/30 text-emerald-300',
  danger:  'bg-rose-900/40 border-rose-500/30 text-rose-300',
}

export default function ToastStack({ toasts, onDismiss }: Props) {
  if (toasts.length === 0) return null

  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 items-center pointer-events-none">
      {toasts.map(t => (
        <div
          key={t.id}
          className={`pointer-events-auto flex items-center gap-3 px-4 py-2.5 rounded-xl border shadow-lg animate-fade-in text-xs font-semibold whitespace-nowrap ${BG[t.type]}`}
        >
          <span>{t.message}</span>
          {t.onUndo && (
            <button
              onClick={t.onUndo}
              className="ml-1 px-2 py-0.5 rounded bg-white/[0.12] hover:bg-white/[0.2] transition-colors text-[11px] font-bold"
            >
              Undo
            </button>
          )}
          <button
            onClick={() => { t.onUndo?.(); onDismiss(t.id) }}
            className="text-current opacity-50 hover:opacity-100 transition-opacity"
          >
            <X size={13} />
          </button>
        </div>
      ))}
    </div>
  )
}
