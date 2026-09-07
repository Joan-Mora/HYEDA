import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { BrandLockup } from '../brand/Logo'
import { useAuth } from '../modules/auth/AuthContext'
import { Pressable } from './Overlay'

const navItems = [
  { to: '/', label: 'Hoy', end: true, icon: 'H' },
  { to: '/alquileres', label: 'Alquileres', icon: 'A' },
  { to: '/clientes', label: 'Clientes', icon: 'C' },
  { to: '/inventario', label: 'Patio', icon: 'P' },
  { to: '/finanzas', label: 'Caja', icon: '$' },
  { to: '/sistema', label: 'Sistema', icon: '⚙' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col bg-[var(--bg)]">
      <header
        className="sticky top-0 z-40 h-[var(--header-h)] flex items-center justify-between gap-4 px-4 md:px-6"
        style={{
          background: 'linear-gradient(90deg, var(--navy-deep) 0%, var(--navy) 55%, #1f2458 100%)',
          boxShadow: '0 8px 28px rgba(20, 24, 51, 0.35)',
        }}
      >
        <BrandLockup inverted />

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="hidden md:inline-flex items-center gap-2 rounded-lg border border-white/20 bg-white/5 px-3 py-1.5 text-xs text-white/70 hover:bg-white/10"
            onClick={() =>
              window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
            }
          >
            <span>Buscar</span>
            <kbd className="rounded bg-white/15 px-1.5 py-0.5 text-[0.65rem] font-bold">Ctrl K</kbd>
          </button>
          <div className="hidden sm:block text-right leading-tight">
            <div className="text-sm font-semibold text-[#F4F4F6]">
              {user?.fullName || user?.username || 'Usuario'}
            </div>
            <div className="text-[0.7rem] uppercase tracking-wider text-[#F4F4F6]/70">
              {(user?.roles || []).join(' · ') || 'Sin rol'}
            </div>
          </div>
          <Pressable
            className="btn btn-primary !py-2 !px-3 text-xs sm:text-sm"
            onClick={() => navigate('/alquileres/nuevo')}
          >
            Abrir alquiler
          </Pressable>
          <Pressable
            className="btn btn-ghost !border-[#ffffff55] !text-[#F4F4F6] !py-2 hover:!bg-[#ffffff18]"
            onClick={handleLogout}
          >
            Salir
          </Pressable>
        </div>
      </header>

      <div className="flex flex-1 min-h-0">
        <aside className="hidden md:flex w-[232px] shrink-0 flex-col border-r border-[var(--border)] bg-[var(--white)]">
          <div className="px-4 pt-5 pb-2">
            <p className="text-[0.65rem] font-bold uppercase tracking-[0.16em] text-[var(--accent)]">
              Patio · Granada
            </p>
            <p className="text-xs text-[var(--steel-muted)] mt-1 leading-snug">
              Material en calle, riesgo y caja en un tablero.
            </p>
          </div>
          <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
            {navItems.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  ['nav-link', isActive ? 'nav-link-active' : ''].join(' ')
                }
              >
                <span className="nav-icon">{link.icon}</span>
                <span>{link.label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="p-4 border-t border-[var(--border)]">
            <div
              className="rounded-lg p-3 text-xs leading-relaxed"
              style={{
                background:
                  'linear-gradient(145deg, rgba(27,31,76,0.06) 0%, rgba(230,81,0,0.08) 100%)',
                color: 'var(--steel-muted)',
              }}
            >
              <strong style={{ color: 'var(--navy)' }}>NIT 000.000.000-0</strong>
              <br />
              la propietaria
            </div>
          </div>
        </aside>

        <main className="flex-1 min-w-0 overflow-auto">
          <div className="md:hidden border-b border-[var(--border)] bg-white px-3 py-2 overflow-x-auto flex gap-2">
            {navItems.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  ['chip-nav', isActive ? 'chip-nav-active' : ''].join(' ')
                }
              >
                {link.label}
              </NavLink>
            ))}
          </div>
          <div className="page-shell p-5 md:p-8 max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
