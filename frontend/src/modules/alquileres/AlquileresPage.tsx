import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Badge, Button, Drawer, EmptyState, Modal, PageHeader, Pagination, Panel } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api } from '../../services/api'
import type { PageResponse, RentalRow, RentalView } from '../../types/api'
import { getErrorMessage, money } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

export function AlquileresPage() {
  const { can } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const [tab, setTab] = useState<'EN_CURSO' | ''>(params.get('tab') === 'historial' ? '' : 'EN_CURSO')
  const [selected, setSelected] = useState<number | null>(params.get('focus') ? Number(params.get('focus')) : null)
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)

  const list = useQuery({
    queryKey: ['alquileres', tab, page],
    queryFn: async () => (await api.get<PageResponse<RentalRow>>('/alquileres', { params: { estado: tab || undefined, page, size: 20 } })).data,
  })
  const detail = useQuery({
    queryKey: ['alquiler', selected],
    queryFn: async () => (await api.get<RentalView>(`/alquileres/${selected}`)).data,
    enabled: selected != null,
  })

  useEffect(() => {
    if (params.get('focus')) setSelected(Number(params.get('focus')))
  }, [params])

  async function act(path: string, body?: unknown) {
    try {
      await api.post(path, body)
      toast.push('Listo')
      void qc.invalidateQueries({ queryKey: ['alquileres'] })
      void qc.invalidateQueries({ queryKey: ['alquiler', selected] })
    } catch (error) {
      toast.push(getErrorMessage(error), 'error')
    }
  }

  return (
    <div>
      <PageHeader eyebrow="Operacion" title="Alquileres" subtitle="En calle, historial y drawer de gestion." />
      <div className="mb-4 flex gap-2">
        <Button variant={tab === 'EN_CURSO' ? 'navy' : 'ghost'} onClick={() => { setTab('EN_CURSO'); setPage(0) }}>En calle</Button>
        <Button variant={tab === '' ? 'navy' : 'ghost'} onClick={() => { setTab(''); setPage(0); setParams({ tab: 'historial' }) }}>Historial</Button>
      </div>
      <Panel>
        {(list.data?.content ?? []).map((r) => (
          <button key={r.id} className="mb-2 flex w-full items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-left" onClick={() => setSelected(r.id)}>
            <div>
              <p className="font-medium text-navy">{r.code} · {r.customerName}</p>
              <p className="text-xs text-steel-muted">{r.startDate} → {r.endDate} · {money(r.amount)}</p>
            </div>
            <Badge tone={r.overdue ? 'danger' : 'navy'}>{r.status}</Badge>
          </button>
        ))}
        {!list.data?.content.length && <EmptyState title="No hay alquileres en esta vista" />}
        <Pagination page={page} totalPages={list.data?.totalPages ?? 0} onPage={setPage} />
      </Panel>

      <Drawer open={selected != null} title={detail.data?.code ?? 'Alquiler'} onClose={() => setSelected(null)}>
        {detail.data && (
          <div className="space-y-3 text-sm">
            <p>{detail.data.customerName} · {detail.data.deliveryAddress}</p>
            {detail.data.deliveryMapsUrl && <a className="text-accent" href={detail.data.deliveryMapsUrl} target="_blank" rel="noreferrer">Ver en Maps</a>}
            <p>Total vigente {money(detail.data.currentAmount)} · Pendiente {money(detail.data.pendingAmount)}</p>
            <ul>{detail.data.items.map((item) => <li key={item.id}>{item.productName} x{item.quantity}</li>)}</ul>
            {can(P.ALQUILERES_GESTIONAR) && (
              <div className="flex flex-wrap gap-2">
                <Button variant="navy" onClick={() => act(`/documentos/alquiler/${detail.data.id}/emitir`)}>Emitir documento</Button>
                <Button onClick={() => act(`/alquileres/${detail.data.id}/entregar`)}>Entregar</Button>
                <Button variant="ghost" onClick={() => {
                  const days = Number(prompt('Dias extra', '1'))
                  if (days > 0) void act(`/alquileres/${detail.data.id}/extender`, { extraDays: days, reason: 'Extension en mostrador' })
                }}>Extender</Button>
                <Button variant="ghost" onClick={() => act(`/alquileres/${detail.data.id}/devolucion`, { returnDate: new Date().toISOString().slice(0, 10), incidents: [] })}>Devolver</Button>
                {can(P.PAGOS_REGISTRAR) && (
                  <Button variant="ghost" onClick={() => {
                    const amount = Number(prompt('Valor recibido', String(detail.data.pendingAmount || detail.data.currentAmount)))
                    if (amount > 0) void act(`/pagos/alquiler/${detail.data.id}/efectivo`, { amount, amountTendered: amount })
                  }}>Cobrar efectivo</Button>
                )}
                <Button variant="ghost" onClick={async () => {
                  const docs = await api.get(`/documentos/alquiler/${detail.data.id}`)
                  const first = docs.data[0]
                  if (!first) return toast.push('Aun no hay PDF', 'error')
                  const blob = await api.get(`/documentos/${first.id}/contenido`, { responseType: 'blob' })
                  setPdfUrl(URL.createObjectURL(blob.data))
                }}>Ver PDF</Button>
              </div>
            )}
          </div>
        )}
      </Drawer>
      <Modal open={Boolean(pdfUrl)} title="Documento" onClose={() => setPdfUrl(null)}>
        {pdfUrl && <iframe title="PDF" src={pdfUrl} className="h-[70vh] w-full rounded" />}
      </Modal>
    </div>
  )
}
