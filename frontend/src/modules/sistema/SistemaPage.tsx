import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Button, FileUpload, Input, PageHeader, Panel, Select } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { api, getAccessToken } from '../../services/api'
import type { ImportJobView, PreviewResponse } from '../../types/api'
import { getErrorMessage } from '../../lib/format'
import { P } from '../../lib/permissions'
import { useAuth } from '../auth/AuthContext'

type Tab = 'usuarios' | 'importar' | 'conexiones' | 'empresa' | 'auditoria'

export function SistemaPage() {
  const { can } = useAuth()
  const [tab, setTab] = useState<Tab>(can(P.USUARIOS_GESTIONAR) ? 'usuarios' : 'importar')
  const tabs: { id: Tab; label: string; permission: string }[] = [
    { id: 'usuarios', label: 'Usuarios', permission: P.USUARIOS_GESTIONAR },
    { id: 'importar', label: 'Importar', permission: P.IMPORTACION_EJECUTAR },
    { id: 'conexiones', label: 'Conexiones', permission: P.CONFIGURACION_GESTIONAR },
    { id: 'empresa', label: 'Empresa', permission: P.CONFIGURACION_GESTIONAR },
    { id: 'auditoria', label: 'Auditoria', permission: P.USUARIOS_GESTIONAR },
  ]

  return (
    <div>
      <PageHeader eyebrow="Sistema" title="Administracion" subtitle="Personas, importaciones, integraciones y bitacora." />
      <div className="mb-4 flex flex-wrap gap-2">
        {tabs.filter((t) => can(t.permission)).map((t) => (
          <Button key={t.id} variant={tab === t.id ? 'navy' : 'ghost'} onClick={() => setTab(t.id)}>{t.label}</Button>
        ))}
      </div>
      {tab === 'usuarios' && <UsersTab />}
      {tab === 'importar' && <ImportTab />}
      {tab === 'conexiones' && <ConnectionsTab />}
      {tab === 'empresa' && <CompanyTab />}
      {tab === 'auditoria' && <AuditTab />}
    </div>
  )
}

function UsersTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const users = useQuery({ queryKey: ['usuarios'], queryFn: async () => (await api.get('/sistema/usuarios')).data })
  const roles = useQuery({ queryKey: ['roles'], queryFn: async () => (await api.get('/sistema/roles')).data })
  const [generated, setGenerated] = useState<string | null>(null)

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Panel title="Usuarios">
        <form className="mb-4 space-y-2" onSubmit={async (e) => {
          e.preventDefault()
          const data = new FormData(e.currentTarget)
          try {
            const res = await api.post('/sistema/usuarios', {
              fullName: data.get('fullName'),
              username: data.get('username') || undefined,
              roleIds: [Number(data.get('roleId'))],
            })
            setGenerated(res.data.generatedPassword)
            toast.push('Usuario creado. Copia la contrasena.')
            void qc.invalidateQueries({ queryKey: ['usuarios'] })
          } catch (error) {
            toast.push(getErrorMessage(error), 'error')
          }
        }}>
          <Input label="Nombre" name="fullName" required />
          <Input label="Usuario (opcional)" name="username" />
          <Select label="Rol" name="roleId">
            {(roles.data ?? []).map((r: { id: number; name: string }) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </Select>
          <Button type="submit">Crear usuario</Button>
        </form>
        {generated && (
          <p className="mb-3 rounded bg-orange-50 p-2 text-sm">
            Contrasena: <strong>{generated}</strong>
            <Button variant="ghost" onClick={() => navigator.clipboard.writeText(generated)}>Copiar</Button>
          </p>
        )}
        {(users.data ?? []).map((u: { id: number; username: string; fullName: string }) => (
          <div key={u.id} className="flex items-center justify-between text-sm">
            <span>{u.fullName} · {u.username}</span>
            <Button variant="ghost" onClick={async () => {
              const res = await api.post(`/sistema/usuarios/${u.id}/reset-password`)
              setGenerated(res.data.generatedPassword)
            }}>Reset clave</Button>
          </div>
        ))}
      </Panel>
      <Panel title="Roles">
        {(roles.data ?? []).map((r: { id: number; name: string; permissions: string[] }) => (
          <p key={r.id} className="text-sm">{r.name}: {r.permissions.length} permisos</p>
        ))}
      </Panel>
    </div>
  )
}

