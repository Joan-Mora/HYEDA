import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { api, clearSession, getStoredUser, persistSession } from '../../services/api'
import type { SessionResponse, UserProfile } from '../../types/api'
import { hasPermission } from '../../lib/permissions'

type AuthContextValue = {
  user: UserProfile | null
  login: (username: string, password: string) => Promise<UserProfile>
  logout: () => Promise<void>
  can: (permission: string) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(() => getStoredUser())

  const value = useMemo<AuthContextValue>(() => ({
    user,
    async login(username, password) {
      const { data } = await api.post<SessionResponse>('/auth/login', { username, password })
      persistSession(data)
      setUser(data.user)
      return data.user
    },
    async logout() {
      try {
        await api.post('/auth/logout', { refreshToken: localStorage.getItem('hyeda_refresh') })
      } catch {
        // El cierre local importa mas que el del servidor.
      }
      clearSession()
      setUser(null)
    },
    can(permission) {
      return hasPermission(user?.permissions, permission)
    },
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider requerido')
  return ctx
}
