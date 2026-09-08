import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Logo } from '../brand/Logo'
import { useAuth } from '../modules/auth/AuthContext'
import { P } from '../lib/permissions'
import { Button } from './ui'

const links = [
  { to: '/', label: 'Hoy', permission: P.DASHBOARD_VER },
  { to: '/alquileres', label: 'Alquileres', permission: P.ALQUILERES_VER },
  { to: '/clientes', label: 'Clientes', permission: P.CLIENTES_VER },
  { to: '/inventario', label: 'Patio', permission: P.INVENTARIO_VER },
  { to: '/finanzas', label: 'Caja', permission: P.FINANZAS_VER },
  { to: '/analiticas', label: 'Analiticas', permission: P.ANALITICAS_VER },
  { to: '/documentos', label: 'Documentos', permission: P.DOCUMENTOS_VER },
  { to: '/sistema', label: 'Sistema', permission: P.USUARIOS_GESTIONAR },
]

export function Layout() {
  const { user, logout, can } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className="border-b border-white/10 bg-navy text-white md:min-h-screen md:border-b-0">
        <div className="flex items-center gap-3 px-4 py-4">
          <Logo />
          <div>
            <p className="font-display text-lg leading-none">HYEDA</p>
            <p className="text-[11px] text-white/70">Patio bajo control</p>
          </div>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col">
          {links.filter((link) => can(link.permission) || (link.to === '/sistema' && (can(P.IMPORTACION_EJECUTAR) || can(P.CONFIGURACION_GESTIONAR)))).map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) => `rounded-[10px] px-3 py-2 text-sm whitespace-nowrap ${isActive ? 'bg-white/15' : 'text-white/80 hover:bg-white/10'}`}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div>
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur">
          <div>
            <p className="text-sm font-semibold text-navy">{user?.fullName}</p>
            <p className="text-xs text-steel-muted">{user?.roles.join(' · ')}</p>
          </div>
          <div className="flex gap-2">
            {can(P.ALQUILERES_GESTIONAR) && (
              <Button onClick={() => navigate('/alquileres/nuevo')}>Abrir alquiler</Button>
            )}
            <Button variant="ghost" onClick={() => logout().then(() => navigate('/login'))}>Salir</Button>
          </div>
        </header>
        <main className="page-shell p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
