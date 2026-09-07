import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import api, { getErrorMessage } from '../../services/api'
import type { CreateUserRequest, Role, User } from '../../types'

export default function AdminPage({ embedded = false }: { embedded?: boolean }) {
  const qc = useQueryClient()
  const [form, setForm] = useState<CreateUserRequest>({
    username: '',
    password: '',
    fullName: '',
    email: '',
    roles: ['ALQUILERES'],
  })
  const [roleName, setRoleName] = useState('')
  const [roleDesc, setRoleDesc] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const usersQuery = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: async () => {
      const { data } = await api.get<User[]>('/admin/users')
      return data
    },
  })

  const rolesQuery = useQuery({
    queryKey: ['admin', 'roles'],
    queryFn: async () => {
      const { data } = await api.get<Role[]>('/admin/roles')
      return data
    },
  })

  const createUser = useMutation({
    mutationFn: async (payload: CreateUserRequest) => {
      const { data } = await api.post('/admin/users', payload)
      return data
    },
    onSuccess: () => {
      setMessage('Usuario creado')
      setError('')
      setForm({ username: '', password: '', fullName: '', email: '', roles: ['ALQUILERES'] })
      void qc.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => {
      setMessage('')
      setError(getErrorMessage(err, 'No se pudo crear el usuario'))
    },
  })

  const createRole = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<Role>('/admin/roles', {
        name: roleName,
        description: roleDesc,
        permissions: [],
      })
      return data
    },
    onSuccess: () => {
      setMessage('Rol creado')
      setError('')
      setRoleName('')
      setRoleDesc('')
      void qc.invalidateQueries({ queryKey: ['admin', 'roles'] })
    },
    onError: (err) => {
      setMessage('')
      setError(getErrorMessage(err, 'No se pudo crear el rol'))
    },
  })

  function submitUser(e: FormEvent) {
    e.preventDefault()
    createUser.mutate(form)
  }

  function submitRole(e: FormEvent) {
    e.preventDefault()
    createRole.mutate()
  }

  return (
    <div className="space-y-6">
      {!embedded && (
        <header>
          <h1 className="font-display text-3xl text-[var(--navy)] uppercase tracking-wide">Administración</h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">Usuarios y roles</p>
        </header>
      )}

      {error && <p className="text-sm text-[var(--danger)]">{error}</p>}
      {message && <p className="text-sm text-[var(--success)]">{message}</p>}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-[var(--border)] bg-[var(--bg-elevated)] overflow-hidden">
          <div className="px-4 py-3 border-b border-[var(--border)] font-medium">Usuarios</div>
          {usersQuery.isLoading ? (
            <p className="p-4 text-[var(--text-muted)]">Cargando…</p>
          ) : usersQuery.error ? (
            <p className="p-4 text-sm text-[var(--danger)]">{getErrorMessage(usersQuery.error)}</p>
          ) : (
            <ul className="divide-y divide-[var(--border)] max-h-64 overflow-y-auto">
              {(usersQuery.data || []).map((u) => (
                <li key={u.id ?? u.username} className="px-4 py-3 text-sm">
                  <div className="font-medium">{u.fullName || u.username}</div>
                  <div className="text-xs text-[var(--text-muted)]">
                    @{u.username} · {(u.roles || []).join(', ')}
                    {u.active === false ? ' · Inactivo' : ''}
                  </div>
                </li>
              ))}
            </ul>
          )}
          <form onSubmit={submitUser} className="p-4 border-t border-[var(--border)] space-y-3">
            <h3 className="text-sm font-medium">Nuevo usuario</h3>
            <input
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Usuario"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
            <input
              type="password"
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Contraseña"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
            <input
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Nombre completo"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            />
            <input
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
            <select
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              value={form.roles[0] || ''}
              onChange={(e) => setForm({ ...form, roles: [e.target.value] })}
            >
              {(rolesQuery.data?.length
                ? rolesQuery.data.map((r) => r.name)
                : ['ADMIN', 'ALQUILERES']
              ).map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
            <button
              type="submit"
              disabled={createUser.isPending}
              className="w-full rounded-md bg-[var(--accent)] px-4 py-2 text-sm font-medium text-[#1a1f24] disabled:opacity-60"
            >
              Crear usuario
            </button>
          </form>
        </section>

        <section className="rounded-lg border border-[var(--border)] bg-[var(--bg-elevated)] overflow-hidden">
          <div className="px-4 py-3 border-b border-[var(--border)] font-medium">Roles</div>
          {rolesQuery.isLoading ? (
            <p className="p-4 text-[var(--text-muted)]">Cargando…</p>
          ) : rolesQuery.error ? (
            <p className="p-4 text-sm text-[var(--danger)]">{getErrorMessage(rolesQuery.error)}</p>
          ) : (
            <ul className="divide-y divide-[var(--border)] max-h-64 overflow-y-auto">
              {(rolesQuery.data || []).map((r) => (
                <li key={r.id} className="px-4 py-3 text-sm">
                  <div className="font-medium">{r.name}</div>
                  <div className="text-xs text-[var(--text-muted)]">
                    {r.description || 'Sin descripción'} · {(r.permissions || []).length} permisos
                  </div>
                </li>
              ))}
            </ul>
          )}
          <form onSubmit={submitRole} className="p-4 border-t border-[var(--border)] space-y-3">
            <h3 className="text-sm font-medium">Nuevo rol</h3>
            <input
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Nombre del rol"
              value={roleName}
              onChange={(e) => setRoleName(e.target.value)}
              required
            />
            <input
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              placeholder="Descripción"
              value={roleDesc}
              onChange={(e) => setRoleDesc(e.target.value)}
            />
            <button
              type="submit"
              disabled={createRole.isPending}
              className="w-full rounded-md bg-[var(--accent)] px-4 py-2 text-sm font-medium text-[#1a1f24] disabled:opacity-60"
            >
              Crear rol
            </button>
          </form>
        </section>
      </div>
    </div>
  )
}
