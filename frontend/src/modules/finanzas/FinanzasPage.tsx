import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Button, Input, PageHeader, Pagination, Panel, Select } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api } from '../../services/api'
import type { DebtRow, MovementView, PageResponse } from '../../types/api'
import { getErrorMessage, money, todayIso } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

export function FinanzasPage() {
  const { can } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState<'movimientos' | 'deudas'>('movimientos')
  const [page, setPage] = useState(0)
  const from = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10)

  const movements = useQuery({
    queryKey: ['movimientos', page],
    queryFn: async () => (await api.get<PageResponse<MovementView>>('/finanzas/movimientos', { params: { page, size: 20 } })).data,
  })
  const debts = useQuery({
    queryKey: ['deudas-caja'],
    queryFn: async () => (await api.get<DebtRow[]>('/finanzas/deudas')).data,
  })
  const summary = useQuery({
    queryKey: ['resumen-caja', from],
    queryFn: async () => (await api.get('/finanzas/resumen', { params: { desde: from, hasta: todayIso() } })).data,
  })

  return (
    <div>
      <PageHeader eyebrow="Caja" title="Finanzas" subtitle="Ingresos, gastos y cartera con antiguedad." />
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <Panel title="Ingresos"><p className="font-display text-2xl">{money(summary.data?.income)}</p></Panel>
        <Panel title="Gastos"><p className="font-display text-2xl">{money(summary.data?.expense)}</p></Panel>
        <Panel title="Cartera"><p className="font-display text-2xl">{money(summary.data?.openDebt)}</p></Panel>
      </div>
      <div className="mb-3 flex gap-2">
        <Button variant={tab === 'movimientos' ? 'navy' : 'ghost'} onClick={() => setTab('movimientos')}>Movimientos</Button>
        <Button variant={tab === 'deudas' ? 'navy' : 'ghost'} onClick={() => setTab('deudas')}>Deudas</Button>
      </div>
      {tab === 'movimientos' && (
        <Panel>
          {can(P.FINANZAS_GESTIONAR) && (
            <form className="mb-4 grid gap-2 md:grid-cols-5" onSubmit={async (e) => {
              e.preventDefault()
              const data = new FormData(e.currentTarget)
              try {
                await api.post('/finanzas/movimientos', {
                  movementType: data.get('movementType'),
                  amount: Number(data.get('amount')),
                  description: data.get('description'),
                  category: data.get('category'),
                  movementDate: data.get('movementDate'),
                })
                toast.push('Movimiento registrado')
                void qc.invalidateQueries({ queryKey: ['movimientos'] })
                void qc.invalidateQueries({ queryKey: ['resumen-caja'] })
                e.currentTarget.reset()
              } catch (error) {
                toast.push(getErrorMessage(error), 'error')
              }
            }}>
              <Select label="Tipo" name="movementType">
                <option value="INGRESO">Ingreso</option>
                <option value="GASTO">Gasto</option>
              </Select>
              <Input label="Valor" name="amount" type="number" required />
              <Input label="Categoria" name="category" />
              <Input label="Fecha" name="movementDate" type="date" defaultValue={todayIso()} />
              <Input label="Descripcion" name="description" />
              <Button type="submit">Registrar</Button>
            </form>
          )}
          {(movements.data?.content ?? []).map((m) => (
            <p key={m.id} className="flex justify-between text-sm">
              <span>{m.movementDate} · {m.movementType} · {m.description}</span>
              <span>{money(m.amount)}</span>
            </p>
          ))}
          <Pagination page={page} totalPages={movements.data?.totalPages ?? 0} onPage={setPage} />
        </Panel>
      )}
      {tab === 'deudas' && (
        <Panel>
          {(debts.data ?? []).map((d) => (
            <div key={d.id} className="mb-2 flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
              <span>{d.customerName} · {d.agingBucket} · {money(d.balance)}</span>
              {can(P.FINANZAS_GESTIONAR) && (
                <Button variant="ghost" onClick={async () => {
                  const motivo = prompt('Motivo de condonacion')
                  if (!motivo) return
                  await api.post(`/finanzas/deudas/${d.id}/condonar`, null, { params: { motivo } })
                  void qc.invalidateQueries({ queryKey: ['deudas-caja'] })
                }}>Condonar</Button>
              )}
            </div>
          ))}
        </Panel>
      )}
    </div>
  )
}
