import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Logo, { BrandLockup } from '../../brand/Logo'
import { company } from '../../brand/company'
import { Alert, Button } from '../../components/ui'
import { getErrorMessage } from '../../services/api'
import { useAuth } from './AuthContext'

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(getErrorMessage(err, 'Credenciales inválidas'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-[1.15fr_0.85fr]">
      <section className="relative hidden lg:flex flex-col justify-between p-10 xl:p-14 overflow-hidden login-hero">
        <div className="login-hero__mesh" aria-hidden />
        <div className="login-hero__grain" aria-hidden />

        <div className="relative z-[1] anim-rise">
          <BrandLockup inverted />
        </div>

        <div className="relative z-[1] max-w-xl anim-rise anim-rise-delay-1">
          <p className="text-[0.7rem] uppercase tracking-[0.2em] text-white/50 font-bold mb-4">
            Centro de control del patio
          </p>
          <Logo size={96} variant="light" className="mb-6 opacity-95" />
          <h1 className="font-display text-5xl xl:text-6xl text-white uppercase leading-[0.98]">
            Quién tiene el equipo.
            <span className="block text-[var(--accent-hover)] mt-1">Quién debe.</span>
            <span className="block mt-1">Qué falta en bodega.</span>
          </h1>
          <p className="mt-6 text-white/75 text-base leading-relaxed max-w-md">
            HYEDA no es un menú de módulos: es el tablero con el que la propietaria abre el día en Granada —
            alquileres en calle, riesgo del cliente y caja, en pocos clics.
          </p>
          <ul className="mt-8 grid gap-3 text-sm text-white/80">
            {[
              'Mesa de mando · material en calle y alertas',
              'Ficha 360 · reputación, antecedentes y cartera',
              'Hub de alquileres · documento, entrega y cierre',
            ].map((line) => (
              <li key={line} className="flex gap-3 items-start">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-[var(--accent)] shrink-0" />
                {line}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative z-[1] text-xs uppercase tracking-[0.14em] text-white/45">
          {[company.city, company.nit && `NIT ${company.nit}`, company.owner].filter(Boolean).join(' · ')}
        </p>
      </section>

      <section className="relative flex items-center justify-center px-6 py-12 bg-[var(--bone)] overflow-hidden">
        <div
          className="pointer-events-none absolute inset-0 opacity-60"
          style={{
            background:
              'radial-gradient(ellipse at 80% 10%, rgba(230,81,0,0.08), transparent 40%), radial-gradient(ellipse at 10% 90%, rgba(27,31,76,0.06), transparent 45%)',
          }}
        />
        <div className="relative w-full max-w-[400px] anim-rise">
          <div className="lg:hidden mb-8">
            <BrandLockup />
          </div>

          <div className="panel p-8 shadow-[var(--shadow-md)] border-t-4 border-t-[var(--accent)]">
            <p className="text-[0.65rem] font-bold uppercase tracking-[0.16em] text-[var(--accent)]">
              Acceso operativo
            </p>
            <h2 className="font-display text-3xl text-[var(--navy)] uppercase mt-2">Entrar</h2>
            <p className="mt-2 text-sm text-[var(--text-muted)] leading-relaxed">
              Usuario del patio. Misma sesión para Hoy, Alquileres, Clientes, Patio y Caja.
            </p>

            <form onSubmit={handleSubmit} className="mt-8 space-y-4">
              <label className="block space-y-1.5">
                <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                  Usuario
                </span>
                <input
                  className="field-input"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  required
                />
              </label>
              <label className="block space-y-1.5">
                <span className="text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)]">
                  Contraseña
                </span>
                <input
                  type="password"
                  className="field-input"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </label>

              {error && <Alert>{error}</Alert>}

              <Button type="submit" disabled={loading} className="w-full !py-3.5 !text-base">
                {loading ? 'Abriendo patio…' : 'Abrir centro de control'}
              </Button>
            </form>
          </div>
        </div>
      </section>
    </div>
  )
}
