import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { SkeletonRows, StatusPulse } from '../../components/Motion'
import { SlideOver } from '../../components/SlideOver'
import { useToast } from '../../components/Toast'
import { Badge, Button, EmptyState, ModuleTabs, PageHeader, Panel } from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type {
  BackgroundCheckRow,
  Customer,
  CustomerRequest,
  CustomerType,
  Debt,
  Rental,
} from '../../types'
import { formatDate, formatMoney } from '../../utils/format'

const emptyForm: CustomerRequest = {
  document: '',
  name: '',
  phone: '',
  email: '',
  address: '',
  dataConsent: true,
  customerType: 'NUEVO',
}

type RiskLevel = 'verde' | 'amarillo' | 'rojo' | 'neutro'

function riskFor(
  customer: Customer,
  checks: BackgroundCheckRow[],
  debts: Debt[],
): { level: RiskLevel; label: string; detail: string } {
  const openDebts = debts.filter(
    (d) => d.customerId === customer.id && (d.status === 'ABIERTA' || d.status === 'VENCIDA' || d.status === 'PARCIAL'),
  )
  const debtSum = openDebts.reduce((s, d) => s + Number(d.balance || 0), 0)
  const lastCheck = checks
    .filter((c) => c.document === customer.document || c.customer?.id === customer.id)
    .sort((a, b) => String(b.checkedAt || '').localeCompare(String(a.checkedAt || '')))[0]
  const score = Number(customer.reputationScore ?? 50)

  if (debtSum > 0 || (lastCheck?.result || '').toLowerCase().includes('riesgo')) {
    return {
      level: 'rojo',
      label: 'Revisar',
      detail: debtSum > 0 ? `Cartera ${formatMoney(debtSum)}` : 'Antecedente con observación',
    }
  }
  if (!lastCheck || score < 45) {
    return {
      level: 'amarillo',
      label: 'Precaución',
      detail: !lastCheck ? 'Sin consulta de antecedentes' : `Reputación ${score}`,
    }
  }
  if (score >= 70 && customer.customerType === 'CONFIANZA') {
    return { level: 'verde', label: 'Confianza', detail: `Reputación ${score} · consulta OK` }
  }
  return { level: 'verde', label: 'OK', detail: lastCheck ? `Última consulta ${formatDate(lastCheck.checkedAt)}` : `Reputación ${score}` }
}

function pulseFor(level: RiskLevel) {
  if (level === 'rojo') return { s: 'vencido' as const, l: 'Revisar' }
  if (level === 'amarillo') return { s: 'en_calle' as const, l: 'Precaución' }
  if (level === 'verde') return { s: 'ok' as const, l: 'OK' }
  return { s: 'muted' as const, l: '—' }
}

