import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Badge, Button, Drawer, EmptyState, Input, PageHeader, Pagination, Panel } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api } from '../../services/api'
import type { CustomerProfile, CustomerView, PageResponse } from '../../types/api'
import { getErrorMessage, money } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

const schema = z.object({
  document: z.string().min(4),
  documentType: z.string(),
  name: z.string().min(3),
  phone: z.string().optional(),
  email: z.string().optional(),
  notes: z.string().optional(),
  dataConsent: z.boolean().refine((v) => v === true, 'El cliente debe aceptar el tratamiento de datos'),
  address: z.string().optional(),
  reference: z.string().optional(),
})

export function ClientesPage() {
  const { can } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<number | null>(null)
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { documentType: 'CC', dataConsent: true as const } })

  const list = useQuery({
    queryKey: ['clientes', q, page],
    queryFn: async () => (await api.get<PageResponse<CustomerView>>('/clientes', { params: { q, page, size: 20 } })).data,
  })
  const profile = useQuery({
    queryKey: ['perfil', selected],
    queryFn: async () => (await api.get<CustomerProfile>(`/clientes/${selected}/perfil`)).data,
    enabled: selected != null,
  })

  return (
    <div>
      <PageHeader
        eyebrow="CRM"
        title="Clientes"
        subtitle="Ficha 360, reputacion, antecedentes y material en su poder."
        action={can(P.CLIENTES_GESTIONAR) ? <Button onClick={() => { form.reset({ documentType: 'CC', dataConsent: true }); setOpen(true) }}>Nuevo cliente</Button> : undefined}
      />
      <Panel>
        <Input label="Buscar" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <div className="mt-4 space-y-2">
          {(list.data?.content ?? []).map((c) => (
            <button key={c.id} className="flex w-full items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-left" onClick={() => setSelected(c.id)}>
              <div>
                <p className="font-medium text-navy">{c.name}</p>
                <p className="text-xs text-steel-muted">{c.document} · {c.phone ?? 'sin telefono'}</p>
              </div>
              <Badge tone={c.reputationScore < 40 ? 'danger' : c.customerType === 'CONFIANZA' ? 'success' : 'navy'}>{c.customerType} · {c.reputationScore}</Badge>
            </button>
          ))}
          {!list.data?.content.length && <EmptyState title="No hay clientes" />}
        </div>
        <Pagination page={page} totalPages={list.data?.totalPages ?? 0} onPage={setPage} />
      </Panel>

      <Drawer open={open} title="Registrar cliente" onClose={() => setOpen(false)}>
        <form className="space-y-3" onSubmit={form.handleSubmit(async (values) => {
          try {
            await api.post('/clientes', {
              ...values,
              addresses: values.address ? [{ address: values.address, reference: values.reference, isDefault: true }] : [],
            })
            toast.push('Cliente creado')
            setOpen(false)
            void qc.invalidateQueries({ queryKey: ['clientes'] })
          } catch (error) {
            toast.push(getErrorMessage(error), 'error')
          }
        })}>
          <Input label="Documento" error={form.formState.errors.document?.message} {...form.register('document')} />
          <Input label="Nombre" error={form.formState.errors.name?.message} {...form.register('name')} />
          <Input label="Telefono" {...form.register('phone')} />
          <Input label="Correo" {...form.register('email')} />
          <Input label="Direccion" {...form.register('address')} />
          <Input label="Referencia" {...form.register('reference')} />
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...form.register('dataConsent')} />
            Acepta tratamiento de datos
          </label>
          {form.formState.errors.dataConsent && <p className="text-xs text-red-700">{form.formState.errors.dataConsent.message}</p>}
          <Button className="w-full">Guardar</Button>
        </form>
      </Drawer>

      <Drawer open={selected != null} title={profile.data?.customer.name ?? 'Ficha'} onClose={() => setSelected(null)}>
        {profile.data && (
          <div className="space-y-4 text-sm">
            <p>{profile.data.customer.document} · Reputacion {profile.data.customer.reputationScore}</p>
            <p>Cartera {money(profile.data.totalDebt)} · Saldo a favor {money(profile.data.availableCredit)}</p>
            <div>
              <h4 className="font-display text-navy">En su poder</h4>
              {profile.data.itemsInPossession.map((item) => (
                <p key={item.rentalCode + item.productName}>{item.productName} x{item.quantity} · {item.rentalCode}</p>
              ))}
              {!profile.data.itemsInPossession.length && <p className="text-steel-muted">Nada en calle.</p>}
            </div>
            <div>
              <h4 className="font-display text-navy">Alquileres activos</h4>
              {profile.data.activeRentals.map((r) => <p key={r.id}>{r.code} · {r.status}</p>)}
            </div>
            {can(P.ANTECEDENTES_CONSULTAR) && (
              <Button variant="navy" onClick={async () => {
                try {
                  await api.post('/antecedentes/consultar', { document: profile.data.customer.document, customerId: profile.data.customer.id })
                  toast.push('Consulta de antecedentes registrada')
                  void qc.invalidateQueries({ queryKey: ['perfil', selected] })
                } catch (error) {
                  toast.push(getErrorMessage(error), 'error')
                }
              }}>Consultar antecedentes</Button>
            )}
            {can(P.CLIENTES_GESTIONAR) && (
              <label className="block text-sm">
                <span className="mb-1 block">Copia de cedula</span>
                <input type="file" onChange={async (e) => {
                  const file = e.target.files?.[0]
                  if (!file || !selected) return
                  const data = new FormData()
                  data.append('file', file)
                  await api.post(`/clientes/${selected}/cedula`, data)
                  toast.push('Cedula adjunta')
                }} />
              </label>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
