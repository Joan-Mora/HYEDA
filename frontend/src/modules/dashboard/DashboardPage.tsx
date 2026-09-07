import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Alert, ButtonLink, EmptyState, PageHeader } from '../../components/ui'
import { AnimatedNumber, SkeletonRows, StatusPulse } from '../../components/Motion'
import { WorksSites } from '../../components/Overlay'
import api, { getErrorMessage } from '../../services/api'
import type { Dashboard, Debt, Product, Rental } from '../../types'
import { formatDate, formatMoney } from '../../utils/format'
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

const emptyDashboard: Dashboard = {
  rentalsActive: 0,
  rentalsClosed: 0,
  lowStockProducts: 0,
  openDebts: 0,
  incomeTotal: 0,
  expenseTotal: 0,
  topProducts: [],
  topCustomersByCount: [],
  topCustomersByValue: [],
  incomeByDay: [],
}

const ACTIVE: Rental['status'][] = [
  'RESERVADO',
  'DOCUMENTO_EMITIDO',
  'EN_CURSO',
  'PENDIENTE_PAGO',
  'FACTURACION_FINAL',
]

async function downloadExcel() {
  try {
    const token = localStorage.getItem('hyeda_token')
    const base = import.meta.env.VITE_API_URL || 'http://localhost:8081/api'
    const res = await fetch(`${base}/analytics/export/excel`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!res.ok) throw new Error('Exportación fallida')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'hyeda-analytics.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    alert('No se pudo exportar el Excel')
  }
}

function pulseFor(status: Rental['status']): { status: 'ok' | 'en_calle' | 'vencido' | 'muted'; label: string } {
  if (status === 'EN_CURSO' || status === 'DOCUMENTO_EMITIDO') return { status: 'en_calle', label: 'En calle' }
  if (status === 'PENDIENTE_PAGO') return { status: 'vencido', label: 'Pago pendiente' }
  if (status === 'CERRADO') return { status: 'ok', label: 'En regla' }
  return { status: 'muted', label: status }
}

