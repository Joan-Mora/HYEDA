import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SkeletonRows } from '../../components/Motion'
import { SlideOver } from '../../components/SlideOver'
import { useToast } from '../../components/Toast'
import {
  Badge,
  Button,
  EmptyState,
  PageHeader,
  Panel,
  StockBar,
} from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type { Product, ProductRequest } from '../../types'
import { formatMoney } from '../../utils/format'

const emptyForm: ProductRequest = {
  name: '',
  description: '',
  dailyPrice: 0,
  stockTotal: 1,
  minStockThreshold: 1,
  active: true,
}

export default function InventarioPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState('')
  const [form, setForm] = useState<ProductRequest>(emptyForm)
  const [showForm, setShowForm] = useState(false)
  const [drawerId, setDrawerId] = useState<number | null>(null)

  const focus = params.get('focus')

  const { data: products = [], isLoading } = useQuery({
    queryKey: ['productos', q],
    queryFn: async () => {
      const { data } = await api.get<Product[]>('/productos', { params: q ? { q } : undefined })
      return data
    },
  })

  const selected = useMemo(
    () => products.find((p) => p.id === drawerId) || null,
    [products, drawerId],
  )

  useEffect(() => {
    if (focus) {
      const id = Number(focus)
      if (Number.isFinite(id)) setDrawerId(id)
    }
  }, [focus])

  const lowCount = products.filter((p) => p.lowStock).length
  const inUse = products.reduce((s, p) => s + Math.max(0, p.stockTotal - p.stockAvailable), 0)

  const createMutation = useMutation({
    mutationFn: async (payload: ProductRequest) => {
      const { data } = await api.post<Product>('/productos', payload)
      return data
    },
    onSuccess: (p) => {
      setForm(emptyForm)
      setShowForm(false)
      toast.success('Equipo registrado en el patio')
      setDrawerId(p.id)
      void qc.invalidateQueries({ queryKey: ['productos'] })
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, 'No se pudo crear el producto'))
    },
  })

  function openProduct(id: number) {
    setDrawerId(id)
    const next = new URLSearchParams(params)
    next.set('focus', String(id))
    setParams(next, { replace: true })
  }

  function closeDrawer() {
    setDrawerId(null)
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
        eyebrow="Bodega"
        title="Patio"
        subtitle="Stock real: qué hay, qué está en calle y qué está bajo el mínimo."
        actions={
          <Button variant="primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cerrar' : 'Nuevo equipo'}
          </Button>
        }
      />

      <div className="flex flex-wrap gap-3">
        <div className="panel px-4 py-3 min-w-[120px]">
          <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold">
            Ítems
          </div>
          <div className="font-display text-2xl text-[var(--navy)]">{products.length}</div>
        </div>
        <div className="panel px-4 py-3 min-w-[120px]">
          <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold">
            En calle
          </div>
          <div className="font-display text-2xl text-[var(--accent)]">{inUse}</div>
        </div>
        <div className="panel px-4 py-3 min-w-[120px]">
          <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold">
            Stock bajo
          </div>
          <div
            className="font-display text-2xl"
            style={{ color: lowCount > 0 ? 'var(--danger)' : 'var(--navy)' }}
          >
            {lowCount}
          </div>
        </div>
        <div className="flex-1 min-w-[180px]">
          <input
            className="field-input h-full"
            placeholder="Buscar equipo…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
      </div>

      {showForm && (
        <Panel title="Registrar equipo" className="anim-panel">
          <form onSubmit={handleSubmit} className="p-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <label className="block space-y-1.5 lg:col-span-2">
              <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                Nombre
              </span>
              <input
                className="field-input"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                Precio/día
              </span>
              <input
                type="number"
                min={0}
                className="field-input"
                value={form.dailyPrice}
                onChange={(e) => setForm({ ...form, dailyPrice: Number(e.target.value) })}
                required
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                Stock
              </span>
              <input
                type="number"
                min={0}
                className="field-input"
                value={form.stockTotal}
                onChange={(e) => setForm({ ...form, stockTotal: Number(e.target.value) })}
                required
              />
            </label>
            <label className="block space-y-1.5 sm:col-span-2 lg:col-span-4">
              <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                Descripción
              </span>
              <textarea
                className="field-input min-h-16"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>
            <div className="lg:col-span-4">
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Guardando…' : 'Guardar en patio'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      <section className="panel overflow-hidden">
        {isLoading ? (
          <SkeletonRows rows={6} />
        ) : products.length === 0 ? (
          <EmptyState
            title="Patio vacío"
            hint="Importa la lista de precios o registra el primer equipo."
            action={
              <Button variant="primary" onClick={() => setShowForm(true)}>
                Nuevo equipo
              </Button>
            }
          />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {products.map((p) => (
              <li key={p.id}>
                <button
                  type="button"
                  onClick={() => openProduct(p.id)}
                  className="w-full text-left px-5 py-4 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-6 hover:bg-[#f9f9fb]"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-semibold text-[var(--navy)]">{p.name}</span>
                      {p.lowStock && <Badge tone="accent">Bajo</Badge>}
                      <Badge tone={p.active ? 'success' : 'muted'}>
                        {p.active ? 'Activo' : 'Inactivo'}
                      </Badge>
                    </div>
                    {p.description && (
                      <p className="text-xs text-[var(--text-muted)] mt-1 truncate">{p.description}</p>
                    )}
                  </div>
                  <div className="font-display text-lg text-[var(--navy)] shrink-0 w-28">
                    {formatMoney(p.dailyPrice)}
                    <span className="block text-[0.65rem] font-body font-normal text-[var(--text-muted)]">
                      / día
                    </span>
                  </div>
                  <StockBar available={p.stockAvailable} total={p.stockTotal} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <SlideOver
        open={drawerId != null && selected != null}
        onClose={closeDrawer}
        title={selected?.name || ''}
        subtitle="Detalle de equipo"
        width={420}
      >
        {selected && (
          <div className="p-5 space-y-5">
            <div className="flex flex-wrap gap-2">
              {selected.lowStock && <Badge tone="accent">Stock bajo</Badge>}
              <Badge tone={selected.active ? 'success' : 'muted'}>
                {selected.active ? 'Activo' : 'Inactivo'}
              </Badge>
            </div>
            <div>
              <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold">
                Precio / día
              </div>
              <div className="font-display text-3xl text-[var(--navy)] mt-1">
                {formatMoney(selected.dailyPrice)}
              </div>
            </div>
            <div>
              <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold mb-2">
                Stock
              </div>
              <StockBar available={selected.stockAvailable} total={selected.stockTotal} />
              <p className="text-xs text-[var(--text-muted)] mt-2">
                Disponible {selected.stockAvailable} de {selected.stockTotal}
                {selected.minStockThreshold != null
                  ? ` · mínimo ${selected.minStockThreshold}`
                  : ''}
              </p>
            </div>
            {selected.description && (
              <div>
                <div className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] font-bold mb-1">
                  Descripción
                </div>
                <p className="text-sm text-[var(--text-muted)] whitespace-pre-wrap">
                  {selected.description}
                </p>
              </div>
            )}
          </div>
        )}
      </SlideOver>
    </div>
  )
}
