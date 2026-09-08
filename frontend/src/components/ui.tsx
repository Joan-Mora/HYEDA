import { useRef, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

export function PageHeader({ eyebrow, title, subtitle, action }: {
  eyebrow?: string
  title: string
  subtitle?: string
  action?: ReactNode
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        {eyebrow && <p className="text-xs font-semibold uppercase tracking-[0.16em] text-accent">{eyebrow}</p>}
        <h1 className="font-display text-3xl text-navy">{title}</h1>
        {subtitle && <p className="mt-1 max-w-2xl text-sm text-steel-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Panel({ title, action, children }: { title?: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-[10px] border border-slate-200 bg-white p-4 shadow-sm">
      {(title || action) && (
        <div className="mb-3 flex items-center justify-between gap-3">
          {title && <h2 className="font-display text-lg text-navy">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function KpiCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-[10px] border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wide text-steel-muted">{label}</p>
      <p className="mt-1 font-display text-2xl text-navy">{value}</p>
      {hint && <p className="mt-1 text-xs text-steel-muted">{hint}</p>}
    </div>
  )
}

export function Button({ variant = 'primary', loading, className = '', children, ...props }: {
  variant?: 'primary' | 'navy' | 'ghost' | 'danger'
  loading?: boolean
  className?: string
  children: ReactNode
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={`btn btn-${variant} ${className}`} disabled={loading || props.disabled} {...props}>
      {loading ? 'Espera…' : children}
    </button>
  )
}

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-navy">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-red-700">{error}</span>}
    </label>
  )
}

export function Input({ label, error, ...props }: { label: string; error?: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <Field label={label} error={error}>
      <input className="field" aria-invalid={Boolean(error)} {...props} />
    </Field>
  )
}

export function Select({ label, error, children, ...props }: { label: string; error?: string; children: ReactNode } & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <Field label={label} error={error}>
      <select className="field" aria-invalid={Boolean(error)} {...props}>{children}</select>
    </Field>
  )
}

export function Textarea({ label, error, ...props }: { label: string; error?: string } & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <Field label={label} error={error}>
      <textarea className="field min-h-24" aria-invalid={Boolean(error)} {...props} />
    </Field>
  )
}

export function DatePicker(props: { label: string; error?: string } & InputHTMLAttributes<HTMLInputElement>) {
  return <Input type="date" {...props} />
}

export function Badge({ tone = 'navy', children }: { tone?: 'navy' | 'accent' | 'success' | 'danger' | 'muted'; children: ReactNode }) {
  const tones = {
    navy: 'bg-navy/10 text-navy',
    accent: 'bg-orange-100 text-accent',
    success: 'bg-emerald-100 text-emerald-800',
    danger: 'bg-red-100 text-red-800',
    muted: 'bg-slate-100 text-slate-600',
  }
  return <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${tones[tone]}`}>{children}</span>
}

export function EmptyState({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <div className="py-10 text-center text-sm text-steel-muted">
      <p>{title}</p>
      {action && <div className="mt-3">{action}</div>}
    </div>
  )
}

export function Modal({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-navy/40 p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded-[10px] bg-white p-5 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-display text-xl text-navy">{title}</h3>
          <button className="btn btn-ghost" onClick={onClose}>Cerrar</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Drawer({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-navy/30" onClick={onClose}>
      <aside className="h-full w-full max-w-xl overflow-auto bg-white p-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-display text-xl text-navy">{title}</h3>
          <button className="btn btn-ghost" onClick={onClose}>Cerrar</button>
        </div>
        {children}
      </aside>
    </div>
  )
}

export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="mb-6 flex flex-wrap gap-2">
      {steps.map((step, index) => (
        <li key={step} className={`rounded-full px-3 py-1 text-xs font-semibold ${index === current ? 'bg-navy text-white' : index < current ? 'bg-orange-100 text-accent' : 'bg-slate-100 text-slate-500'}`}>
          {index + 1}. {step}
        </li>
      ))}
    </ol>
  )
}

export function FileUpload({ label, accept, onFile }: { label: string; accept?: string; onFile: (file: File) => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  return (
    <div
      className="cursor-pointer rounded-[10px] border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm text-steel-muted"
      onClick={() => inputRef.current?.click()}
      onDragOver={(e) => e.preventDefault()}
      onDrop={(e) => {
        e.preventDefault()
        const file = e.dataTransfer.files[0]
        if (file) onFile(file)
      }}
    >
      <p className="font-medium text-navy">{label}</p>
      <p>Arrastra el archivo o haz clic para elegirlo</p>
      <input ref={inputRef} type="file" accept={accept} className="hidden" onChange={(e) => {
        const file = e.target.files?.[0]
        if (file) onFile(file)
      }} />
    </div>
  )
}

export function Table({ columns, rows, empty }: {
  columns: { key: string; header: string; render?: (row: Record<string, unknown>) => ReactNode }[]
  rows: Record<string, unknown>[]
  empty?: string
}) {
  if (rows.length === 0) return <EmptyState title={empty ?? 'No hay registros'} />
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b text-xs uppercase text-steel-muted">
            {columns.map((col) => <th key={col.key} className="px-2 py-2">{col.header}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
              {columns.map((col) => (
                <td key={col.key} className="px-2 py-2">{col.render ? col.render(row) : String(row[col.key] ?? '—')}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-end gap-2 text-sm">
      <Button variant="ghost" disabled={page <= 0} onClick={() => onPage(page - 1)}>Anterior</Button>
      <span>Pagina {page + 1} de {totalPages}</span>
      <Button variant="ghost" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Siguiente</Button>
    </div>
  )
}

export function Can({ permission, children }: { permission: string; children: ReactNode }) {
  const raw = localStorage.getItem('hyeda_user')
  const user = raw ? JSON.parse(raw) as { permissions?: string[] } : null
  if (!user?.permissions?.includes(permission)) return null
  return <>{children}</>
}
