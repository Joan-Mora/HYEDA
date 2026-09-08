import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { SessionResponse } from '../types/api'

const ACCESS = 'hyeda_access'
const REFRESH = 'hyeda_refresh'
const USER = 'hyeda_user'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api',
})

export function getAccessToken() {
  return localStorage.getItem(ACCESS)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER)
  return raw ? (JSON.parse(raw) as SessionResponse['user']) : null
}

export function persistSession(session: SessionResponse) {
  localStorage.setItem(ACCESS, session.accessToken)
  localStorage.setItem(REFRESH, session.refreshToken)
  localStorage.setItem(USER, JSON.stringify(session.user))
}

export function clearSession() {
  localStorage.removeItem(ACCESS)
  localStorage.removeItem(REFRESH)
  localStorage.removeItem(USER)
}

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status !== 401 || original._retry || original.url?.includes('/auth/')) {
      return Promise.reject(error)
    }
    original._retry = true
    if (!refreshing) {
      const refreshToken = localStorage.getItem(REFRESH)
      refreshing = refreshToken
        ? api.post<SessionResponse>('/auth/refresh', { refreshToken })
            .then((res) => {
              persistSession(res.data)
              return res.data.accessToken
            })
            .catch(() => {
              clearSession()
              if (!window.location.pathname.startsWith('/login')) {
                window.location.assign('/login')
              }
              return null
            })
            .finally(() => {
              refreshing = null
            })
        : Promise.resolve(null)
    }
    const token = await refreshing
    if (!token) return Promise.reject(error)
    original.headers.Authorization = `Bearer ${token}`
    return api(original)
  },
)
