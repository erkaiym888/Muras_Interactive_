// Запросы к Spring Boot. Адрес /api в разработке проксируется на localhost:8080 (см. vite.config.ts).

export type RiteCategory = { id: number; name: string }

export type RiteSummary = {
  id: number
  title: string
  categoryId: number
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
/** Сортировка каталога (FR-2.3): по алфавиту, в обратном порядке, сначала новые, сначала давние. */
export type RiteSort = 'title' | 'title_desc' | 'new' | 'old'

export function fetchRites(params: { categoryId?: number | null; q?: string; sort?: RiteSort } = {}) {
  const query = new URLSearchParams()
  if (params.categoryId != null) query.set('categoryId', String(params.categoryId))
  if (params.q?.trim()) query.set('q', params.q.trim())
  if (params.sort) query.set('sort', params.sort)
  const qs = query.toString()
  return getJson<RiteSummary[]>(qs ? `/api/rites?${qs}` : '/api/rites')
}
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

/** Запрос с JSON-телом и CSRF-токеном (POST, PUT, DELETE). Возвращает ответ сервера, если он есть. */
async function send<T>(method: 'POST' | 'PUT' | 'DELETE', url: string, body?: unknown): Promise<T> {
  const csrfResponse = await fetch('/api/auth/csrf')
  if (!csrfResponse.ok) throw await toApiError(csrfResponse)
  const csrf = (await csrfResponse.json()) as { token: string; headerName: string }

  const headers: Record<string, string> = { [csrf.headerName]: csrf.token }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  if (!response.ok) throw await toApiError(response)
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

async function getAuthed<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw await toApiError(response)
  return response.json() as Promise<T>
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

// ---------- Фольклор: герои и произведения (FR-3.1, FR-3.2) ----------

export type FolkloreKind = 'hero' | 'work'
export type FolkloreType = { id: number; name: string }

export type FolkloreItem = {
  kind: FolkloreKind
  id: number
  title: string
  type: string
  summary: string | null
  imageUrl: string | null
}

export type FolkloreLink = { kind: FolkloreKind; id: number; title: string }
export type FolkloreImage = { url: string; caption: string | null }

export type FolkloreDetail = {
  kind: FolkloreKind
  id: number
  title: string
  type: string
  description: string | null
  related: FolkloreLink[]
  images: FolkloreImage[]
}

export const fetchFolkloreTypes = () => getJson<FolkloreType[]>('/api/folklore/types')
export const fetchFolkloreItems = () => getJson<FolkloreItem[]>('/api/folklore/items')
export const fetchFolkloreDetail = (kind: FolkloreKind, id: number) =>
  getJson<FolkloreDetail>(`/api/folklore/${kind === 'hero' ? 'heroes' : 'works'}/${id}`)

// ---------- Редактор контента (FR-7.1) ----------

export type RiteStatus = 'DRAFT' | 'PUBLISHED' | 'HIDDEN'

/** Роли, которым доступно управление содержимым. */
export const canEditContent = (user: User | null) => user?.role === 'EDITOR' || user?.role === 'ADMIN'

export type EditorRiteRow = {
  id: number
  title: string
  categoryId: number
  category: string
  status: RiteStatus
  stepCount: number
  imageUrl: string | null
}

export type RiteForm = {
  id?: number | null
  title: string
  categoryId: number | null
  status: RiteStatus
  summary: string
  description: string
  symbolism: string
  sources: string
  steps: { title: string; text: string }[]
  images: { url: string; caption: string }[]
}

export type EditorCategory = { id: number; name: string; riteCount: number }

export const editorRites = () => getAuthed<EditorRiteRow[]>('/api/editor/rites')
export const editorRite = (id: number) => getAuthed<RiteForm>(`/api/editor/rites/${id}`)
export const createRite = (form: RiteForm) => send<RiteForm>('POST', '/api/editor/rites', form)
export const updateRite = (id: number, form: RiteForm) => send<RiteForm>('PUT', `/api/editor/rites/${id}`, form)
export const deleteRite = (id: number) => send<void>('DELETE', `/api/editor/rites/${id}`)

export const editorCategories = () => getAuthed<EditorCategory[]>('/api/editor/rite-categories')
export const createCategory = (name: string) => send<EditorCategory>('POST', '/api/editor/rite-categories', { name })
export const renameCategory = (id: number, name: string) => send<EditorCategory>('PUT', `/api/editor/rite-categories/${id}`, { name })
export const deleteCategory = (id: number) => send<void>('DELETE', `/api/editor/rite-categories/${id}`)

// ---------- Редактор: фольклор (FR-7.1) ----------

export type EditorImage = { url: string; caption: string }

export type EditorFolkloreRow = {
  kind: FolkloreKind
  id: number
  title: string
  typeId: number
  type: string
  linkCount: number
  imageUrl: string | null
}

export type FolkloreHeroForm = {
  id?: number | null
  name: string
  typeId: number | null
  description: string
  workIds: number[]
  images: EditorImage[]
}

export type FolkloreWorkForm = {
  id?: number | null
  title: string
  typeId: number | null
  summary: string
  heroIds: number[]
  images: EditorImage[]
}

export const editorFolklore = () => getAuthed<EditorFolkloreRow[]>('/api/editor/folklore')
export const editorFolkloreTypes = () => getAuthed<FolkloreType[]>('/api/editor/folklore/types')
export const editorHero = (id: number) => getAuthed<FolkloreHeroForm>(`/api/editor/folklore/heroes/${id}`)
export const createHero = (form: FolkloreHeroForm) => send<FolkloreHeroForm>('POST', '/api/editor/folklore/heroes', form)
export const updateHero = (id: number, form: FolkloreHeroForm) => send<FolkloreHeroForm>('PUT', `/api/editor/folklore/heroes/${id}`, form)
export const deleteHero = (id: number) => send<void>('DELETE', `/api/editor/folklore/heroes/${id}`)
export const editorWork = (id: number) => getAuthed<FolkloreWorkForm>(`/api/editor/folklore/works/${id}`)
export const createWork = (form: FolkloreWorkForm) => send<FolkloreWorkForm>('POST', '/api/editor/folklore/works', form)
export const updateWork = (id: number, form: FolkloreWorkForm) => send<FolkloreWorkForm>('PUT', `/api/editor/folklore/works/${id}`, form)
export const deleteWork = (id: number) => send<void>('DELETE', `/api/editor/folklore/works/${id}`)
