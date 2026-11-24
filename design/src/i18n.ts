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

/** Перевод с подстановкой: tf('k', 'Набрано {0} из {1}', a, b). */
export function tf(key: string, ru: string, ...args: unknown[]) {
  return t(key, ru).replace(/\{(\d+)\}/g, (_, i) => String(args[Number(i)] ?? ''))
}

const LOCALES: Record<string, string> = { ru: 'ru-RU', ky: 'ky-KG', en: 'en-GB' }
/** Локаль для дат и чисел (toLocaleDateString и т. п.) по выбранному языку. */
export const locale = () => LOCALES[state.lang] ?? state.lang

/**
 * Системные тексты, которые присылает сервер (названия уровней и значков, строки начисления очков, описания ролей).
 * Это не редакционный контент, поэтому их перевод лежит в тех же словарях интерфейса под ключом «srv:русский текст».
 */
const SERVER_PATTERNS: [RegExp, string][] = [
  [/^Серия из (\d+) верных ответов подряд$/, 'srv.streak'],
  [/^Бонус за завершение уже получен (\d+) раза сегодня$/, 'srv.completeLimit'],
  [/^Значок «(.+)»$/, 'srv.badge'],
]
export function ts(text: string): string
export function ts(text: string | null | undefined): string | null | undefined
export function ts(text: string | null | undefined) {
  if (!text || state.lang === DEFAULT) return text
  const direct = state.dict[`srv:${text}`]
  if (direct) return direct
  for (const [re, key] of SERVER_PATTERNS) {
    const m = text.match(re)
    if (m && state.dict[key]) return state.dict[key].replace('{0}', key === 'srv.badge' ? ts(m[1]) : m[1])
  }
  return text
}

/**
 * Слово после числа: tp('plural.points', n, 'очко', 'очка', 'очков').
 * Русский - по своим трём формам; в словарях других языков ключи `.one` и `.other` (правила Intl.PluralRules).
 */
export function tp(key: string, n: number, one: string, few: string, many: string) {
  if (state.lang !== DEFAULT) {
    const form = new Intl.PluralRules(locale()).select(n)
    const word = state.dict[`${key}.${form}`] ?? state.dict[`${key}.other`]
    if (word) return word
  }
  const last = Math.abs(n) % 10, tail = Math.abs(n) % 100
  if (last === 1 && tail !== 11) return one
  if (last >= 2 && last <= 4 && (tail < 12 || tail > 14)) return few
  return many
}

/**
 * Справочник подписей, который переводится при каждом обращении: lrec('role', { USER: 'Пользователь' })[r].
 * Нужен для констант уровня модуля - обычный объект вычислился бы один раз и не сменил бы язык.
 * Вложенные массивы и объекты переводятся по пути ключей: lrec('auth.titles', { login: ['А', 'Б'] }).login[1].
 * Поле value (значение для сервера, а не подпись) не переводится.
 */
export function lrec<T extends object>(prefix: string, source: T): T {
  return new Proxy(source, {
    get(target, prop, receiver) {
      const value = Reflect.get(target, prop, receiver)
      if (typeof prop !== 'string') return value
      if (typeof value === 'string') return prop === 'value' ? value : t(`${prefix}.${prop}`, value)
      if (value && typeof value === 'object') return lrec(`${prefix}.${prop}`, value)
      return value
    },
  })
}

const subscribe = (l: () => void) => { listeners.add(l); return () => { listeners.delete(l) } }
const snapshot = () => state

/** Подписка компонента на смену языка. */
export function useI18n() {
  const s = useSyncExternalStore(subscribe, snapshot)
  return { lang: s.lang, languages: s.languages, t: (key: string, ru: string) => s.dict[key] ?? ru, setLang }
}
