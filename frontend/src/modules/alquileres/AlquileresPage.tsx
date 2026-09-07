import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SkeletonRows, StatusPulse } from '../../components/Motion'
import { PdfModal, WorksSites } from '../../components/Overlay'
import { SlideOver } from '../../components/SlideOver'
import { useToast } from '../../components/Toast'
import {
  Alert,
  Button,
  ButtonLink,
  EmptyState,
  ModuleTabs,
  PageHeader,
} from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type { Document, Rental } from '../../types'
import { formatDate, formatMoney } from '../../utils/format'

type HubTab = 'calle' | 'historial' | 'documentos'

const ON_STREET: Rental['status'][] = [
  'RESERVADO',
  'DOCUMENTO_EMITIDO',
  'EN_CURSO',
  'PENDIENTE_PAGO',
  'FACTURACION_FINAL',
  'DEVUELTO',
]

function pulse(status: string) {
  if (status === 'EN_CURSO' || status === 'DOCUMENTO_EMITIDO') return { s: 'en_calle' as const, l: 'En calle' }
  if (status === 'PENDIENTE_PAGO' || status === 'CANCELADO') return { s: 'vencido' as const, l: status }
  if (status === 'CERRADO' || status === 'DEVUELTO') return { s: 'ok' as const, l: 'En regla' }
  return { s: 'muted' as const, l: status }
}

