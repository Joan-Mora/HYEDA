import { AnimatePresence, motion } from 'framer-motion'
import type { ReactNode } from 'react'

export function SlideOver({
  open,
  onClose,
  title,
  subtitle,
  children,
  width = 440,
}: {
  open: boolean
  onClose: () => void
  title?: string
  subtitle?: string
  children: ReactNode
  width?: number
}) {
  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[60] flex justify-end" role="dialog" aria-modal="true">
          <motion.button
            type="button"
            aria-label="Cerrar"
            className="absolute inset-0 bg-[rgba(20,24,51,0.45)] backdrop-blur-[3px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.aside
            className="relative h-full bg-[var(--white)] shadow-[-12px_0_40px_rgba(20,24,51,0.18)] border-l border-[rgba(0,0,0,0.08)] flex flex-col"
            style={{ width: `min(100vw, ${width}px)` }}
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', stiffness: 380, damping: 36 }}
          >
            <header className="px-5 py-4 border-b border-[var(--border)] flex items-start justify-between gap-3 shrink-0">
              <div className="min-w-0">
                {subtitle && (
                  <p className="text-[0.65rem] font-bold uppercase tracking-[0.14em] text-[var(--accent)]">
                    {subtitle}
                  </p>
                )}
                {title && (
                  <h2 className="font-display text-2xl text-[var(--navy)] uppercase leading-tight mt-0.5 truncate">
                    {title}
                  </h2>
                )}
              </div>
              <button type="button" className="btn btn-ghost !py-1.5 !px-2.5" onClick={onClose}>
                ✕
              </button>
            </header>
            <div className="flex-1 overflow-y-auto">{children}</div>
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  )
}
