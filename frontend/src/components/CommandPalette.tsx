import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import api from '../services/api'
import type { Customer, Product, Rental } from '../types'

type Hit = { id: string; group: string; label: string; hint?: string; to: string }

export function CommandPalette() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [q, setQ] = useState('')
  const [active, setActive] = useState(0)

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen((v) => !v)
        setQ('')
        setActive(0)
      }
      if (e.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const customers = useQuery({
    queryKey: ['clientes', 'cmdk'],
    enabled: open,
    queryFn: async () => (await api.get<Customer[]>('/clientes')).data,
  })
  const products = useQuery({
    queryKey: ['productos', 'cmdk'],
    enabled: open,
    queryFn: async () => (await api.get<Product[]>('/productos')).data,
  })
  const rentals = useQuery({
    queryKey: ['alquileres', 'cmdk'],
    enabled: open,
    queryFn: async () => (await api.get<Rental[]>('/alquileres')).data,
  })

  const hits = useMemo(() => {
    const term = q.trim().toLowerCase()
    const nav: Hit[] = [
      { id: 'n-hoy', group: 'Ir a', label: 'Hoy', to: '/' },
      { id: 'n-alq', group: 'Ir a', label: 'Alquileres', to: '/alquileres' },
      { id: 'n-cli', group: 'Ir a', label: 'Clientes', to: '/clientes' },
      { id: 'n-pat', group: 'Ir a', label: 'Patio', to: '/inventario' },
      { id: 'n-caj', group: 'Ir a', label: 'Caja', to: '/finanzas' },
      { id: 'n-new', group: 'Acción', label: 'Abrir alquiler', to: '/alquileres/nuevo' },
    ]
    const cli: Hit[] = (customers.data || []).map((c) => ({
      id: `c-${c.id}`,
      group: 'Clientes',
      label: c.name,
      hint: c.document,
      to: `/clientes?focus=${c.id}`,
    }))
    const prod: Hit[] = (products.data || []).map((p) => ({
      id: `p-${p.id}`,
      group: 'Patio',
      label: p.name,
      hint: `Stock ${p.stockAvailable}/${p.stockTotal}`,
      to: `/inventario?focus=${p.id}`,
    }))
    const alq: Hit[] = (rentals.data || []).map((r) => ({
      id: `r-${r.id}`,
      group: 'Alquileres',
      label: `Alquiler #${r.id}`,
      hint: `${r.customerName || r.customerId} · ${r.status}`,
      to: `/alquileres?focus=${r.id}`,
    }))
    const all = [...nav, ...cli, ...prod, ...alq]
    if (!term) return all.slice(0, 12)
    return all
      .filter(
        (h) =>
          h.label.toLowerCase().includes(term) ||
          (h.hint || '').toLowerCase().includes(term) ||
          h.group.toLowerCase().includes(term),
      )
      .slice(0, 16)
  }, [q, customers.data, products.data, rentals.data])

  useEffect(() => setActive(0), [q, open])

  function go(hit: Hit) {
    setOpen(false)
    navigate(hit.to)
  }

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[80] flex items-start justify-center pt-[12vh] px-4">
          <motion.button
            type="button"
            aria-label="Cerrar"
            className="absolute inset-0 bg-[rgba(20,24,51,0.5)] backdrop-blur-[4px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setOpen(false)}
          />
          <motion.div
            className="relative w-full max-w-xl overflow-hidden rounded-xl border border-[rgba(0,0,0,0.08)] bg-white shadow-2xl"
            initial={{ opacity: 0, y: -16, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.98 }}
            transition={{ type: 'spring', stiffness: 400, damping: 30 }}
          >
            <div className="flex items-center gap-3 px-4 border-b border-[var(--border)]">
              <span className="text-[var(--text-muted)] text-sm font-bold">⌘K</span>
              <input
                autoFocus
                className="flex-1 py-4 outline-none text-sm bg-transparent"
                placeholder="Buscar cliente, alquiler, equipo o ir a…"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'ArrowDown') {
                    e.preventDefault()
                    setActive((a) => Math.min(hits.length - 1, a + 1))
                  } else if (e.key === 'ArrowUp') {
                    e.preventDefault()
                    setActive((a) => Math.max(0, a - 1))
                  } else if (e.key === 'Enter' && hits[active]) {
                    go(hits[active])
                  }
                }}
              />
            </div>
            <ul className="max-h-[360px] overflow-y-auto py-2">
              {hits.length === 0 && (
                <li className="px-4 py-6 text-sm text-[var(--text-muted)] text-center">Sin resultados</li>
              )}
              {hits.map((h, i) => (
                <li key={h.id}>
                  <button
                    type="button"
                    className={[
                      'w-full text-left px-4 py-2.5 flex items-center justify-between gap-3 text-sm',
                      i === active ? 'bg-[rgba(27,31,76,0.08)]' : 'hover:bg-[var(--bone)]',
                    ].join(' ')}
                    onMouseEnter={() => setActive(i)}
                    onClick={() => go(h)}
                  >
                    <div className="min-w-0">
                      <div className="font-semibold text-[var(--navy)] truncate">{h.label}</div>
                      {h.hint && <div className="text-xs text-[var(--text-muted)] truncate">{h.hint}</div>}
                    </div>
                    <span className="text-[0.65rem] uppercase tracking-wider text-[var(--text-muted)] shrink-0">
                      {h.group}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