export default function AlquileresPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const tab = (params.get('tab') as HubTab) || 'calle'
  const focus = params.get('focus')
  const [drawerId, setDrawerId] = useState<number | null>(null)
  const [pdfId, setPdfId] = useState<number | null>(null)

  const { data: rentals = [], isLoading, error } = useQuery({
    queryKey: ['alquileres'],
    queryFn: async () => (await api.get<Rental[]>('/alquileres')).data,
  })

  const docsQuery = useQuery({
    queryKey: ['documentos'],
    enabled: tab === 'documentos' || pdfId != null,
    queryFn: async () => (await api.get<Document[]>('/documentos')).data,
  })

  const onStreet = useMemo(
    () =>
      rentals.filter(
        (r) => ON_STREET.includes(r.status) && r.status !== 'CERRADO' && r.status !== 'CANCELADO',
      ),
    [rentals],
  )
  const history = useMemo(() => [...rentals].sort((a, b) => b.id - a.id), [rentals])
  const list = tab === 'calle' ? onStreet : history
  const selected = rentals.find((r) => r.id === drawerId) || null

  useEffect(() => {
    if (focus) {
      const id = Number(focus)
      if (Number.isFinite(id)) setDrawerId(id)
    }
  }, [focus])

  const workSites = onStreet
    .filter((r) => (r.deliveryReference || r.deliveryAddress || '').trim().length > 0)
    .map((r) => ({
      id: r.id,
      reference: r.deliveryReference,
      address: r.deliveryAddress,
      label: `#${r.id} · ${r.customerName || r.customerId}`,
    }))

  const act = useMutation({
    mutationFn: async ({
      id,
      action,
    }: {
      id: number
      action: 'emitir' | 'entregar' | 'devolver' | 'cerrar'
    }) => {
      if (action === 'emitir') return (await api.post(`/alquileres/${id}/emitir-documento`)).data
      if (action === 'entregar') return (await api.post(`/alquileres/${id}/confirmar-entrega`)).data
      if (action === 'devolver')
        return (
          await api.post(`/alquileres/${id}/devolver`, {
            actualReturnDate: new Date().toISOString().slice(0, 10),
            incidentReason: null,
            incidentDescription: null,
          })
        ).data
      return (await api.post(`/alquileres/${id}/cerrar`)).data
    },
    onSuccess: (_d, vars) => {
      toast.success(`Alquiler #${vars.id}: ${vars.action} OK`)
      void qc.invalidateQueries({ queryKey: ['alquileres'] })
      void qc.invalidateQueries({ queryKey: ['dashboard'] })
      void qc.invalidateQueries({ queryKey: ['documentos'] })
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  })

  function setTab(next: HubTab) {
    const nextParams = new URLSearchParams(params)
    if (next === 'calle') nextParams.delete('tab')
    else nextParams.set('tab', next)
    setParams(nextParams)
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Operación"
        title="Alquileres"
        subtitle="Hub con drawer, obras por dirección y documentos in-app"
        actions={<ButtonLink to="/alquileres/nuevo">Abrir alquiler</ButtonLink>}
      />

      {error && <Alert>{getErrorMessage(error)}</Alert>}

      {tab === 'calle' && <WorksSites sites={workSites} />}

      <ModuleTabs
        value={tab}
        onChange={(id) => setTab(id as HubTab)}
        tabs={[
          { id: 'calle', label: `En calle (${onStreet.length})` },
          { id: 'historial', label: `Historial (${rentals.length})` },
          { id: 'documentos', label: 'Documentos' },
        ]}
      />

      {(tab === 'calle' || tab === 'historial') && (
        <section className="panel overflow-hidden anim-panel">
          {isLoading ? (
            <SkeletonRows rows={6} />
          ) : list.length === 0 ? (
            <EmptyState
              title={tab === 'calle' ? 'Nada en calle' : 'Sin historial'}
              action={<ButtonLink to="/alquileres/nuevo">Abrir alquiler</ButtonLink>}
            />
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {list.map((r) => {
                const p = pulse(r.status)
                return (
                  <li key={r.id}>
                    <button
                      type="button"
                      className="w-full text-left px-5 py-3.5 hover:bg-[#f9f9fb] flex items-center justify-between gap-3"
                      onClick={() => setDrawerId(r.id)}
                    >
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-semibold text-[var(--navy)]">#{r.id}</span>
                          <StatusPulse status={p.s} label={p.l} />
                        </div>
                        <p className="text-sm truncate mt-0.5">{r.customerName || r.customerId}</p>
                        <p className="text-xs text-[var(--text-muted)]">
                          {formatDate(r.startDate)} · {formatMoney(r.finalAmount ?? r.totalAmount)}
                        </p>
                      </div>
                      <span className="text-xs font-bold text-[var(--accent)] shrink-0">Abrir →</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      )}

      {tab === 'documentos' && (
        <section className="panel overflow-hidden">
          {docsQuery.isLoading ? (
            <SkeletonRows />
          ) : (docsQuery.data || []).length === 0 ? (
            <EmptyState title="Sin documentos" />
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {(docsQuery.data || []).map((doc) => (
                <li key={doc.id}>
                  <button
                    type="button"
                    className="w-full text-left px-5 py-3 hover:bg-[#f9f9fb]"
                    onClick={() => setPdfId(doc.id)}
                  >
                    <div className="font-medium text-[var(--navy)]">{doc.documentType}</div>
                    <div className="text-xs text-[var(--text-muted)]">
                      Alquiler #{doc.rentalId} · {formatDate(doc.generatedAt)}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <SlideOver
        open={drawerId != null && selected != null}
        onClose={() => setDrawerId(null)}
        title={selected ? `Alquiler #${selected.id}` : ''}
        subtitle="Detalle operativo"
        width={460}
      >
        {selected && (
          <div className="flex flex-col h-full">
            <div className="p-5 space-y-4 flex-1">
              <StatusPulse {...{ status: pulse(selected.status).s, label: pulse(selected.status).l }} />
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-xs uppercase font-bold text-[var(--text-muted)]">Cliente</dt>
                  <dd className="font-medium">{selected.customerName || selected.customerId}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase font-bold text-[var(--text-muted)]">Total</dt>
                  <dd className="font-medium">{formatMoney(selected.finalAmount ?? selected.totalAmount)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase font-bold text-[var(--text-muted)]">Inicio</dt>
                  <dd>{formatDate(selected.startDate)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase font-bold text-[var(--text-muted)]">Días</dt>
                  <dd>{selected.estimatedDays ?? '—'}</dd>
                </div>
              </dl>
              {(selected.deliveryReference || selected.deliveryAddress) && (
                <div className="text-sm space-y-1">
                  {selected.deliveryReference && (
                    <p>
                      <strong className="text-[var(--navy)]">Referencia:</strong> {selected.deliveryReference}
                    </p>
                  )}
                  {selected.deliveryAddress && (
                    <p className="text-[var(--text-muted)]">
                      <strong className="text-[var(--navy)]">Dirección:</strong> {selected.deliveryAddress}
                    </p>
                  )}
                </div>
              )}
              {(selected.items || []).length > 0 && (
                <ul className="dense-list">
                  {selected.items.map((it, i) => (
                    <li key={i}>
                      <span>
                        {it.productName || `#${it.productId}`} × {it.quantity}
                      </span>
                      <span>{formatMoney(it.unitPrice)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div className="p-4 border-t border-[var(--border)] flex flex-wrap gap-2 bg-[var(--bone)]">
              <Button
                variant="navy"
                disabled={act.isPending}
                onClick={() => act.mutate({ id: selected.id, action: 'emitir' })}
              >
                Emitir doc
              </Button>
              <Button
                variant="ghost"
                disabled={act.isPending}
                onClick={() => act.mutate({ id: selected.id, action: 'entregar' })}
              >
                Entregar
              </Button>
              <Button
                variant="ghost"
                disabled={act.isPending}
                onClick={() => act.mutate({ id: selected.id, action: 'devolver' })}
              >
                Devolver
              </Button>
              <Button
                variant="primary"
                disabled={act.isPending}
                onClick={() => act.mutate({ id: selected.id, action: 'cerrar' })}
              >
                Cerrar
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  const doc = (docsQuery.data || []).find((d) => d.rentalId === selected.id)
                  if (doc) setPdfId(doc.id)
                  else {
                    setTab('documentos')
                    toast.push('Abre Documentos para ver PDFs de este alquiler')
                  }
                }}
              >
                Ver PDF
              </Button>
            </div>
          </div>
        )}
      </SlideOver>

      <PdfModal
        open={pdfId != null}
        documentId={pdfId}
        onClose={() => setPdfId(null)}
        title="Vista previa del contrato"
      />
    </div>
  )
}
