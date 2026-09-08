import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useRef, useState, type MouseEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, PageHeader, Panel, Select, Stepper } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api } from '../../services/api'
import type { CustomerView, PageResponse, ProductView, QuoteView, RentalEligibility, RentalView } from '../../types/api'
import { getErrorMessage, money, plusDaysIso, todayIso } from '../../lib/format'

const steps = ['Cliente', 'Equipos y obra', 'Documento', 'Pago']

export function WizardPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [step, setStep] = useState(0)
  const [q, setQ] = useState('')
  const [customer, setCustomer] = useState<CustomerView | null>(null)
  const [eligibility, setEligibility] = useState<RentalEligibility | null>(null)
  const [startDate, setStartDate] = useState(todayIso())
  const [endDate, setEndDate] = useState(plusDaysIso(2))
  const [address, setAddress] = useState('')
  const [reference, setReference] = useState('')
  const [hauling, setHauling] = useState(0)
  const [discount, setDiscount] = useState(0)
  const [discountReason, setDiscountReason] = useState('')
  const [policy, setPolicy] = useState('CONTRATO')
  const [items, setItems] = useState<{ productId: number; name: string; quantity: number }[]>([])
  const [created, setCreated] = useState<RentalView | null>(null)

  const customers = useQuery({
    queryKey: ['wizard-clientes', q],
    queryFn: async () => (await api.get<PageResponse<CustomerView>>('/clientes', { params: { q, size: 8 } })).data,
    enabled: q.length > 1,
  })
  const products = useQuery({
    queryKey: ['wizard-productos'],
    queryFn: async () => (await api.get<PageResponse<ProductView>>('/inventario', { params: { size: 50, activo: true } })).data,
  })
  const quote = useQuery({
    queryKey: ['cotizar', startDate, endDate, items, hauling, discount],
    queryFn: async () => (await api.post<QuoteView>('/alquileres/cotizar', {
      startDate, endDate, items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
      haulingCost: hauling, discountAmount: discount,
    })).data,
    enabled: items.length > 0,
  })

  const total = quote.data?.total ?? 0
  const sessionKey = useMemo(() => crypto.randomUUID(), [])

  function drawStart(e: MouseEvent<HTMLCanvasElement>) {
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx || !canvasRef.current) return
    const rect = canvasRef.current.getBoundingClientRect()
    ctx.strokeStyle = '#1B1F4C'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top)
    const move = (ev: globalThis.MouseEvent) => {
      ctx.lineTo(ev.clientX - rect.left, ev.clientY - rect.top)
      ctx.stroke()
    }
    const up = () => {
      window.removeEventListener('mousemove', move)
      window.removeEventListener('mouseup', up)
    }
    window.addEventListener('mousemove', move)
    window.addEventListener('mouseup', up)
  }

  const finish = useMutation({
    mutationFn: async (mode: 'efectivo' | 'qr' | 'pendiente') => {
      if (!customer) throw new Error('Selecciona un cliente')
      const { data } = await api.post<RentalView>('/alquileres', {
        customerId: customer.id, startDate, endDate,
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        deliveryAddress: address, deliveryReference: reference,
        haulingEnabled: hauling > 0, haulingCost: hauling,
        discountAmount: discount, discountReason: discount ? discountReason : null,
        documentPolicy: policy, dataConsentAccepted: true, sessionKey, notes: '',
      })
      const canvas = canvasRef.current
      if (canvas) {
        const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
        if (blob) {
          const form = new FormData()
          form.append('file', blob, 'firma.png')
          await api.post(`/alquileres/${data.id}/firma`, form)
        }
      }
      await api.post(`/documentos/alquiler/${data.id}/emitir`)
      if (mode === 'efectivo') {
        await api.post(`/pagos/alquiler/${data.id}/efectivo`, { amount: data.currentAmount, amountTendered: data.currentAmount })
      } else if (mode === 'qr') {
        await api.post(`/pagos/alquiler/${data.id}/qr`, { amount: data.currentAmount })
      } else {
        await api.post(`/pagos/alquiler/${data.id}/sin-pago`)
      }
      await api.post(`/notificaciones/alquiler/${data.id}/resumen`).catch(() => undefined)
      return data
    },
    onSuccess: (data) => {
      setCreated(data)
      toast.push('Alquiler abierto')
    },
    onError: (error) => toast.push(getErrorMessage(error), 'error'),
  })

  if (created) {
    return (
      <Panel title="Alquiler abierto">
        <p className="mb-4">{created.code} para {created.customerName}. Total {money(created.currentAmount)}.</p>
        <Button onClick={() => navigate('/alquileres')}>Ir al hub</Button>
      </Panel>
    )
  }

  return (
    <div>
      <PageHeader eyebrow="Wizard" title="Abrir alquiler" subtitle="Un paso a la vez: cliente, equipos, documento y pago." />
      <Stepper steps={steps} current={step} />
      {step === 0 && (
        <Panel title="Cliente">
          <Input label="Buscar" value={q} onChange={(e) => setQ(e.target.value)} />
          <div className="mt-3 space-y-2">
            {(customers.data?.content ?? []).map((c) => (
              <button key={c.id} className="block w-full rounded-lg bg-slate-50 px-3 py-2 text-left" onClick={async () => {
                setCustomer(c)
                const { data } = await api.get<RentalEligibility>(`/alquileres/elegibilidad/${c.id}`)
                setEligibility(data)
                if (data.canSkipContract) setPolicy(data.suggestedPolicy)
              }}>
                {c.name} · {c.document}
              </button>
            ))}
          </div>
          {eligibility && (
            <div className="mt-4 text-sm">
              <p>{eligibility.customerName} · {eligibility.customerType} · reputacion {eligibility.reputationScore}</p>
              {eligibility.warnings.map((w) => <p key={w} className="text-red-700">{w}</p>)}
            </div>
          )}
          <Button className="mt-4" disabled={!customer} onClick={() => setStep(1)}>Continuar</Button>
        </Panel>
      )}
      {step === 1 && (
        <Panel title="Equipos y obra">
          <div className="grid gap-3 md:grid-cols-2">
            <Input label="Entrega" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            <Input label="Devolucion" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
            <Input label="Direccion de obra" value={address} onChange={(e) => setAddress(e.target.value)} />
            <Input label="Referencia" value={reference} onChange={(e) => setReference(e.target.value)} />
            <Input label="Acarreo" type="number" value={hauling} onChange={(e) => setHauling(Number(e.target.value))} />
            <Input label="Descuento" type="number" value={discount} onChange={(e) => setDiscount(Number(e.target.value))} />
            {discount > 0 && <Input label="Motivo del descuento" value={discountReason} onChange={(e) => setDiscountReason(e.target.value)} />}
          </div>
          <Select label="Agregar equipo" value="" onChange={(e) => {
            const product = products.data?.content.find((p) => String(p.id) === e.target.value)
            if (product) setItems((current) => [...current, { productId: product.id, name: product.name, quantity: 1 }])
          }}>
            <option value="">Selecciona…</option>
            {(products.data?.content ?? []).map((p) => <option key={p.id} value={p.id}>{p.name} ({p.stockAvailable})</option>)}
          </Select>
          <ul className="my-3 text-sm">
            {items.map((item, i) => (
              <li key={item.productId} className="flex justify-between">
                {item.name}
                <input className="field w-20" type="number" min={1} value={item.quantity} onChange={(e) => {
                  const qty = Number(e.target.value)
                  setItems((current) => current.map((row, idx) => idx === i ? { ...row, quantity: qty } : row))
                }} />
              </li>
            ))}
          </ul>
          <p className="font-display text-xl text-navy">Total {money(total)}</p>
          <div className="mt-4 flex gap-2">
            <Button variant="ghost" onClick={() => setStep(0)}>Atras</Button>
            <Button disabled={!items.length || !address} onClick={() => setStep(2)}>Continuar</Button>
          </div>
        </Panel>
      )}
      {step === 2 && (
        <Panel title="Documento y firma">
          <Select label="Politica" value={policy} onChange={(e) => setPolicy(e.target.value)}>
            <option value="CONTRATO">Contrato</option>
            {eligibility?.canSkipContract && <option value="REMISION">Remision</option>}
          </Select>
          <p className="my-3 text-sm">Firma del cliente</p>
          <canvas ref={canvasRef} width={520} height={180} className="w-full rounded border" onMouseDown={drawStart} />
          <div className="mt-4 flex gap-2">
            <Button variant="ghost" onClick={() => setStep(1)}>Atras</Button>
            <Button onClick={() => setStep(3)}>Continuar</Button>
          </div>
        </Panel>
      )}
      {step === 3 && (
        <Panel title="Pago">
          <p className="mb-4">A cobrar {money(total)}.</p>
          <div className="flex flex-wrap gap-2">
            <Button loading={finish.isPending} onClick={() => finish.mutate('efectivo')}>Efectivo</Button>
            <Button variant="navy" loading={finish.isPending} onClick={() => finish.mutate('qr')}>QR</Button>
            <Button variant="ghost" loading={finish.isPending} onClick={() => finish.mutate('pendiente')}>Continuar sin pago</Button>
          </div>
        </Panel>
      )}
    </div>
  )
}
