import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Badge, Button, Drawer, EmptyState, Input, PageHeader, Pagination, Panel } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api } from '../../services/api'
import type { PageResponse, ProductView } from '../../types/api'
import { getErrorMessage, money } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

const schema = z.object({
  sku: z.string().optional(),
  name: z.string().min(2, 'El nombre es obligatorio'),
  category: z.string().optional(),
  dailyPrice: z.coerce.number().min(0),
  lateFeePerDay: z.coerce.number().min(0).optional(),
  replacementValue: z.coerce.number().min(0).optional(),
  stockTotal: z.coerce.number().min(0),
  minStockThreshold: z.coerce.number().min(0).optional(),
})

export function InventarioPage() {
  const { can } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ProductView | null>(null)
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { name: '', dailyPrice: 0, stockTotal: 0 } })

  const list = useQuery({
    queryKey: ['inventario', q, page],
    queryFn: async () => (await api.get<PageResponse<ProductView>>('/inventario', { params: { q, page, size: 20 } })).data,
  })

  return (
    <div>
      <PageHeader
        eyebrow="Patio"
        title="Inventario"
        subtitle="Equipos, precio por dia, recargo por mora y stock disponible."
        action={can(P.INVENTARIO_GESTIONAR) ? <Button onClick={() => { setEditing(null); form.reset({ name: '', dailyPrice: 0, stockTotal: 0 }); setOpen(true) }}>Nuevo equipo</Button> : undefined}
      />
      <Panel>
        <Input label="Buscar" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <div className="mt-4 space-y-2">
          {(list.data?.content ?? []).map((p) => (
            <button key={p.id} className="flex w-full items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-left" onClick={() => { setEditing(p); form.reset(p); setOpen(true) }}>
              <div>
                <p className="font-medium text-navy">{p.name} {p.sku && <span className="text-xs text-steel-muted">{p.sku}</span>}</p>
                <p className="text-xs text-steel-muted">{money(p.dailyPrice)} / dia · {p.stockAvailable} libres de {p.stockTotal}</p>
              </div>
              {p.lowStock && <Badge tone="danger">Stock bajo</Badge>}
            </button>
          ))}
          {!list.data?.content.length && <EmptyState title="No hay equipos que coincidan" />}
        </div>
        <Pagination page={page} totalPages={list.data?.totalPages ?? 0} onPage={setPage} />
      </Panel>
      <Drawer open={open} title={editing ? editing.name : 'Nuevo equipo'} onClose={() => setOpen(false)}>
        <form className="space-y-3" onSubmit={form.handleSubmit(async (values) => {
          try {
            if (editing) await api.put(`/inventario/${editing.id}`, values)
            else await api.post('/inventario', values)
            toast.push('Equipo guardado')
            setOpen(false)
            void qc.invalidateQueries({ queryKey: ['inventario'] })
          } catch (error) {
            toast.push(getErrorMessage(error), 'error')
          }
        })}>
          <Input label="SKU" {...form.register('sku')} />
          <Input label="Nombre" error={form.formState.errors.name?.message} {...form.register('name')} />
          <Input label="Categoria" {...form.register('category')} />
          <Input label="Precio / dia" type="number" {...form.register('dailyPrice')} />
          <Input label="Recargo mora / dia" type="number" {...form.register('lateFeePerDay')} />
          <Input label="Valor reposicion" type="number" {...form.register('replacementValue')} />
          <Input label="Stock total" type="number" {...form.register('stockTotal')} />
          <Input label="Umbral minimo" type="number" {...form.register('minStockThreshold')} />
          {can(P.INVENTARIO_GESTIONAR) && <Button className="w-full" loading={form.formState.isSubmitting}>Guardar</Button>}
        </form>
        {editing && can(P.INVENTARIO_GESTIONAR) && (
          <form className="mt-6 space-y-3 border-t pt-4" onSubmit={async (e) => {
            e.preventDefault()
            const formData = new FormData(e.currentTarget)
            try {
              await api.post(`/inventario/${editing.id}/ajustar-stock`, {
                newTotal: Number(formData.get('newTotal')),
                reason: String(formData.get('reason')),
              })
              toast.push('Stock ajustado')
              void qc.invalidateQueries({ queryKey: ['inventario'] })
            } catch (error) {
              toast.push(getErrorMessage(error), 'error')
            }
          }}>
            <h4 className="font-display text-navy">Ajustar stock</h4>
            <Input label="Nuevo total" name="newTotal" type="number" defaultValue={editing.stockTotal} />
            <Input label="Motivo" name="reason" required />
            <Button variant="navy" type="submit">Aplicar ajuste</Button>
          </form>
        )}
      </Drawer>
    </div>
  )
}
