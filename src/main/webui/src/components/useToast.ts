import { useState, useCallback } from 'react'

export interface Toast {
  id: number
  message: string
  type: 'info' | 'success' | 'danger'
  /** If present, clicking Undo cancels the pending action. */
  onUndo?: () => void
  /** Timeout handle so we can clear it on undo. */
  timeoutId?: number
}

let nextId = 1

export function useToast() {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  /**
   * Shows an info/success toast that auto-dismisses after `ms` milliseconds.
   */
  const show = useCallback((message: string, type: Toast['type'] = 'info', ms = 3000) => {
    const id = nextId++
    const timeoutId = window.setTimeout(() => dismiss(id), ms)
    setToasts(prev => [...prev, { id, message, type, timeoutId }])
  }, [dismiss])

  /**
   * Shows a toast with an Undo button.  The `action` fires after `delayMs`
   * (default 5 s) unless the user clicks Undo first.
   * Returns the toast id so callers can dismiss it early if needed.
   */
  const showUndoable = useCallback((
    message: string,
    action: () => void,
    delayMs = 5000,
  ): number => {
    const id = nextId++
    const timeoutId = window.setTimeout(() => {
      action()
      dismiss(id)
    }, delayMs)

    setToasts(prev => [...prev, {
      id,
      message,
      type: 'danger',
      timeoutId,
      onUndo: () => {
        clearTimeout(timeoutId)
        dismiss(id)
      },
    }])

    return id
  }, [dismiss])

  return { toasts, show, showUndoable, dismiss }
}
