import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../modules/auth/AuthContext'

export function ProtectedRoute({ permission }: { permission?: string }) {
  const { user, can } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (permission && !can(permission)) {
    return <div className="p-8 text-sm text-steel-muted">Tu rol no tiene acceso a esta seccion.</div>
  }
  return <Outlet />
}
