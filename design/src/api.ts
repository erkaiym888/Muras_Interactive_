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
