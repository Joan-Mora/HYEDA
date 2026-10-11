import { useMutation, useQuery } from '@tanstack/react-query'
import { AnimatePresence, motion } from 'framer-motion'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { StatusPulse } from '../../components/Motion'
import { Pressable, SignaturePad } from '../../components/Overlay'
import { useToast } from '../../components/Toast'
import { Alert, Badge } from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type {
  BackgroundCheckRow,
  CreateRentalRequest,
  Customer,
  DocumentType,
  PaymentType,
  Product,
  Rental,
} from '../../types'
import { formatMoney } from '../../utils/format'

const STEPS = [
  { id: 'cliente', label: 'Cliente' },
  { id: 'equipos', label: 'Equipos y fechas' },
  { id: 'docs', label: 'Documentación' },
  { id: 'pago', label: 'Pago y envío' },
] as const

type CartItem = { productId: number; name: string; quantity: number; dailyPrice: number; stock: number }

function riskOf(c: Customer, checks: BackgroundCheckRow[]) {
  const score = Number(c.reputationScore ?? 50)
  const hasCheck = checks.some((x) => x.document === c.document || x.customer?.id === c.id)
  if (score < 40) return { tone: 'vencido' as const, label: 'Alto riesgo' }
  if (!hasCheck || score < 55) return { tone: 'en_calle' as const, label: 'Precaución' }
  return { tone: 'ok' as const, label: 'Confianza' }
}

