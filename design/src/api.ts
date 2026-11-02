// Запросы к Spring Boot. Адрес /api в разработке проксируется на localhost:8080 (см. vite.config.ts).

export type RiteCategory = { id: number; name: string }

export type RiteSummary = {
  id: number
  title: string
  category: string
  summary: string
  imageUrl: string | null
}

export type RiteStep = { order: number; title: string | null; text: string }
export type RiteImage = { url: string; caption: string | null }

export type RiteDetail = {
  id: number
  title: string
  category: string
  summary: string
  description: string | null
  symbolism: string | null
  sources: string | null
  steps: RiteStep[]
  images: RiteImage[]
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`Ошибка ${response.status}`)
  return response.json() as Promise<T>
}

export const fetchCategories = () => getJson<RiteCategory[]>('/api/rite-categories')
export const fetchRites = () => getJson<RiteSummary[]>('/api/rites')
export const fetchRite = (id: number) => getJson<RiteDetail>(`/api/rites/${id}`)

// ---------- Учётные записи (FR-6.1) ----------

export type User = { id: number; email: string; displayName: string; role: string }

/** Ошибка от сервера: статус, ошибки по полям формы и общее сообщение. */
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, fieldErrors: Record<string, string> = {}, message = '') {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  let data: { errors?: Record<string, string>; message?: string } | null = null
  try { data = await response.json() } catch { /* тела нет */ }
  return new ApiError(response.status, data?.errors ?? {}, data?.message ?? '')
}

/** Перед каждым POST берём свежий CSRF-токен (после входа и выхода он меняется). */
async function post(url: string, body?: Record<string, string>, asForm = false): Promise<void> {
  const csrfResponse = await fetch('/api/auth/csrf')
  if (!csrfResponse.ok) throw await toApiError(csrfResponse)
  const csrf = (await csrfResponse.json()) as { token: string; headerName: string }

  const headers: Record<string, string> = { [csrf.headerName]: csrf.token }
  let payload: BodyInit | undefined
  if (body && asForm) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    payload = new URLSearchParams(body)
  } else if (body) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }
  const response = await fetch(url, { method: 'POST', headers, body: payload })
  if (!response.ok) throw await toApiError(response)
}

/** Текущий пользователь или null, если не вошёл. */
export async function fetchMe(): Promise<User | null> {
  const response = await fetch('/api/auth/me')
  if (response.status === 204 || response.status === 401) return null
  if (!response.ok) throw await toApiError(response)
  return response.json() as Promise<User>
}

export const login = (email: string, password: string) =>
  post('/api/auth/login', { username: email, password }, true)

export const register = (displayName: string, email: string, password: string) =>
  post('/api/auth/register', { displayName, email, password })

export const logout = () => post('/api/auth/logout')

export const requestPasswordReset = (email: string) => post('/api/auth/forgot', { email })

export const resetPassword = (token: string, password: string) =>
  post('/api/auth/reset', { token, password })
