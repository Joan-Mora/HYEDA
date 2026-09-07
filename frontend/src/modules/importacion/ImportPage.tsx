import { useState, type FormEvent } from 'react'
import api, { getErrorMessage } from '../../services/api'

type UploadKind =
  | 'productos-docx'
  | 'inventario-excel'
  | 'clientes-excel'
  | 'movimientos-excel'
  | 'seed'

export default function ImportPage({ embedded = false }: { embedded?: boolean }) {
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [movementType, setMovementType] = useState<'INGRESO' | 'GASTO'>('INGRESO')

  async function upload(
    kind: UploadKind,
    files: Record<string, File | undefined>,
    extra?: Record<string, string>,
  ) {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const form = new FormData()
      for (const [key, file] of Object.entries(files)) {
        if (file) form.append(key, file)
      }
      const params = extra ? { params: extra } : undefined
      const endpoint =
        kind === 'seed'
          ? '/importacion/seed'
          : `/importacion/${kind}`
      const { data } = await api.post(endpoint, form, {
        ...params,
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setMessage(typeof data === 'object' ? JSON.stringify(data) : 'Importación completada')
    } catch (err) {
      setError(getErrorMessage(err, 'Error en la importación'))
    } finally {
      setLoading(false)
    }
  }

  function singleUpload(kind: Exclude<UploadKind, 'seed'>, e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const input = form.elements.namedItem('file') as HTMLInputElement
    const file = input.files?.[0]
    if (!file) {
      setError('Selecciona un archivo')
      return
    }
    const extra = kind === 'movimientos-excel' ? { type: movementType } : undefined
    void upload(kind, { file }, extra)
  }

  function seedUpload(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const get = (name: string) => (form.elements.namedItem(name) as HTMLInputElement).files?.[0]
    void upload('seed', {
      products: get('products'),
      inventory: get('inventory'),
      customers: get('customers'),
      ingresos: get('ingresos'),
      gastos: get('gastos'),
    })
  }

  return (
    <div className="space-y-6">
      {!embedded && (
        <header>
          <h1 className="font-display text-3xl text-[var(--navy)] uppercase tracking-wide">Importación</h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">
            Carga de datos semilla (DOCX / Excel)
          </p>
        </header>
      )}

      {error && (
        <p className="rounded-md bg-[rgba(212,91,91,0.15)] px-3 py-2 text-sm text-[var(--danger)]">
          {error}
        </p>
      )}
      {message && (
        <p className="rounded-md bg-[rgba(76,175,122,0.15)] px-3 py-2 text-sm text-[var(--success)] break-all">
          {message}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <UploadCard
          title="Productos (DOCX)"
          description="Lista de productos desde documento Word"
          accept=".docx"
          loading={loading}
          onSubmit={(e) => singleUpload('productos-docx', e)}
        />
        <UploadCard
          title="Inventario (Excel)"
          description="Stock y precios desde XLSX"
          accept=".xlsx,.xls"
          loading={loading}
          onSubmit={(e) => singleUpload('inventario-excel', e)}
        />
        <UploadCard
          title="Clientes (Excel)"
          description="Importar base de clientes"
          accept=".xlsx,.xls"
          loading={loading}
          onSubmit={(e) => singleUpload('clientes-excel', e)}
        />
        <section className="rounded-lg border border-[var(--border)] bg-[var(--bg-elevated)] p-5 space-y-3">
          <h2 className="font-medium">Movimientos (Excel)</h2>
          <p className="text-sm text-[var(--text-muted)]">Ingresos o gastos financieros</p>
          <form onSubmit={(e) => singleUpload('movimientos-excel', e)} className="space-y-3">
            <select
              className="w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              value={movementType}
              onChange={(e) => setMovementType(e.target.value as 'INGRESO' | 'GASTO')}
            >
              <option value="INGRESO">Ingreso</option>
              <option value="GASTO">Gasto</option>
            </select>
            <input
              name="file"
              type="file"
              accept=".xlsx,.xls"
              className="block w-full text-sm text-[var(--text-muted)]"
              required
            />
            <button
              type="submit"
              disabled={loading}
              className="rounded-md bg-[var(--accent)] px-4 py-2 text-sm font-medium text-[#1a1f24] disabled:opacity-60"
            >
              Subir
            </button>
          </form>
        </section>
      </div>

      <section className="rounded-lg border border-[var(--border)] bg-[var(--bg-elevated)] p-5 space-y-4">
        <div>
          <h2 className="font-medium">Seed completo</h2>
          <p className="text-sm text-[var(--text-muted)] mt-1">
            Carga múltiple de archivos de inicialización
          </p>
        </div>
        <form onSubmit={seedUpload} className="grid gap-3 md:grid-cols-2">
          <FileField name="products" label="Productos DOCX" accept=".docx" />
          <FileField name="inventory" label="Inventario Excel" accept=".xlsx,.xls" />
          <FileField name="customers" label="Clientes Excel" accept=".xlsx,.xls" />
          <FileField name="ingresos" label="Ingresos Excel" accept=".xlsx,.xls" />
          <FileField name="gastos" label="Gastos Excel" accept=".xlsx,.xls" />
          <div className="md:col-span-2">
            <button
              type="submit"
              disabled={loading}
              className="rounded-md btn btn-primary disabled:opacity-60"
            >
              {loading ? 'Importando…' : 'Ejecutar seed'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function UploadCard({
  title,
  description,
  accept,
  loading,
  onSubmit,
}: {
  title: string
  description: string
  accept: string
  loading: boolean
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
}) {
  return (
    <section className="rounded-lg border border-[var(--border)] bg-[var(--bg-elevated)] p-5 space-y-3">
      <h2 className="font-medium">{title}</h2>
      <p className="text-sm text-[var(--text-muted)]">{description}</p>
      <form onSubmit={onSubmit} className="space-y-3">
        <input
          name="file"
          type="file"
          accept={accept}
          className="block w-full text-sm text-[var(--text-muted)]"
          required
        />
        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-[var(--accent)] px-4 py-2 text-sm font-medium text-[#1a1f24] disabled:opacity-60"
        >
          Subir
        </button>
      </form>
    </section>
  )
}

function FileField({
  name,
  label,
  accept,
}: {
  name: string
  label: string
  accept: string
}) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm text-[var(--text-muted)]">{label}</span>
      <input
        name={name}
        type="file"
        accept={accept}
        className="block w-full text-sm text-[var(--text-muted)]"
      />
    </label>
  )
}
