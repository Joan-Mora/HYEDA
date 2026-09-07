import type { ButtonHTMLAttributes, PropsWithChildren, ReactNode } from 'react'
import { Link } from 'react-router-dom'

export function PageHeader({
  title,
  subtitle,
  eyebrow,
  actions,
}: {
  title: string
  subtitle?: string
  eyebrow?: string
  actions?: ReactNode
}) {
  return (
    <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between mb-6">
      <div>
        {eyebrow && (
          <p className="text-[0.65rem] font-bold uppercase tracking-[0.16em] text-[var(--accent)] mb-1">
            {eyebrow}
          </p>
        )}
        <h1 className="font-display text-3xl md:text-4xl text-[var(--navy)] uppercase tracking-wide leading-none">
          {title}
        </h1>
        {subtitle && <p className="mt-2 text-sm text-[var(--text-muted)] max-w-2xl leading-relaxed">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  )
}

export function ModuleTabs({
  tabs,
  value,
  onChange,
}: {
  tabs: { id: string; label: string }[]
  value: string
  onChange: (id: string) => void
}) {
  return (
    <div className="module-tabs" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          aria-selected={value === t.id}
          className={['module-tab', value === t.id ? 'module-tab-active' : ''].join(' ')}
          onClick={() => onChange(t.id)}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}

export function Panel({
  children,
  className = '',
  title,
  action,
}: PropsWithChildren<{ className?: string; title?: string; action?: ReactNode }>) {
  return (
    <section className={`panel overflow-hidden ${className}`}>
      {(title || action) && (
        <div className="flex items-center justify-between gap-3 px-5 py-3.5 border-b border-[var(--border)]">
          {title ? (
            <h2 className="font-display text-lg text-[var(--navy)] uppercase tracking-wide">{title}</h2>
          ) : (
            <span />
          )}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function KpiCard({
  label,
  value,
  hint,
  accent = false,
}: {
  label: string
  value: string
  hint?: string
  accent?: boolean
}) {
  return (
    <div className="panel p-5 relative overflow-hidden">
      <div
        className="absolute left-0 top-0 bottom-0 w-1"
        style={{ background: accent ? 'var(--accent)' : 'var(--navy)' }}
      />
      <div className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-semibold pl-2">
        {label}
      </div>
      <div
        className="mt-2 pl-2 font-display text-3xl"
        style={{ color: accent ? 'var(--accent)' : 'var(--navy)' }}
      >
        {value}
      </div>
      {hint && <div className="mt-1 pl-2 text-xs text-[var(--text-muted)]">{hint}</div>}
    </div>
  )
}

type BtnProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'navy' | 'ghost'
}

export function Button({ variant = 'primary', className = '', children, ...rest }: BtnProps) {
  const v =
    variant === 'primary' ? 'btn-primary' : variant === 'navy' ? 'btn-navy' : 'btn-ghost'
  return (
    <button type="button" className={`btn ${v} ${className}`} {...rest}>
      {children}
    </button>
  )
}

export function ButtonLink({
  to,
  children,
  variant = 'primary',
  className = '',
}: {
  to: string
  children: ReactNode
  variant?: 'primary' | 'navy' | 'ghost'
  className?: string
}) {
  const v =
    variant === 'primary' ? 'btn-primary' : variant === 'navy' ? 'btn-navy' : 'btn-ghost'
  return (
    <Link to={to} className={`btn ${v} ${className}`}>
      {children}
    </Link>
  )
}

export function Badge({
  children,
  tone = 'navy',
}: PropsWithChildren<{ tone?: 'navy' | 'accent' | 'success' | 'danger' | 'muted' }>) {
  const colors = {
    navy: { bg: 'rgba(27,31,76,0.08)', color: 'var(--navy)' },
    accent: { bg: 'var(--accent-soft)', color: 'var(--accent)' },
    success: { bg: 'rgba(46,125,50,0.12)', color: 'var(--success)' },
    danger: { bg: 'rgba(198,40,40,0.12)', color: 'var(--danger)' },
    muted: { bg: 'var(--bone)', color: 'var(--steel-muted)' },
  }[tone]
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold"
      style={{ background: colors.bg, color: colors.color }}
    >
      {children}
    </span>
  )
}

export function Alert({
  children,
  tone = 'danger',
}: PropsWithChildren<{ tone?: 'danger' | 'success' | 'muted' }>) {
  const styles = {
    danger: 'bg-[rgba(198,40,40,0.08)] text-[var(--danger)] border-[rgba(198,40,40,0.2)]',
    success: 'bg-[rgba(46,125,50,0.08)] text-[var(--success)] border-[rgba(46,125,50,0.2)]',
    muted: 'bg-[var(--bone)] text-[var(--text-muted)] border-[var(--border)]',
  }[tone]
  return <div className={`rounded-lg border px-3 py-2 text-sm ${styles}`}>{children}</div>
}

export function EmptyState({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="px-5 py-12 text-center relative overflow-hidden">
      <div
        className="pointer-events-none absolute inset-0 opacity-40"
        style={{
          background:
            'radial-gradient(ellipse at 50% 0%, rgba(27,31,76,0.06), transparent 55%)',
        }}
      />
      <p className="relative font-display text-lg text-[var(--navy)] uppercase tracking-wide">{title}</p>
      {hint && (
        <p className="relative mt-2 text-sm text-[var(--text-muted)] max-w-sm mx-auto leading-relaxed">
          {hint}
        </p>
      )}
      {action && <div className="relative mt-4 flex justify-center">{action}</div>}
    </div>
  )
}

export function DenseList({ children }: PropsWithChildren) {
  return <ul className="dense-list">{children}</ul>
}

export function StockBar({ available, total }: { available: number; total: number }) {
  const pct = total > 0 ? Math.min(100, Math.round((available / total) * 100)) : 0
  const low = pct <= 25
  return (
    <div className="space-y-1 min-w-[100px]">
      <div className="flex justify-between text-[0.65rem] font-semibold text-[var(--text-muted)]">
        <span>
          {available}/{total}
        </span>
        <span>{pct}%</span>
      </div>
      <div className="h-1.5 rounded-full bg-[var(--bone)] overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-500"
          style={{
            width: `${pct}%`,
            background: low ? 'var(--accent)' : 'var(--navy)',
          }}
        />
      </div>
    </div>
  )
}
