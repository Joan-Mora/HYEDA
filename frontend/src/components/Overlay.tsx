import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useRef, useState, type PointerEvent, type ReactNode } from 'react'
import { getToken } from '../services/auth'
import { getErrorMessage } from '../services/api'
import { Button } from './ui'

export function PdfModal({
  open,
  onClose,
  documentId,
  title = 'Documento',
}: {
  open: boolean
  onClose: () => void
  documentId: number | null
  title?: string
}) {
  const [url, setUrl] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let objectUrl: string | null = null
    let cancelled = false
    async function load(id: number) {
      setLoading(true)
      setError('')
      setUrl(null)
      try {
        const token = getToken()
        const base = import.meta.env.VITE_API_URL || 'http://localhost:8081/api'
        const res = await fetch(`${base}/documentos/${id}/pdf`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        })
        if (!res.ok) throw new Error(`No se pudo cargar (${res.status})`)
        const blob = await res.blob()
        objectUrl = URL.createObjectURL(blob)
        if (!cancelled) setUrl(objectUrl)
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err, 'Error al cargar PDF'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    if (open && documentId != null) void load(documentId)
    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [open, documentId])

  return (
    <AnimatePresence>
      {open && documentId != null && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
          <motion.button
            type="button"
            aria-label="Cerrar"
            className="absolute inset-0 bg-[rgba(20,24,51,0.55)] backdrop-blur-[4px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            className="relative w-full max-w-4xl h-[min(86vh,820px)] bg-white rounded-[var(--radius)] shadow-2xl border border-[rgba(0,0,0,0.08)] flex flex-col overflow-hidden"
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.98 }}
            transition={{ type: 'spring', stiffness: 360, damping: 30 }}
          >
            <header className="px-5 py-3.5 border-b border-[var(--border)] flex items-center justify-between gap-3">
              <h3 className="font-display text-xl text-[var(--navy)] uppercase">{title}</h3>
              <div className="flex gap-2">
                {url && (
                  <>
                    <a className="btn btn-ghost !py-1.5" href={url} download={`hyeda-doc-${documentId}.pdf`}>
                      Descargar
                    </a>
                    <button type="button" className="btn btn-navy !py-1.5" onClick={() => window.open(url, '_blank')}>
                      Imprimir
                    </button>
                  </>
                )}
                <Button variant="ghost" className="!py-1.5" onClick={onClose}>
                  Cerrar
                </Button>
              </div>
            </header>
            <div className="flex-1 bg-[var(--bone)] min-h-0">
              {loading && <p className="p-6 text-[var(--text-muted)]">Cargando vista previa…</p>}
              {error && <p className="p-6 text-[var(--danger)]">{error}</p>}
              {url && <iframe title={title} src={url} className="w-full h-full border-0" />}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}

export function SignaturePad({
  onChange,
}: {
  onChange: (dataUrl: string | null) => void
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const drawing = useRef(false)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const dpr = window.devicePixelRatio || 1
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width * dpr
    canvas.height = rect.height * dpr
    ctx.scale(dpr, dpr)
    ctx.strokeStyle = '#1b1f4c'
    ctx.lineWidth = 2.2
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
  }, [])

  function pos(e: PointerEvent<HTMLCanvasElement>) {
    const rect = e.currentTarget.getBoundingClientRect()
    return { x: e.clientX - rect.left, y: e.clientY - rect.top }
  }

  function start(e: PointerEvent<HTMLCanvasElement>) {
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx) return
    drawing.current = true
    e.currentTarget.setPointerCapture(e.pointerId)
    const p = pos(e)
    ctx.beginPath()
    ctx.moveTo(p.x, p.y)
  }

  function move(e: PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx) return
    const p = pos(e)
    ctx.lineTo(p.x, p.y)
    ctx.stroke()
  }

  function end() {
    drawing.current = false
    const canvas = canvasRef.current
    if (canvas) onChange(canvas.toDataURL('image/png'))
  }

  function clear() {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    onChange(null)
  }

  return (
    <div className="space-y-2">
      <canvas
        ref={canvasRef}
        className="w-full h-40 rounded-lg border border-[var(--border)] bg-white touch-none cursor-crosshair"
        onPointerDown={start}
        onPointerMove={move}
        onPointerUp={end}
        onPointerLeave={end}
      />
      <button type="button" className="text-xs font-semibold text-[var(--accent)]" onClick={clear}>
        Limpiar firma
      </button>
    </div>
  )
}

export function WorksSites({
  sites,
}: {
  sites: { id: number; reference?: string; address?: string; label: string }[]
}) {
  const listed = sites.filter((s) => (s.reference || s.address || '').trim().length > 0)

  if (!listed.length) {
    return (
      <div className="rounded-[var(--radius)] border border-[var(--border)] bg-[var(--navy-deep)] text-white/70 p-6 text-sm">
        Sin obras con dirección. Al registrar referencia y dirección en el wizard aparecerán aquí.
      </div>
    )
  }

  function mapsUrl(s: { reference?: string; address?: string }) {
    const q = [s.reference, s.address].filter(Boolean).join(', ')
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`
  }

  return (
    <div className="rounded-[var(--radius)] overflow-hidden border border-[rgba(255,255,255,0.08)] shadow-lg">
      <div className="bg-[var(--navy-deep)] px-4 py-3 flex items-center justify-between gap-2">
        <span className="text-[0.65rem] font-bold uppercase tracking-wider text-white/80">
          Obras activas · {listed.length}
        </span>
      </div>
      <ul className="bg-[var(--navy)] text-white/90 text-sm divide-y divide-white/10 max-h-64 overflow-y-auto">
        {listed.map((s) => (
          <li key={s.id} className="px-4 py-3 flex justify-between gap-3 items-start">
            <div className="min-w-0">
              <p className="font-semibold truncate">{s.reference || s.label}</p>
              {s.address && <p className="text-xs text-white/60 mt-0.5 truncate">{s.address}</p>}
              <p className="text-[0.65rem] text-white/45 mt-1">{s.label}</p>
            </div>
            <a
              className="text-[0.7rem] font-bold uppercase tracking-wide text-[var(--accent-hover)] shrink-0"
              href={mapsUrl(s)}
              target="_blank"
              rel="noreferrer"
            >
              Abrir Maps
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function Pressable({
  children,
  className = '',
  onClick,
  type = 'button',
  disabled,
}: {
  children: ReactNode
  className?: string
  onClick?: () => void
  type?: 'button' | 'submit'
  disabled?: boolean
}) {
  return (
    <motion.button
      type={type}
      disabled={disabled}
      className={className}
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      whileHover={{ scale: disabled ? 1 : 1.02 }}
      transition={{ type: 'spring', stiffness: 500, damping: 28 }}
    >
      {children}
    </motion.button>
  )
}
