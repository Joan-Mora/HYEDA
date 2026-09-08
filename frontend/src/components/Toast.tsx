import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

type ToastContextValue = { push: (message: string, tone?: 'ok' | 'error') => void }

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<{ id: number; message: string; tone: 'ok' | 'error' }[]>([])
  const value = useMemo<ToastContextValue>(() => ({
    push(message, tone = 'ok') {
      const id = Date.now()
      setToasts((current) => [...current, { id, message, tone }])
      window.setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), 4000)
    },
  }), [])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 space-y-2">
        {toasts.map((toast) => (
          <div key={toast.id} className={`rounded-[10px] px-4 py-2 text-sm text-white shadow ${toast.tone === 'error' ? 'bg-red-700' : 'bg-navy'}`}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('ToastProvider requerido')
  return ctx
}
