export function money(value?: number | string | null) {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(amount)
}

export function dateEs(value?: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(value))
}

export function datetimeEs(value?: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}

export function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error && 'response' in error) {
    const data = (error as { response?: { data?: { message?: string } } }).response?.data
    if (data?.message) return data.message
  }
  if (error instanceof Error) return error.message
  return 'Ocurrio un error inesperado'
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export function plusDaysIso(days: number) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}
