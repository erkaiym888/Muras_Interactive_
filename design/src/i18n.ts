import { useSyncExternalStore } from 'react'

/**
 * Многоязычность интерфейса (FR-8.1).
 * Тексты интерфейса лежат в public/locales/{код}.json; русский встроен прямо в вызовы t(ключ, 'русский текст').
 * Чтобы добавить кыргызский или английский, не нужно менять код: положить public/locales/ky.json
 * (копия ru.json с переводом) и включить язык в разделе «Пользователи → Языки».
 * Язык контента сервер выбирает по заголовку X-Lang, который ставит api.ts.
 */

export type Language = { code: string; nativeName: string }

const STORAGE_KEY = 'muras-lang'
const DEFAULT = 'ru'

type State = { lang: string; languages: Language[]; dict: Record<string, string> }

/** FR-8.2: язык из ссылки (?lang=ky) важнее сохранённого - чтобы ссылкой можно было поделиться. */
function readStored() {
  const fromUrl = new URLSearchParams(window.location.search).get('lang')
  if (fromUrl) return fromUrl.toLowerCase()
  try { return localStorage.getItem(STORAGE_KEY) || DEFAULT } catch { return DEFAULT }
}

/** Язык в адресной строке: ?lang=ky; для основного языка параметр убираем. Страница не перезагружается. */
function writeUrl(code: string) {
  const url = new URL(window.location.href)
  if (code === DEFAULT) url.searchParams.delete('lang'); else url.searchParams.set('lang', code)
  window.history.replaceState(window.history.state, '', url)
}

let state: State = { lang: readStored(), languages: [{ code: DEFAULT, nativeName: 'Русский' }], dict: {} }
const listeners = new Set<() => void>()
const emit = (next: Partial<State>) => { state = { ...state, ...next }; listeners.forEach(l => l()) }

/** Язык для заголовка X-Lang. */
export const currentLang = () => state.lang

async function loadDictionary(code: string): Promise<Record<string, string>> {
  if (code === DEFAULT) return {}
  try {
    const response = await fetch(`/locales/${code}.json`)
    return response.ok ? await response.json() as Record<string, string> : {}
  } catch { return {} }
}

/** Сменить язык: словарь интерфейса подгружается, контент перезапрашивается страницами. */
export async function setLang(code: string) {
  const dict = await loadDictionary(code)
  try { localStorage.setItem(STORAGE_KEY, code) } catch { /* приватный режим */ }
  document.documentElement.lang = code
  writeUrl(code)
  emit({ lang: code, dict })
}

/** При старте: какие языки включены на сервере; выбранный ранее, но выключенный язык сбрасываем. */
export async function initLanguages() {
  try {
    const response = await fetch('/api/languages')
    if (!response.ok) return
    const data = await response.json() as { defaultCode: string; languages: Language[] }
    const languages = data.languages.length ? data.languages : state.languages
    const lang = languages.some(l => l.code === state.lang) ? state.lang : data.defaultCode || DEFAULT
    emit({ languages })
    if (lang !== DEFAULT || state.lang !== lang) await setLang(lang)
  } catch { /* сервер недоступен - остаёмся на русском */ }
}

/** Перевод строки интерфейса: из словаря языка, иначе русский текст из вызова. */
export function t(key: string, ru: string) {
  return state.dict[key] ?? ru
}

const subscribe = (l: () => void) => { listeners.add(l); return () => { listeners.delete(l) } }
const snapshot = () => state

/** Подписка компонента на смену языка. */
export function useI18n() {
  const s = useSyncExternalStore(subscribe, snapshot)
  return { lang: s.lang, languages: s.languages, t: (key: string, ru: string) => s.dict[key] ?? ru, setLang }
}