export default function ClientesPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState('')
  const [form, setForm] = useState<CustomerRequest>(emptyForm)
  const [showForm, setShowForm] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [observations, setObservations] = useState('')
  const [evidenceId, setEvidenceId] = useState<number | null>(null)

  const panel = params.get('panel') || ''
  const focus = params.get('focus')

  const { data: customers = [], isLoading } = useQuery({
    queryKey: ['clientes', q],
    queryFn: async () => {
      const { data } = await api.get<Customer[]>('/clientes', { params: q ? { q } : undefined })
      return data
    },
  })

  const checksQuery = useQuery({
    queryKey: ['antecedentes'],
    queryFn: async () => {
      const { data } = await api.get<BackgroundCheckRow[]>('/antecedentes')
      return data
    },
  })

  const debtsQuery = useQuery({
    queryKey: ['finanzas', 'deudas'],
    queryFn: async () => {
      const { data } = await api.get<Debt[]>('/finanzas/deudas')
      return data
    },
  })

  const selected = useMemo(
    () => customers.find((c) => c.id === selectedId) || null,
    [customers, selectedId],
  )

  const rentalsQuery = useQuery({
    queryKey: ['alquileres', 'cliente', selectedId],
    enabled: selectedId != null,
    queryFn: async () => {
      const { data } = await api.get<Rental[]>(`/alquileres/cliente/${selectedId}`)
      return data
    },
  })

  useEffect(() => {
    if (focus) {
      const id = Number(focus)
      if (Number.isFinite(id)) setSelectedId(id)
    }
  }, [focus])

  useEffect(() => {
    if (panel === 'antecedentes' && selected) {
      setEvidenceId(null)
    }
  }, [panel, selected])

  const createMutation = useMutation({
    mutationFn: async (payload: CustomerRequest) => {
      const { data } = await api.post<Customer>('/clientes', payload)
      return data
    },
    onSuccess: (c) => {
      setForm(emptyForm)
      setShowForm(false)
      toast.success('Cliente creado')
      setSelectedId(c.id)
      void qc.invalidateQueries({ queryKey: ['clientes'] })
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, 'No se pudo crear el cliente'))
    },
  })

  const consultMutation = useMutation({
    mutationFn: async () => {
      if (!selected) throw new Error('Sin cliente')
      const { data } = await api.post<BackgroundCheckRow>('/antecedentes/consultar', {
        customerId: selected.id,
        document: selected.document,
      })
      return data
    },
    onSuccess: (row) => {
      toast.success(`Consulta #${row.id} registrada`)
      setEvidenceId(row.id)
      setPanel('antecedentes')
      void qc.invalidateQueries({ queryKey: ['antecedentes'] })
    },
    onError: (err) => toast.error(getErrorMessage(err, 'No se pudo consultar antecedentes')),
  })

  const evidenceMutation = useMutation({
    mutationFn: async (file: File) => {
      if (evidenceId == null) throw new Error('Registra una consulta primero')
      const formData = new FormData()
      formData.append('file', file)
      if (observations) formData.append('observations', observations)
      const { data } = await api.post(`/antecedentes/${evidenceId}/evidencia`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return data
    },
    onSuccess: () => {
      toast.success('Evidencia adjunta')
      setObservations('')
      void qc.invalidateQueries({ queryKey: ['antecedentes'] })
    },
    onError: (err) => toast.error(getErrorMessage(err, 'No se pudo adjuntar evidencia')),
  })

  const customerChecks = (checksQuery.data || []).filter(
    (c) => selected && (c.document === selected.document || c.customer?.id === selected.id),
  )
  const customerDebts = (debtsQuery.data || []).filter((d) => selected && d.customerId === selected.id)
  const risk = selected
    ? riskFor(selected, checksQuery.data || [], debtsQuery.data || [])
    : null

  function setPanel(id: string) {
    const next = new URLSearchParams(params)
    if (!id) next.delete('panel')
    else next.set('panel', id)
    setParams(next, { replace: true })
  }

  function openCustomer(id: number) {
    setSelectedId(id)
    const next = new URLSearchParams(params)
    next.set('focus', String(id))
    setParams(next, { replace: true })
  }

  function closeDrawer() {
    setSelectedId(null)
    const next = new URLSearchParams(params)
    next.delete('focus')
    setParams(next, { replace: true })
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    createMutation.mutate(form)
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Relaciones"
        title="Clientes"
        subtitle="Ficha 360 · reputación, antecedentes, cartera e historial"
        actions={
          <Button variant="primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cerrar formulario' : 'Nuevo cliente'}
          </Button>
        }
      />

      {showForm && (
        <Panel title="Registrar cliente" className="anim-panel">
          <form onSubmit={handleSubmit} className="p-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="Documento" value={form.document} onChange={(v) => setForm({ ...form, document: v })} required />
            <Field label="Nombre" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required />
            <Field label="Teléfono" value={form.phone || ''} onChange={(v) => setForm({ ...form, phone: v })} />
            <Field label="Email" value={form.email || ''} onChange={(v) => setForm({ ...form, email: v })} />
            <Field label="Dirección" value={form.address || ''} onChange={(v) => setForm({ ...form, address: v })} />
            <label className="block space-y-1.5">
              <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">Tipo</span>
              <select
                className="field-input"
                value={form.customerType}
                onChange={(e) => setForm({ ...form, customerType: e.target.value as CustomerType })}
              >
                <option value="NUEVO">Nuevo</option>
                <option value="FRECUENTE">Frecuente</option>
                <option value="CONFIANZA">Confianza</option>
              </select>
            </label>
            <label className="flex items-center gap-2 text-sm text-[var(--text-muted)] sm:col-span-2">
              <input
                type="checkbox"
                checked={form.dataConsent}
                onChange={(e) => setForm({ ...form, dataConsent: e.target.checked })}
              />
              Consentimiento de datos personales
            </label>
            <div className="sm:col-span-2 lg:col-span-3">
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Guardando…' : 'Crear cliente'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      <section className="panel overflow-hidden">
        <div className="px-4 py-3 border-b border-[var(--border)] flex items-center gap-2">
          <input
            className="field-input !py-2 !text-sm"
            placeholder="Buscar documento o nombre…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        {isLoading ? (
          <SkeletonRows rows={6} />
        ) : customers.length === 0 ? (
          <EmptyState title="Sin clientes" hint="Registra el primero." />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {customers.map((c) => {
              const r = riskFor(c, checksQuery.data || [], debtsQuery.data || [])
              const p = pulseFor(r.level)
              return (
                <li key={c.id}>
                  <button
                    type="button"
                    onClick={() => openCustomer(c.id)}
                    className="w-full text-left px-5 py-3.5 hover:bg-[#f9f9fb] transition-colors flex items-center justify-between gap-3"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-[var(--navy)] truncate">{c.name}</span>
                        <span className={`risk-dot risk-${r.level}`} title={r.label} />
                        <StatusPulse status={p.s} label={r.label} />
                      </div>
                      <div className="text-xs text-[var(--text-muted)] mt-1">{c.document}</div>
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        <Badge tone={c.customerType === 'CONFIANZA' ? 'accent' : 'muted'}>
                          {c.customerType || 'NUEVO'}
                        </Badge>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[var(--accent)] shrink-0">Ficha →</span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <SlideOver
        open={selectedId != null && selected != null}
        onClose={closeDrawer}
        title={selected?.name || ''}
        subtitle="Ficha 360"
        width={520}
      >
        {selected && (
          <div className="flex flex-col h-full">
            <div className="px-5 py-4 border-b border-[var(--border)] flex flex-col gap-3">
              <p className="text-sm text-[var(--text-muted)]">
                {selected.document}
                {selected.phone ? ` · ${selected.phone}` : ''}
                {selected.email ? ` · ${selected.email}` : ''}
              </p>
              {risk && (
                <div className={`risk-banner risk-banner-${risk.level}`}>
                  <div className="text-[0.65rem] uppercase tracking-wider font-bold opacity-80">Semáforo</div>
                  <div className="font-display text-2xl uppercase mt-0.5">{risk.label}</div>
                  <div className="text-xs mt-1 opacity-90">{risk.detail}</div>
                </div>
              )}
            </div>

            <div className="px-5 pt-4">
              <ModuleTabs
                value={panel}
                onChange={setPanel}
                tabs={[
                  { id: '', label: 'Resumen' },
                  { id: 'antecedentes', label: 'Antecedentes' },
                  { id: 'historial', label: 'Alquileres' },
                ]}
              />
            </div>

            <div className="p-5 space-y-5 flex-1 overflow-y-auto">
              {(!panel || panel === '') && (
                <>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <Meta label="Tipo" value={selected.customerType || '—'} />
                    <Meta label="Reputación" value={String(selected.reputationScore ?? 50)} />
                    <Meta label="Consentimiento" value={selected.dataConsent ? 'Sí' : 'No'} />
                  </div>
                  {selected.address && (
                    <p className="text-sm text-[var(--text-muted)]">
                      <strong className="text-[var(--navy)]">Dirección:</strong> {selected.address}
                    </p>
                  )}
                  <div>
                    <h3 className="font-display text-base text-[var(--navy)] uppercase mb-2">Cartera</h3>
                    {customerDebts.length === 0 ? (
                      <p className="text-sm text-[var(--text-muted)]">Sin deudas abiertas.</p>
                    ) : (
                      <ul className="dense-list">
                        {customerDebts.map((d) => (
                          <li key={d.id}>
                            <span>
                              #{d.rentalId ?? d.id} · {d.status}
                            </span>
                            <strong>{formatMoney(d.balance)}</strong>
                          </li>
                        ))}
                      </ul>
                    )}
                    <Link to="/finanzas" className="inline-block mt-2 text-xs font-semibold text-[var(--accent)]">
                      Ir a Caja →
                    </Link>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button variant="navy" onClick={() => setPanel('antecedentes')}>
                      Ver antecedentes
                    </Button>
                    <Link to="/alquileres/nuevo" className="btn btn-primary">
                      Alquilar a este cliente
                    </Link>
                  </div>
                </>
              )}

              {panel === 'antecedentes' && (
                <div className="space-y-4">
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="primary"
                      disabled={consultMutation.isPending || !selected.dataConsent}
                      onClick={() => consultMutation.mutate()}
                    >
                      {consultMutation.isPending ? 'Consultando…' : 'Registrar consulta'}
                    </Button>
                    {!selected.dataConsent && (
                      <span className="text-xs text-[var(--danger)] self-center">
                        Requiere consentimiento de datos
                      </span>
                    )}
                  </div>

                  <div className="rounded-lg border border-[var(--border)] p-4 space-y-3 bg-[var(--bone)]">
                    <h3 className="font-display text-base text-[var(--navy)] uppercase">Evidencia PDF</h3>
                    <p className="text-xs text-[var(--text-muted)]">
                      Consulta activa: {evidenceId != null ? `#${evidenceId}` : 'ninguna — registra primero'}
                    </p>
                    <textarea
                      className="field-input min-h-16"
                      placeholder="Observaciones"
                      value={observations}
                      onChange={(e) => setObservations(e.target.value)}
                    />
                    <input
                      type="file"
                      accept="application/pdf,image/*"
                      disabled={evidenceId == null}
                      onChange={(e) => {
                        const file = e.target.files?.[0]
                        if (file) evidenceMutation.mutate(file)
                      }}
                    />
                  </div>

                  {customerChecks.length === 0 ? (
                    <EmptyState title="Sin consultas" hint="Registra la primera verificación aquí." />
                  ) : (
                    <ul className="dense-list">
                      {customerChecks.map((row) => (
                        <li key={row.id}>
                          <button
                            type="button"
                            className="text-left"
                            onClick={() => setEvidenceId(row.id)}
                          >
                            <span className="font-semibold text-[var(--navy)]">#{row.id}</span>
                            <span className="text-[var(--text-muted)]"> · {row.provider || 'manual'}</span>
                            <div className="text-xs text-[var(--text-muted)] truncate max-w-md">
                              {row.result || 'Sin resultado'} · {formatDate(row.checkedAt)}
                            </div>
                          </button>
                          <Badge tone="navy">{row.evidencePath ? 'Con PDF' : 'Pendiente'}</Badge>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              {panel === 'historial' && (
                <div>
                  {rentalsQuery.isLoading ? (
                    <SkeletonRows rows={4} />
                  ) : (rentalsQuery.data || []).length === 0 ? (
                    <EmptyState title="Sin alquileres" hint="Este cliente aún no tiene historial." />
                  ) : (
                    <ul className="dense-list">
                      {(rentalsQuery.data || []).map((r) => (
                        <li key={r.id}>
                          <span>
                            <strong className="text-[var(--navy)]">#{r.id}</strong> · {r.status} ·{' '}
                            {formatDate(r.startDate)}
                          </span>
                          <strong>{formatMoney(r.finalAmount ?? r.totalAmount)}</strong>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </SlideOver>
    </div>
  )
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[var(--border)] px-3 py-2.5">
      <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold">{label}</div>
      <div className="font-semibold text-[var(--navy)] mt-0.5">{value}</div>
    </div>
  )
}

function Field({
  label,
  value,
  onChange,
  required,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  required?: boolean
}) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">{label}</span>
      <input className="field-input" value={value} onChange={(e) => onChange(e.target.value)} required={required} />
    </label>
  )
}
