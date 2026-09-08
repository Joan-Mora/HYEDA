import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Bar, BarChart, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Button, KpiCard, PageHeader, Panel, Select } from '../../components/ui'
import { api, getAccessToken } from '../../services/api'
import type { Dashboard } from '../../types/api'
import { money } from '../../lib/format'

export function AnaliticasPage() {
  const [rango, setRango] = useState('MES')
  const dash = useQuery({
    queryKey: ['analiticas', rango],
    queryFn: async () => (await api.get<Dashboard>('/analiticas/tablero', { params: { rango } })).data,
  })
  const k = dash.data?.kpis

  async function download(path: string) {
    const res = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8081/api'}${path}?rango=${rango}`, {
      headers: { Authorization: `Bearer ${getAccessToken()}` },
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = path.includes('xlsx') ? 'reporte-hyeda.xlsx' : 'reporte-hyeda.pdf'
    a.click()
  }

  return (
    <div>
      <PageHeader
        eyebrow="Pulso"
        title="Analiticas"
        subtitle="Producto mas alquilado, cliente estrella, quiebres y devoluciones con problema."
        action={
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => download('/analiticas/reporte.pdf')}>PDF</Button>
            <Button variant="navy" onClick={() => download('/analiticas/reporte.xlsx')}>Excel</Button>
          </div>
        }
      />
      <div className="mb-4 max-w-xs">
        <Select label="Rango" value={rango} onChange={(e) => setRango(e.target.value)}>
          <option value="DIA">Dia</option>
          <option value="SEMANA">Semana</option>
          <option value="MES">Mes</option>
          <option value="ANIO">Anio</option>
        </Select>
      </div>
      <div className="mb-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="Abiertos" value={String(k?.rentalsOpened ?? 0)} />
        <KpiCard label="Exitosos" value={String(k?.successfulRentals ?? 0)} />
        <KpiCard label="Problemas" value={String(k?.problemReturns ?? 0)} />
        <KpiCard label="Ticket promedio" value={money(k?.averageTicket ?? 0)} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="Ingresos vs gastos">
          <div className="h-56">
            <ResponsiveContainer>
              <LineChart data={dash.data?.revenueSeries ?? []}>
                <XAxis dataKey="date" hide />
                <YAxis hide />
                <Tooltip formatter={(v) => money(Number(v ?? 0))} />
                <Line dataKey="income" stroke="#1B1F4C" dot={false} />
                <Line dataKey="expense" stroke="#E65100" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="Producto mas alquilado">
          <div className="h-56">
            <ResponsiveContainer>
              <BarChart data={dash.data?.topProducts ?? []}>
                <XAxis dataKey="name" hide />
                <YAxis hide />
                <Tooltip />
                <Bar dataKey="units" fill="#1B1F4C" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="Cliente estrella por valor">
          {(dash.data?.topCustomersByValue ?? []).map((c) => (
            <p key={c.customerId} className="text-sm">{c.name}: {money(c.amount)}</p>
          ))}
        </Panel>
        <Panel title="Quiebres de stock">
          {(dash.data?.stockouts ?? []).map((s) => (
            <p key={s.productId} className="text-sm">{s.name}: {s.available}/{s.total}</p>
          ))}
        </Panel>
      </div>
    </div>
  )
}
