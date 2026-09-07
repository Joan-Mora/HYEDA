import api from './api'
import type { LoginResponse, User } from '../types'

const TOKEN_KEY = 'hyeda_token'
const USER_KEY = 'hyeda_user'

export async function login(username: string, password: string): Promise<User> {
  const { data } = await api.post<LoginResponse>('/auth/login', { username, password })
  const user: User = {
    username: data.username,
    fullName: data.fullName,
    roles: data.roles ?? [],
    permissions: data.permissions ?? [],
    token: data.token,
  }
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function logout(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function isAuthenticated(): boolean {
  return Boolean(getToken())
}
