import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Badge, Button, EmptyState, PageHeader, Panel } from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'

type IntegrationCard = {
  name: string
  enabled?: boolean
  configured: boolean
  mode: string
  description: string
  provider?: string
  adminPhone?: string
  apiKey?: string
}

type StatusResponse = {
  whatsapp: IntegrationCard
  payments: IntegrationCard
  maps: IntegrationCard
  backgroundCheck: IntegrationCard
}

type NotificationRow = {
  id: number
  channel?: string
  message?: string
  status?: string
  sentAt?: string
}

const order: (keyof StatusResponse)[] = ['whatsapp', 'payments', 'maps', 'backgroundCheck']

const titles: Record<keyof StatusResponse, string> = {
  whatsapp: 'WhatsApp Cloud API',
  payments: 'Pasarela de pagos QR',
  maps: 'Google Maps',
  backgroundCheck: 'Proveedor de antecedentes',
}

function asNotifications(data: unknown): NotificationRow[] {
  return Array.isArray(data) ? (data as NotificationRow[]) : []
}

export default function IntegracionesPage({ embedded = false }: { embedded?: boolean }) {
  const qc = useQueryClient()
  const [testPhone, setTestPhone] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const statusQuery = useQuery({
    queryKey: ['integraciones', 'status'],
    queryFn: async () => {
      const { data } = await api.get<StatusResponse>('/integraciones/status')
      return data
    },
    retry: 1,
  })

  const notifQuery = useQuery({
    queryKey: ['integraciones', 'notificaciones'],
    queryFn: async () => {
      const { data } = await api.get<NotificationRow[]>('/integraciones/notificaciones')
      return asNotifications(data)
    },
    retry: 1,
  })

  const testWa = useMutation({
    mutationFn: async () => {
      const { data } = await api.post('/integraciones/whatsapp/test', {
        phone: testPhone || undefined,
        message: 'HYEDA: prueba de conexión desde el panel.',
      })
      return data as { sent: boolean; phone: string; mode: string }
    },
    onSuccess: (data) => {
      setMessage(`WhatsApp ${data.mode}: mensaje a ${data.phone} → ${data.sent ? 'OK' : 'FALLÓ'}`)
      setError('')
      void qc.invalidateQueries({ queryKey: ['integraciones', 'notificaciones'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const qrDemo = useMutation({
    mutationFn: async () => {
      const { data } = await api.post('/integraciones/pagos/qr-demo', {
        rentalId: 0,
        amount: 25000,
      })
      return data as Record<string, string>
    },
    onSuccess: (data) => {
      setMessage(`QR demo: ${data.reference} · payload ${data.qrPayload}`)
      setError('')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const status = statusQuery.data
  const notifications = asNotifications(notifQuery.data)
  const statusError = statusQuery.isError ? getErrorMessage(statusQuery.error) : ''
  const notifError = notifQuery.isError ? getErrorMessage(notifQuery.error) : ''

  return (
    <div className="space-y-6">
      {!embedded && (
        <PageHeader
          title="Sistemas externos"
          subtitle="Estado de WhatsApp, pagos QR, Google Maps y antecedentes · ver docs/INTEGRACIONES.md"
        />
      )}

      {(error || statusError) && <Alert>{error || statusError}</Alert>}
      {message && <Alert tone="success">{message}</Alert>}

      {statusQuery.isLoading ? (
        <p className="text-[var(--text-muted)]">Cargando conexiones…</p>
      ) : statusQuery.isError ? (
        <Panel title="No se pudo cargar el estado">
          <div className="p-5 space-y-3 text-sm text-[var(--text-muted)]">
            <p>
              El backend no respondió en <code>/api/integraciones/status</code>. Reinicia Spring Boot en el
              puerto 8081 y vuelve a iniciar sesión.
            </p>
            <Button variant="navy" onClick={() => void statusQuery.refetch()}>
              Reintentar
            </Button>
          </div>
        </Panel>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {order.map((key) => {
            const item = status?.[key]
            if (!item || typeof item !== 'object') return null
            const live = item.mode === 'LIVE' || item.mode === 'API'
            return (
              <article key={key} className="panel overflow-hidden">
                <div
                  className="px-5 py-4 flex items-start justify-between gap-3"
                  style={{
                    background: live
                      ? 'linear-gradient(120deg, var(--navy), #2a3066)'
                      : 'linear-gradient(120deg, #5c5f6b, #3d4150)',
                    color: '#fff',
                  }}
                >
                  <div>
                    <div className="text-[0.65rem] uppercase tracking-[0.14em] opacity-70 font-bold">
                      {key}
                    </div>
                    <h2 className="font-display text-2xl uppercase mt-1">{titles[key]}</h2>
                  </div>
                  <Badge tone={item.configured ? 'success' : 'accent'}>{item.mode || '—'}</Badge>
                </div>
                <div className="p-5 space-y-3">
                  <p className="text-sm text-[var(--text-muted)] leading-relaxed">
                    {item.description}
                  </p>
                  <div className="flex flex-wrap gap-2 text-xs">
                    <Badge tone={item.configured ? 'success' : 'danger'}>
                      {item.configured ? 'Configurado' : 'Pendiente credenciales'}
                    </Badge>
                    {item.provider ? <Badge tone="navy">Provider: {item.provider}</Badge> : null}
                    {item.adminPhone ? <Badge tone="muted">Admin: {item.adminPhone}</Badge> : null}
                  </div>

                  {key === 'whatsapp' && (
                    <div className="flex flex-col sm:flex-row gap-2 pt-2">
                      <input
                        className="field-input"
                        placeholder="Teléfono prueba (opcional)"
                        value={testPhone}
                        onChange={(e) => setTestPhone(e.target.value)}
                      />
                      <Button
                        variant="primary"
                        disabled={testWa.isPending}
                        onClick={() => testWa.mutate()}
                        className="shrink-0"
                      >
                        Probar WhatsApp
                      </Button>
                    </div>
                  )}

                  {key === 'payments' && (
                    <Button variant="navy" disabled={qrDemo.isPending} onClick={() => qrDemo.mutate()}>
                      Generar QR demo
                    </Button>
                  )}

                  {key === 'maps' && (
                    <p className="text-xs text-[var(--text-muted)]">
                      {item.configured
                        ? 'API key presente. El wizard puede embeber el mapa.'
                        : 'Define GOOGLE_MAPS_API_KEY / hyeda.maps.api-key'}
                    </p>
                  )}

                  {key === 'backgroundCheck' && (
                    <ButtonLink to="/clientes?panel=antecedentes" />
                  )}
                </div>
              </article>
            )
          })}
        </div>
      )}

      <Panel title="Cola de notificaciones">
        {notifQuery.isLoading ? (
          <p className="p-5 text-[var(--text-muted)]">Cargando…</p>
        ) : notifQuery.isError ? (
          <div className="p-5 space-y-2 text-sm text-[var(--text-muted)]">
            <p>{notifError || 'No se pudieron cargar las notificaciones.'}</p>
            <Button variant="ghost" onClick={() => void notifQuery.refetch()}>
              Reintentar
            </Button>
          </div>
        ) : notifications.length === 0 ? (
          <EmptyState
            title="Sin notificaciones"
            hint="Al crear alquileres o probar WhatsApp aparecerán aquí."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Canal</th>
                  <th>Estado</th>
                  <th>Mensaje</th>
                  <th>Enviado</th>
                </tr>
              </thead>
              <tbody>
                {[...notifications]
                  .reverse()
                  .slice(0, 30)
                  .map((n) => (
                    <tr key={n.id}>
                      <td>#{n.id}</td>
                      <td>
                        <Badge tone="navy">{n.channel || '—'}</Badge>
                      </td>
                      <td>
                        <Badge tone={n.status === 'SENT' ? 'success' : 'danger'}>
                          {n.status || '—'}
                        </Badge>
                      </td>
                      <td className="max-w-md truncate" title={n.message}>
                        {n.message}
                      </td>
                      <td>{n.sentAt ? new Date(n.sentAt).toLocaleString('es-CO') : '—'}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <div className="panel p-5 text-sm text-[var(--text-muted)] leading-relaxed">
        <strong className="text-[var(--navy)]">Checklist de producción:</strong> Meta Business + token
        WhatsApp, llaves Wompi, API key de Google Maps y decisión de proveedor de antecedentes (o flujo
        manual). Detalle en <code>HYEDA/docs/INTEGRACIONES.md</code>.
      </div>
    </div>
  )
}

function ButtonLink({ to }: { to: string }) {
  return (
    <Link to={to} className="btn btn-ghost inline-flex">
      Ir a Antecedentes →
    </Link>
  )
}
