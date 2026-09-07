import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ModuleTabs, PageHeader } from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type { Debt, FinancialMovement } from '../../types'
import { formatDate, formatMoney } from '../../utils/format'

type Tab = 'movimientos' | 'deudas'

export default function FinanzasPage() {
  const [tab, setTab] = useState<Tab>('movimientos')

  const movementsQuery = useQuery({
    queryKey: ['finanzas', 'movimientos'],
    queryFn: async () => {
      const { data } = await api.get<FinancialMovement[]>('/finanzas/movimientos')
      return data
    },
    enabled: tab === 'movimientos',
  })

  const debtsQuery = useQuery({
    queryKey: ['finanzas', 'deudas'],
    queryFn: async () => {
      const { data } = await api.get<Debt[]>('/finanzas/deudas')
      return data
    },
    enabled: tab === 'deudas',
  })

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Dinero"
        title="Caja"
        subtitle="Movimientos del día y cartera de clientes"
      />

      <ModuleTabs
        value={tab}
        onChange={(id) => setTab(id as Tab)}
        tabs={[
          { id: 'movimientos', label: 'Movimientos' },
          { id: 'deudas', label: 'Deudas' },
        ]}
      />

      <section className="panel overflow-hidden anim-panel" key={tab}>
        {tab === 'movimientos' && (
          <>
            {movementsQuery.error && (
              <p className="p-4 text-sm text-[var(--danger)]">
                {getErrorMessage(movementsQuery.error)}
              </p>
            )}
            {movementsQuery.isLoading ? (
              <p className="p-4 text-[var(--text-muted)]">Cargando…</p>
            ) : (movementsQuery.data || []).length === 0 ? (
              <p className="p-4 text-[var(--text-muted)]">Sin movimientos.</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Categoría</th>
                    <th>Descripción</th>
                    <th>Monto</th>
                  </tr>
                </thead>
                <tbody>
                  {(movementsQuery.data || []).map((m) => (
                    <tr key={m.id}>
                      <td>{formatDate(m.movementDate)}</td>
                      <td>{m.movementType}</td>
                      <td>{m.category || '—'}</td>
                      <td>{m.description || '—'}</td>
                      <td className="font-medium">{formatMoney(m.amount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}

        {tab === 'deudas' && (
          <>
            {debtsQuery.error && (
              <p className="p-4 text-sm text-[var(--danger)]">{getErrorMessage(debtsQuery.error)}</p>
            )}
            {debtsQuery.isLoading ? (
              <p className="p-4 text-[var(--text-muted)]">Cargando…</p>
            ) : (debtsQuery.data || []).length === 0 ? (
              <p className="p-4 text-[var(--text-muted)]">Sin deudas.</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Cliente</th>
                    <th>Alquiler</th>
                    <th>Saldo</th>
                    <th>Vence</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {(debtsQuery.data || []).map((d) => (
                    <tr key={d.id}>
                      <td>{d.customerName || d.customerId}</td>
                      <td>#{d.rentalId ?? '—'}</td>
                      <td className="font-medium">{formatMoney(d.balance)}</td>
                      <td>{formatDate(d.dueDate)}</td>
                      <td>{d.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </section>
    </div>
  )
}