export default function DashboardPage() {
  const dashQuery = useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => (await api.get<Dashboard>('/analytics/dashboard')).data,
    retry: 1,
  })
  const rentalsQuery = useQuery({
    queryKey: ['alquileres', 'hoy'],
    queryFn: async () => (await api.get<Rental[]>('/alquileres')).data,
  })
  const debtsQuery = useQuery({
    queryKey: ['finanzas', 'deudas', 'hoy'],
    queryFn: async () => (await api.get<Debt[]>('/finanzas/deudas')).data,
  })
  const stockQuery = useQuery({
    queryKey: ['productos', 'low-stock'],
    queryFn: async () => (await api.get<Product[]>('/productos/low-stock')).data,
  })

  const dash = dashQuery.data ?? emptyDashboard
  const activeRentals = (rentalsQuery.data || []).filter((r) => ACTIVE.includes(r.status))
  const openDebts = (debtsQuery.data || []).filter(
    (d) => d.status === 'ABIERTA' || d.status === 'VENCIDA' || d.status === 'PARCIAL',
  )
  const lowStock = stockQuery.data || []
  const incomeData = (dash.incomeByDay || []).map((d, i) => ({
    fecha: String(d.date ?? d.label ?? d.name ?? `D${i + 1}`).slice(5),
    ingresos: Number(d.amount ?? d.value ?? 0),
  }))
  const workSites = activeRentals
    .filter((r) => (r.deliveryReference || r.deliveryAddress || '').trim().length > 0)
    .map((r) => ({
      id: r.id,
      reference: r.deliveryReference,
      address: r.deliveryAddress,
      label: `#${r.id} · ${r.customerName || r.customerId}`,
    }))

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Mesa de mando"
        title="Hoy"
        subtitle="Situación del patio · Granada, Meta"
        actions={
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn btn-ghost !py-2" onClick={() => void downloadExcel()}>
              Exportar Excel
            </button>
            <ButtonLink to="/alquileres/nuevo">Abrir alquiler</ButtonLink>
          </div>
        }
      />

      <section className="situation-band anim-rise">
        <div className="situation-band__texture" aria-hidden />
        <div className="situation-band__grid" aria-hidden />
        <div className="relative z-[1] flex flex-col lg:flex-row lg:items-end lg:justify-between gap-6">
          <div className="max-w-xl">
            <p className="text-[0.7rem] uppercase tracking-[0.18em] text-white/55 font-bold">
              Situación del patio
            </p>
            <h2 className="font-display text-3xl md:text-[2.6rem] uppercase mt-2 leading-[1.05] text-white">
              Quién tiene el equipo.
              <br />
              Quién debe. Qué falta.
            </h2>
          </div>
          <div className="flex flex-wrap gap-2">
            <div className="rounded-md px-3.5 py-2.5 min-w-[96px] bg-white/10 border border-white/15">
              <div className="text-[0.65rem] uppercase tracking-wider text-white/60 font-bold">En calle</div>
              <AnimatedNumber className="font-display text-2xl text-white" value={dash.rentalsActive} />
            </div>
            <div className="rounded-md px-3.5 py-2.5 min-w-[96px] bg-[rgba(230,81,0,0.22)] border border-[rgba(230,81,0,0.45)]">
              <div className="text-[0.65rem] uppercase tracking-wider text-white/60 font-bold">Deudas</div>
              <AnimatedNumber className="font-display text-2xl text-white" value={dash.openDebts} />
            </div>
            <div className="rounded-md px-3.5 py-2.5 min-w-[96px] bg-white/10 border border-white/15">
              <div className="text-[0.65rem] uppercase tracking-wider text-white/60 font-bold">Stock bajo</div>
              <AnimatedNumber className="font-display text-2xl text-white" value={dash.lowStockProducts} />
            </div>
          </div>
        </div>
        <div className="relative z-[1] mt-6 flex flex-wrap gap-8 text-sm border-t border-white/15 pt-4">
          <div>
            <span className="text-white/50 text-xs uppercase tracking-wider">Ingresos</span>
            <div className="font-display text-2xl text-white mt-0.5">
              <AnimatedNumber value={dash.incomeTotal} format={(n) => formatMoney(n)} />
            </div>
          </div>
          <div>
            <span className="text-white/50 text-xs uppercase tracking-wider">Gastos</span>
            <div className="font-display text-2xl text-white/85 mt-0.5">
              <AnimatedNumber value={dash.expenseTotal} format={(n) => formatMoney(n)} />
            </div>
          </div>
        </div>
      </section>

      {dashQuery.error && (
        <Alert>{getErrorMessage(dashQuery.error, 'No se pudo cargar el tablero.')}</Alert>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <section className="panel overflow-hidden anim-rise anim-rise-delay-1">
          <div className="flex items-center justify-between gap-3 px-5 py-3.5 border-b border-[var(--border)]">
            <h2 className="font-display text-lg text-[var(--navy)] uppercase tracking-wide">Cola operativa</h2>
            <Link to="/alquileres" className="text-xs font-semibold text-[var(--accent)]">
              Ver hub →
            </Link>
          </div>
          {rentalsQuery.isLoading ? (
            <SkeletonRows rows={5} />
          ) : activeRentals.length === 0 && openDebts.length === 0 && lowStock.length === 0 ? (
            <EmptyState title="Patio en calma" hint="Abre el primer alquiler del día." />
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {activeRentals.slice(0, 8).map((r) => {
                const p = pulseFor(r.status)
                return (
                  <li key={r.id} className="flex items-center justify-between gap-3 px-5 py-3.5 hover:bg-[#f9f9fb]">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-[var(--navy)]">#{r.id}</span>
                        <StatusPulse status={p.status} label={p.label} />
                      </div>
                      <p className="text-sm mt-0.5 truncate">{r.customerName || `Cliente #${r.customerId}`}</p>
                      <p className="text-xs text-[var(--text-muted)]">
                        {formatDate(r.startDate)} · {formatMoney(r.finalAmount ?? r.totalAmount)}
                      </p>
                    </div>
                    <Link to={`/alquileres?focus=${r.id}`} className="text-xs font-bold text-[var(--navy)]">
                      Gestionar
                    </Link>
                  </li>
                )
              })}
              {openDebts.slice(0, 3).map((d) => (
                <li key={`d-${d.id}`} className="px-5 py-3.5 flex justify-between gap-3 bg-[rgba(198,40,40,0.04)]">
                  <div>
                    <StatusPulse status="vencido" label="Cartera" />
                    <p className="text-sm mt-1">{d.customerName || d.customerId}</p>
                  </div>
                  <strong className="text-[var(--danger)]">{formatMoney(d.balance)}</strong>
                </li>
              ))}
            </ul>
          )}
        </section>

        <div className="space-y-6">
          <WorksSites sites={workSites} />
          <section className="panel overflow-hidden">
            <div className="px-5 py-3.5 border-b border-[var(--border)]">
              <h2 className="font-display text-lg text-[var(--navy)] uppercase">Pulso de ingresos</h2>
            </div>
            <div className="p-4 h-[220px]">
              {dashQuery.isLoading ? (
                <SkeletonRows rows={4} />
              ) : incomeData.length === 0 ? (
                <EmptyState title="Sin serie" />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={incomeData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                    <XAxis dataKey="fecha" tick={{ fontSize: 11, fill: 'var(--steel-muted)' }} />
                    <YAxis tick={{ fontSize: 11, fill: 'var(--steel-muted)' }} width={56} />
                    <Tooltip formatter={(v) => formatMoney(Number(v))} />
                    <Line type="monotone" dataKey="ingresos" stroke="var(--accent)" strokeWidth={2.5} dot={{ r: 3 }} />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