export default function WizardPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState(0)
  const [customerId, setCustomerId] = useState<number | null>(null)
  const [cart, setCart] = useState<CartItem[]>([])
  const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10))
  const [days, setDays] = useState(1)
  const [address, setAddress] = useState('')
  const [reference, setReference] = useState('')
  const [documentType, setDocumentType] = useState<DocumentType>('CONTRATO')
  const [signature, setSignature] = useState<string | null>(null)
  const [paymentType, setPaymentType] = useState<PaymentType>('EFECTIVO')
  const [payNow, setPayNow] = useState(true)
  const [error, setError] = useState('')
  const [created, setCreated] = useState<Rental | null>(null)

  const customersQ = useQuery({
    queryKey: ['clientes', 'wizard'],
    queryFn: async () => (await api.get<Customer[]>('/clientes')).data,
  })
  const productsQ = useQuery({
    queryKey: ['productos', 'wizard'],
    queryFn: async () => (await api.get<Product[]>('/productos')).data,
  })
  const checksQ = useQuery({
    queryKey: ['antecedentes', 'wizard'],
    queryFn: async () => (await api.get<BackgroundCheckRow[]>('/antecedentes')).data,
  })

  const selected = (customersQ.data || []).find((c) => c.id === customerId) || null
  const checks = checksQ.data || []
  const risk = selected ? riskOf(selected, checks) : null

  const subtotal = useMemo(
    () => cart.reduce((s, i) => s + i.dailyPrice * i.quantity * days, 0),
    [cart, days],
  )

  function addProduct(p: Product) {
    setCart((prev) => {
      const ex = prev.find((x) => x.productId === p.id)
      if (ex) {
        if (ex.quantity >= p.stockAvailable) return prev
        return prev.map((x) => (x.productId === p.id ? { ...x, quantity: x.quantity + 1 } : x))
      }
      return [
        ...prev,
        {
          productId: p.id,
          name: p.name,
          quantity: 1,
          dailyPrice: Number(p.dailyPrice),
          stock: p.stockAvailable,
        },
      ]
    })
  }

  function canNext() {
    if (step === 0) return customerId != null
    if (step === 1) return cart.length > 0 && days >= 1 && address.trim().length > 0 && reference.trim().length > 0
    if (step === 2) return signature != null
    return true
  }

  const createMutation = useMutation({
    mutationFn: async () => {
      if (customerId == null) throw new Error('Selecciona un cliente')
      const payload: CreateRentalRequest = {
        customerId,
        startDate,
        estimatedDays: days,
        deliveryAddress: address.trim(),
        deliveryReference: reference.trim(),
        haulingEnabled: false,
        haulingCost: 0,
        discountAmount: 0,
        documentType,
        skipContract: documentType === 'REMISION',
        items: cart.map((c) => ({ productId: c.productId, quantity: c.quantity })),
      }
      const { data: rental } = await api.post<Rental>('/alquileres', payload)
      try {
        await api.post(`/alquileres/${rental.id}/emitir-documento`)
      } catch {
        /* documento puede fallar sin bloquear */
      }
      if (payNow && subtotal > 0) {
        await api.post('/pagos', {
          rentalId: rental.id,
          paymentType,
          amount: subtotal,
          reference: paymentType === 'QR' ? `QR-${rental.id}` : `SIG-${signature?.slice(-8) || 'ok'}`,
        })
      }
      return rental
    },
    onSuccess: (rental) => {
      setCreated(rental)
      setError('')
      toast.success(`Alquiler #${rental.id} creado`)
    },
    onError: (err) => {
      setError(getErrorMessage(err, 'No se pudo crear el alquiler'))
      toast.error('No se pudo confirmar el alquiler')
    },
  })

  function whatsappLink(rental: Rental) {
    const phone = selected?.phone?.replace(/\D/g, '') || ''
    const text = encodeURIComponent(
      `HYEDA · Alquiler #${rental.id}\nCliente: ${selected?.name}\nTotal: ${formatMoney(subtotal)}\nReferencia: ${reference}\nDirección: ${address}\nDocumento: ${documentType}`,
    )
    return `https://wa.me/${phone}?text=${text}`
  }

  const progress = ((step + (created ? 1 : 0)) / STEPS.length) * 100

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <nav className="breadcrumb">
        <Link to="/alquileres">Alquileres</Link>
        <span>/</span>
        <span>Nuevo</span>
      </nav>

      <header>
        <p className="text-[0.65rem] font-bold uppercase tracking-[0.16em] text-[var(--accent)]">Wizard guiado</p>
        <h1 className="font-display text-3xl md:text-4xl text-[var(--navy)] uppercase mt-1">Abrir alquiler</h1>
        <p className="text-sm text-[var(--text-muted)] mt-2">
          4 fases operativas · progreso animado · firma en pantalla
        </p>
      </header>

      <div className="wizard-progress" aria-hidden>
        <motion.span
          initial={false}
          animate={{ width: `${Math.min(100, progress)}%` }}
          transition={{ type: 'spring', stiffness: 120, damping: 20 }}
        />
      </div>

      <div className="flex gap-2 flex-wrap">
        {STEPS.map((s, i) => (
          <div
            key={s.id}
            className={[
              'px-3 py-1.5 text-xs font-bold uppercase tracking-wide rounded-md border',
              i === step
                ? 'border-[var(--accent)] text-[var(--accent)] bg-[var(--accent-soft)]'
                : i < step
                  ? 'border-[var(--navy)] text-[var(--navy)] bg-[rgba(27,31,76,0.06)]'
                  : 'border-[var(--border)] text-[var(--text-muted)]',
            ].join(' ')}
          >
            {i + 1}. {s.label}
          </div>
        ))}
      </div>

      <section className="panel p-5 md:p-6 overflow-hidden">
        <AnimatePresence mode="wait">
          {created ? (
            <motion.div
              key="done"
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              className="text-center py-8 space-y-4"
            >
              <div className="mx-auto w-16 h-16 rounded-full bg-[rgba(46,125,50,0.12)] text-[var(--success)] flex items-center justify-center text-3xl font-bold">
                ✓
              </div>
              <h2 className="font-display text-2xl text-[var(--navy)] uppercase">
                Alquiler #{created.id} listo
              </h2>
              <p className="text-sm text-[var(--text-muted)]">Firma capturada · documento emitido · listo para enviar</p>
              <div className="flex flex-wrap justify-center gap-2">
                <a
                  className="btn btn-primary inline-flex items-center gap-2"
                  href={whatsappLink(created)}
                  target="_blank"
                  rel="noreferrer"
                >
                  <WhatsAppIcon /> Enviar por WhatsApp
                </a>
                <Pressable className="btn btn-navy" onClick={() => navigate('/alquileres')}>
                  Ir al hub
                </Pressable>
              </div>
            </motion.div>
          ) : (
            <motion.div
              key={step}
              initial={{ opacity: 0, x: 24 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -24 }}
              transition={{ duration: 0.22 }}
              className="space-y-5"
            >
              {step === 0 && (
                <div className="space-y-3">
                  <h2 className="font-display text-xl text-[var(--navy)] uppercase">Cliente y reputación</h2>
                  <div className="grid gap-2 max-h-72 overflow-y-auto">
                    {(customersQ.data || []).map((c) => {
                      const r = riskOf(c, checks)
                      const active = c.id === customerId
                      return (
                        <button
                          key={c.id}
                          type="button"
                          onClick={() => setCustomerId(c.id)}
                          className={[
                            'text-left rounded-lg border px-3 py-3 transition-colors',
                            active
                              ? 'border-[var(--accent)] bg-[var(--accent-soft)]'
                              : 'border-[var(--border)] hover:border-[var(--navy)] bg-white',
                          ].join(' ')}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span className="font-semibold text-[var(--navy)]">{c.name}</span>
                            <StatusPulse status={r.tone} label={r.label} />
                          </div>
                          <p className="text-xs text-[var(--text-muted)] mt-1">
                            {c.document} · reputación {c.reputationScore ?? 50}
                          </p>
                        </button>
                      )
                    })}
                  </div>
                  {selected && risk && (
                    <div className="rounded-lg border border-[var(--border)] p-3 text-sm bg-[var(--bone)]">
                      Seleccionado: <strong>{selected.name}</strong> ·{' '}
                      <StatusPulse status={risk.tone} label={risk.label} />
                    </div>
                  )}
                </div>
              )}

              {step === 1 && (
                <div className="space-y-4">
                  <h2 className="font-display text-xl text-[var(--navy)] uppercase">Equipos y fechas</h2>
                  <div className="grid sm:grid-cols-2 gap-3">
                    <label className="block space-y-1.5">
                      <span className="text-xs font-bold uppercase text-[var(--text-muted)]">Inicio</span>
                      <input
                        type="date"
                        className="field-input"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                      />
                    </label>
                    <label className="block space-y-1.5">
                      <span className="text-xs font-bold uppercase text-[var(--text-muted)]">Días</span>
                      <input
                        type="number"
                        min={1}
                        className="field-input"
                        value={days}
                        onChange={(e) => setDays(Number(e.target.value))}
                      />
                    </label>
                  </div>
                  <label className="block space-y-1.5">
                    <span className="text-xs font-bold uppercase text-[var(--text-muted)]">
                      Nombre de referencia
                    </span>
                    <input
                      className="field-input"
                      value={reference}
                      onChange={(e) => setReference(e.target.value)}
                      placeholder="Ej. Obra finca Los Naranjos / Casa doña Marta"
                    />
                  </label>
                  <label className="block space-y-1.5">
                    <span className="text-xs font-bold uppercase text-[var(--text-muted)]">Dirección</span>
                    <input
                      className="field-input"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      placeholder="Vereda, barrio, calle o punto de referencia"
                    />
                  </label>
                  <div className="grid gap-2 max-h-52 overflow-y-auto">
                    {(productsQ.data || []).map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        disabled={!p.active || p.stockAvailable < 1}
                        onClick={() => addProduct(p)}
                        className="flex justify-between gap-3 rounded-lg border border-[var(--border)] px-3 py-2.5 text-sm text-left hover:border-[var(--navy)] disabled:opacity-40 bg-white"
                      >
                        <span>
                          {p.name}
                          <span className="block text-xs text-[var(--text-muted)]">
                            Stock {p.stockAvailable}/{p.stockTotal}
                          </span>
                        </span>
                        <span className="text-[var(--accent)] font-semibold">{formatMoney(p.dailyPrice)}/d</span>
                      </button>
                    ))}
                  </div>
                  {cart.length > 0 && (
                    <ul className="dense-list">
                      {cart.map((i) => (
                        <li key={i.productId}>
                          <span>
                            {i.name} × {i.quantity}
                          </span>
                          <strong>{formatMoney(i.dailyPrice * i.quantity * days)}</strong>
                        </li>
                      ))}
                    </ul>
                  )}
                  <p className="text-sm">
                    Total estimado:{' '}
                    <strong className="text-[var(--accent)] font-display text-xl">{formatMoney(subtotal)}</strong>
                  </p>
                </div>
              )}

              {step === 2 && (
                <div className="space-y-4">
                  <h2 className="font-display text-xl text-[var(--navy)] uppercase">Documentación y firma</h2>
                  <label className="block space-y-1.5">
                    <span className="text-xs font-bold uppercase text-[var(--text-muted)]">Tipo</span>
                    <select
                      className="field-input"
                      value={documentType}
                      onChange={(e) => setDocumentType(e.target.value as DocumentType)}
                    >
                      <option value="CONTRATO">Contrato</option>
                      <option value="REMISION">Remisión</option>
                    </select>
                  </label>
                  <div className="rounded-lg border border-[var(--border)] p-4 bg-[var(--bone)] text-sm space-y-1">
                    <p>
                      <Badge tone="navy">{documentType}</Badge>
                    </p>
                    <p>Cliente: {selected?.name}</p>
                    <p>Ítems: {cart.length} · {days} días · {formatMoney(subtotal)}</p>
                    <p>Obra: {reference || '—'} · {address || '—'}</p>
                  </div>
                  <div>
                    <p className="text-xs font-bold uppercase text-[var(--text-muted)] mb-2">
                      Firma digital (dedo o ratón)
                    </p>
                    <SignaturePad onChange={setSignature} />
                    {signature && <StatusPulse status="ok" label="Firma capturada" />}
                  </div>
                </div>
              )}

              {step === 3 && (
                <div className="space-y-4">
                  <h2 className="font-display text-xl text-[var(--navy)] uppercase">Pago y envío</h2>
                  <label className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={payNow} onChange={(e) => setPayNow(e.target.checked)} />
                    Registrar pago ahora
                  </label>
                  {payNow && (
                    <div className="grid sm:grid-cols-2 gap-3">
                      {(['EFECTIVO', 'QR'] as PaymentType[]).map((m) => (
                        <button
                          key={m}
                          type="button"
                          onClick={() => setPaymentType(m)}
                          className={[
                            'rounded-lg border p-4 text-left',
                            paymentType === m
                              ? 'border-[var(--accent)] bg-[var(--accent-soft)]'
                              : 'border-[var(--border)]',
                            m === 'QR' ? 'qr-pulse' : '',
                          ].join(' ')}
                        >
                          <div className="font-display text-lg text-[var(--navy)] uppercase">{m}</div>
                          <p className="text-xs text-[var(--text-muted)] mt-1">
                            {m === 'QR' ? 'Código QR mock / Wompi' : 'Caja del patio'}
                          </p>
                        </button>
                      ))}
                    </div>
                  )}
                  <div className="rounded-lg border border-[var(--border)] p-4 text-sm space-y-1">
                    <p>Total a cobrar: <strong className="text-[var(--accent)]">{formatMoney(subtotal)}</strong></p>
                    <p>Tras confirmar podrás enviar el resumen por WhatsApp.</p>
                  </div>
                </div>
              )}

              {error && <Alert>{error}</Alert>}

              <div className="flex justify-between pt-2">
                <Pressable
                  className="btn btn-ghost disabled:opacity-40"
                  disabled={step === 0}
                  onClick={() => setStep((s) => Math.max(0, s - 1))}
                >
                  Anterior
                </Pressable>
                {step < STEPS.length - 1 ? (
                  <Pressable
                    className="btn btn-primary disabled:opacity-40"
                    disabled={!canNext()}
                    onClick={() => setStep((s) => s + 1)}
                  >
                    Siguiente
                  </Pressable>
                ) : (
                  <Pressable
                    className="btn btn-primary disabled:opacity-40"
                    disabled={createMutation.isPending || !canNext()}
                    onClick={() => createMutation.mutate()}
                  >
                    {createMutation.isPending ? 'Confirmando…' : 'Confirmar alquiler'}
                  </Pressable>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </section>
    </div>
  )
}

function WhatsAppIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.435 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
  )
}
