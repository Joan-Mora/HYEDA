import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Button, KpiCard, PageHeader, Panel } from '../../components/ui'
import { api } from '../../services/api'
import type { Dashboard, DebtRow, ProductView, RentalRow } from '../../types/api'
import { money } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

export function DashboardPage() {
  const navigate = useNavigate()
  const { can } = useAuth()
  const dashboard = useQuery({
    queryKey: ['tablero', 'MES'],
    queryFn: async () => (await api.get<Dashboard>('/analiticas/tablero', { params: { rango: 'MES' } })).data,
    enabled: can(P.ANALITICAS_VER) || can(P.DASHBOARD_VER),
  })
  const active = useQuery({
    queryKey: ['alquileres-activos'],
    queryFn: async () => (await api.get<RentalRow[]>('/alquileres/en-curso')).data,
  })
  const low = useQuery({
    queryKey: ['stock-bajo'],
    queryFn: async () => (await api.get<ProductView[]>('/inventario/stock-bajo')).data,
  })
  const debts = useQuery({
    queryKey: ['deudas'],
    queryFn: async () => (await api.get<DebtRow[]>('/finanzas/deudas')).data,
  })

  const kpis = dashboard.data?.kpis
  return (
    <div>
      <PageHeader
        eyebrow="Mesa de mando"
        title="Hoy en el patio"
        subtitle="Lo que hay que atender ahora: equipos en calle, deudas y stock bajo."
        action={can(P.ALQUILERES_GESTIONAR) ? <Button onClick={() => navigate('/alquileres/nuevo')}>Abrir alquiler</Button> : undefined}
      />
      <div className="mb-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="En calle" value={String(kpis?.activeRentals ?? active.data?.length ?? 0)} />
        <KpiCard label="Vencidos" value={String(kpis?.overdueRentals ?? 0)} />
        <KpiCard label="Cartera" value={money(kpis?.openDebt ?? 0)} />
        <KpiCard label="Utilidad del mes" value={money(kpis?.profit ?? 0)} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="Cola operativa">
          <ul className="space-y-2 text-sm">
            {(active.data ?? []).slice(0, 8).map((rental) => (
              <li key={rental.id} className="flex justify-between rounded-lg bg-slate-50 px-3 py-2">
                <span>{rental.code} · {rental.customerName}</span>
                <button className="text-accent" onClick={() => navigate(`/alquileres?focus=${rental.id}`)}>Gestionar</button>
              </li>
            ))}
            {!active.data?.length && <p className="text-steel-muted">No hay equipos en calle.</p>}
          </ul>
        </Panel>
        <Panel title="Pulso de ingresos">
          <div className="h-56">
            <ResponsiveContainer>
              <LineChart data={dashboard.data?.revenueSeries ?? []}>
                <XAxis dataKey="date" hide />
                <YAxis hide />
                <Tooltip formatter={(value) => money(Number(value ?? 0))} />
                <Line type="monotone" dataKey="income" stroke="#1B1F4C" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="Stock bajo">
          {(low.data ?? []).slice(0, 6).map((p) => (
            <p key={p.id} className="text-sm">{p.name}: {p.stockAvailable} libres</p>
          ))}
          {!low.data?.length && <p className="text-sm text-steel-muted">Sin alertas de inventario.</p>}
        </Panel>
        <Panel title="Cartera abierta">
          {(debts.data ?? []).slice(0, 6).map((d) => (
            <p key={d.id} className="text-sm">{d.customerName}: {money(d.balance)}</p>
          ))}
          {!debts.data?.length && <p className="text-sm text-steel-muted">Nadie debe en este momento.</p>}
        </Panel>
      </div>
    </div>
  )
}