function ImportTab() {
  const toast = useToast()
  const [tipo, setTipo] = useState('PRODUCTOS')
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<PreviewResponse | null>(null)
  const jobs = useQuery({ queryKey: ['imports'], queryFn: async () => (await api.get<ImportJobView[]>('/importacion')).data })

  async function send(path: string) {
    if (!file) return
    const data = new FormData()
    data.append('file', file)
    const res = await api.post(path, data, { params: { tipo } })
    return res.data
  }

  return (
    <Panel title="Importacion CSV / Excel">
      <Select label="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)}>
        <option>PRODUCTOS</option>
        <option>CLIENTES</option>
        <option>MOVIMIENTOS</option>
        <option>DEUDAS</option>
      </Select>
      <div className="my-4">
        <FileUpload label="Archivo de la administradora" accept=".csv,.xlsx,.xls" onFile={setFile} />
        {file && <p className="mt-2 text-sm">{file.name}</p>}
      </div>
      <div className="mb-4 flex gap-2">
        <Button variant="ghost" onClick={async () => {
          try { setPreview(await send('/importacion/previsualizar')) }
          catch (error) { toast.push(getErrorMessage(error), 'error') }
        }}>Previsualizar</Button>
        <Button onClick={async () => {
          try {
            await send('/importacion/ejecutar')
            toast.push('Importacion ejecutada')
          } catch (error) { toast.push(getErrorMessage(error), 'error') }
        }}>Ejecutar</Button>
      </div>
      {preview && (
        <div className="mb-4 text-sm">
          <p>{preview.validRows} filas validas · {preview.errorRows} con error</p>
          <p>Mapeo: {Object.entries(preview.suggestedMapping).map(([k, v]) => `${k}→${v}`).join(', ')}</p>
        </div>
      )}
      {(jobs.data ?? []).map((job) => (
        <div key={job.id} className="flex justify-between text-sm">
          <span>{job.fileName} · {job.status} · {job.rowsOk}/{job.rowsTotal}</span>
          {job.hasErrorReport && (
            <button className="text-accent" onClick={async () => {
              const res = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8081/api'}/importacion/${job.id}/errores`, {
                headers: { Authorization: `Bearer ${getAccessToken()}` },
              })
              const blob = await res.blob()
              const a = document.createElement('a')
              a.href = URL.createObjectURL(blob)
              a.download = `errores-${job.id}.csv`
              a.click()
            }}>Errores</button>
          )}
        </div>
      ))}
    </Panel>
  )
}

function ConnectionsTab() {
  const toast = useToast()
  const status = useQuery({ queryKey: ['integraciones'], queryFn: async () => (await api.get('/integraciones/estado')).data })
  const inbox = useQuery({ queryKey: ['notificaciones'], queryFn: async () => (await api.get('/notificaciones')).data })
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Panel title="Canales">
        {status.data && (['whatsapp', 'pagos', 'maps', 'antecedentes'] as const).map((key) => {
          const ch = status.data[key] as { id: string; enabled: boolean; provider: string }
          return <p key={key} className="text-sm">{ch.id}: {ch.provider} {ch.enabled ? 'activo' : 'simulado'}</p>
        })}
        <Button className="mt-3" onClick={async () => {
          try {
            await api.post('/integraciones/whatsapp/prueba')
            toast.push('Mensaje de prueba encolado')
          } catch (error) { toast.push(getErrorMessage(error), 'error') }
        }}>Probar WhatsApp</Button>
      </Panel>
      <Panel title="Bandeja">
        {(inbox.data?.content ?? []).slice(0, 8).map((n: { id: number; templateCode?: string; status?: string }) => (
          <p key={n.id} className="text-sm">{n.templateCode} · {n.status}</p>
        ))}
      </Panel>
    </div>
  )
}

function CompanyTab() {
  const toast = useToast()
  const company = useQuery({ queryKey: ['empresa'], queryFn: async () => (await api.get('/sistema/empresa')).data })
  if (!company.data) return null
  return (
    <Panel title="Datos de la empresa">
      <form className="grid gap-3 md:grid-cols-2" onSubmit={async (e) => {
        e.preventDefault()
        const data = new FormData(e.currentTarget)
        try {
          await api.put('/sistema/empresa', Object.fromEntries(data.entries()))
          toast.push('Empresa actualizada')
        } catch (error) { toast.push(getErrorMessage(error), 'error') }
      }}>
        <Input label="Razon social" name="legalName" defaultValue={company.data.legalName} />
        <Input label="Nombre comercial" name="tradeName" defaultValue={company.data.tradeName} />
        <Input label="NIT" name="nit" defaultValue={company.data.nit} />
        <Input label="Propietaria" name="ownerName" defaultValue={company.data.ownerName} />
        <Input label="Telefono" name="phone" defaultValue={company.data.phone} />
        <Input label="WhatsApp admin" name="whatsappAdmin" defaultValue={company.data.whatsappAdmin} />
        <Input label="Direccion" name="address" defaultValue={company.data.address} />
        <Input label="Ciudad" name="city" defaultValue={company.data.city} />
        <Button type="submit">Guardar</Button>
      </form>
    </Panel>
  )
}

function AuditTab() {
  const logs = useQuery({ queryKey: ['auditoria'], queryFn: async () => (await api.get('/sistema/auditoria')).data })
  return (
    <Panel title="Bitacora">
      {(logs.data?.content ?? []).map((log: { id: number; username: string; action: string; detail?: string }) => (
        <p key={log.id} className="text-sm">{log.username} · {log.action} · {log.detail}</p>
      ))}
    </Panel>
  )
}
