import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../modules/auth/AuthContext'
import { isAuthenticated as hasToken } from '../services/auth'

export default function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated && !hasToken()) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
