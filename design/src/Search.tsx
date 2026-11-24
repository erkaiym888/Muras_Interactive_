import { Fragment, useEffect, useId, useRef, useState } from 'react'
import { search } from './api'
import { lrec, t } from './i18n'
import type { SearchKind, SearchResult } from './api'

/**
 * Поиск по всем разделам (FR-4.1) - строка посередине шапки, как в Gmail.
 * С 2 символов под строкой появляется выпадающий список найденного (названия, вид, фрагмент
 * с подсвеченными словами). Переход - только по выбору варианта: кликом или стрелками и Enter.
 * Отдельной страницы поиска нет.
 *
 * FR-4.2: подсказки с 2 символов, не больше 7 вариантов; сервер учитывает формы слова и опечатки
 * (до 2 ошибок в слове) - тогда над списком пишется, по какому запросу показаны результаты.
 *
 * FR-4.3: результаты сгруппированы по виду материала (обряды, герои, произведения, юрта) с заголовками,
 * сверху - фильтр по виду с числом найденного.
 */

export const MIN_QUERY = 2
const LIMIT = 7
const DEBOUNCE_MS = 250
const KIND_LABEL: Record<SearchKind, string> = lrec('kind', { rite: 'Обряд', hero: 'Герой', work: 'Произведение', yurt: 'Элемент юрты' })
const GROUP_LABEL: Record<SearchKind, string> = lrec('search.group', { rite: 'Обряды', hero: 'Герои', work: 'Произведения', yurt: 'Юрта' })
const KINDS: SearchKind[] = ['rite', 'hero', 'work', 'yurt']

const SearchIcon = () => <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4.5 4.5" /></svg>

type State =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; query: string; corrected: string | null; total: number; counts: Record<SearchKind, number>; results: SearchResult[] }
  | { status: 'error' }

export function HeaderSearch({ onOpen }: { onOpen: (r: SearchResult) => void }) {
  const [value, setValue] = useState('')
  const [state, setState] = useState<State>({ status: 'idle' })
  const [open, setOpen] = useState(false)          // выпадающий список
  const [mobileOpen, setMobileOpen] = useState(false) // поле на телефоне
  const [active, setActive] = useState(-1)
  const [kind, setKind] = useState<SearchKind | null>(null) // фильтр по виду (FR-4.3)
  const [counts, setCounts] = useState<Record<SearchKind, number> | null>(null)
  const box = useRef<HTMLDivElement>(null)
  const input = useRef<HTMLInputElement>(null)
  const seq = useRef(0)
  const listId = useId()

  // Запрос с задержкой: не на каждую букву, а когда пользователь остановился
  useEffect(() => {
    const q = value.trim()
    if (q.length < MIN_QUERY) { setState({ status: 'idle' }); setActive(-1); setCounts(null); return }
    setState({ status: 'loading' })
    const my = ++seq.current
    const t = setTimeout(() => {
      search(q, LIMIT, kind)
        .then(r => { if (my === seq.current) { setState({ status: 'ready', query: r.query, corrected: r.corrected, total: r.total, counts: r.counts, results: r.results }); setCounts(r.counts); setActive(-1) } })
        .catch(() => { if (my === seq.current) setState({ status: 'error' }) })
    }, DEBOUNCE_MS)
    return () => clearTimeout(t)
  }, [value, kind])

  // Новый запрос - фильтр сбрасывается на «Все»
  const prevValue = useRef(value)
  useEffect(() => { if (prevValue.current.trim() !== value.trim()) setKind(null); prevValue.current = value }, [value])

  // Клик мимо - закрываем список
  useEffect(() => {
    const onDown = (e: MouseEvent) => { if (box.current && !box.current.contains(e.target as Node)) { setOpen(false); setMobileOpen(false) } }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [])

  useEffect(() => { if (mobileOpen) input.current?.focus() }, [mobileOpen])

  const results = state.status === 'ready' ? state.results : []

  const choose = (r: SearchResult) => {
    onOpen(r)
    setValue(''); setKind(null); setOpen(false); setMobileOpen(false); input.current?.blur()
  }

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown' && results.length) { e.preventDefault(); setOpen(true); setActive(i => (i + 1) % results.length) }
    else if (e.key === 'ArrowUp' && results.length) { e.preventDefault(); setOpen(true); setActive(i => (i <= 0 ? results.length : i) - 1) }
    else if (e.key === 'Enter') {
      e.preventDefault()
      // Enter открывает только выбранный стрелками вариант (или единственный найденный)
      const pick = active >= 0 ? results[active] : results.length === 1 ? results[0] : null
      if (pick) choose(pick)
    } else if (e.key === 'Escape') {
      if (open && value) { setOpen(false) } else { setValue(''); setMobileOpen(false); input.current?.blur() }
    }
  }

  const showList = open && value.trim().length >= MIN_QUERY
  const optionId = (i: number) => `${listId}-opt-${i}`

  return <div ref={box} className={mobileOpen ? 'header-search open' : 'header-search'}>
    <button type="button" className="search-toggle" onClick={() => setMobileOpen(true)} aria-label={t('search.s1', "Открыть поиск")}><SearchIcon /></button>
    <div className={showList ? 'search-field with-list' : 'search-field'}>
      <SearchIcon />
      <input ref={input} type="text" value={value} autoComplete="off" spellCheck={false}
        onChange={e => { setValue(e.target.value); setOpen(true) }}
        onFocus={() => setOpen(true)} onKeyDown={onKey}
        placeholder={t('search.placeholder', 'Поиск по музею')} aria-label={t('search.placeholder', 'Поиск по музею')}
        role="combobox" aria-expanded={showList} aria-controls={listId} aria-autocomplete="list"
        aria-activedescendant={showList && active >= 0 ? optionId(active) : undefined} />
      {value && <button type="button" className="search-clear" onClick={() => { setValue(''); input.current?.focus() }} aria-label={t('search.s2', "Очистить")}>×</button>}
      <button type="button" className="search-close" onClick={() => { setMobileOpen(false); setOpen(false) }} aria-label={t('search.s3', "Закрыть поиск")}>←</button>

      {showList && <div className="search-drop">
        {counts && KINDS.some(k => counts[k] > 0) && <div className="drop-filter" role="group" aria-label={t('search.s4', "Показать только")}>
          <button type="button" className={kind === null ? 'on' : ''} aria-pressed={kind === null}
            onMouseDown={e => e.preventDefault()} onClick={() => setKind(null)}>{t('search.s5', "Все")}{' '}<span>{KINDS.reduce((n, k) => n + counts[k], 0)}</span></button>
          {KINDS.filter(k => counts[k] > 0).map(k => <button type="button" key={k} className={`k-${k}${kind === k ? ' on' : ''}`} aria-pressed={kind === k}
            onMouseDown={e => e.preventDefault()} onClick={() => setKind(kind === k ? null : k)}><i aria-hidden="true" />{GROUP_LABEL[k]} <span>{counts[k]}</span></button>)}
        </div>}
        {state.status === 'loading' && <p className="drop-note" role="status">{t('search.loading', 'Ищем…')}</p>}
        {state.status === 'error' && <p className="drop-note" role="alert">{t('search.s6', "Поиск сейчас недоступен")}</p>}
        {state.status === 'ready' && results.length === 0 && <p className="drop-note" role="status">{t('search.empty', 'Ничего не найдено. Попробуйте начало слова или другое слово.')}</p>}
        {state.status === 'ready' && state.corrected && results.length > 0 &&
          <p className="drop-note corrected" role="status">{t('search.s7', "Показаны результаты для «")}<b>{state.corrected}</b>»</p>}
        <ul id={listId} role="listbox" aria-label={t('search.s8', "Найденные материалы")}>
          {results.map((r, i) => <Fragment key={`${r.kind}-${r.id ?? r.key}`}>
            {(i === 0 || results[i - 1].kind !== r.kind) && <li role="presentation" className="drop-group">{GROUP_LABEL[r.kind]}
              {state.status === 'ready' && state.counts[r.kind] > 0 && <span>{state.counts[r.kind]}</span>}</li>}
            <li id={optionId(i)} role="option" aria-selected={i === active}
            className={`drop-item k-${r.kind}${i === active ? ' active' : ''}`}
            onMouseEnter={() => setActive(i)} onMouseDown={e => e.preventDefault()} onClick={() => choose(r)}>
            <span className="drop-dot" aria-hidden="true" />
            <span className="drop-text">
              <b><Highlighted text={r.title} words={queryWords(state.status === 'ready' && state.corrected ? `${value} ${state.corrected}` : value)} /></b>
              {r.snippet && <span className="drop-snippet"><Highlighted text={r.snippet} /></span>}
            </span>
            <span className="drop-kind">{KIND_LABEL[r.kind]}</span>
          </li></Fragment>)}
        </ul>
        {state.status === 'ready' && state.total > results.length &&
          <p className="drop-note more">{t('search.s9', "Показаны первые")}{' '}{results.length}{' '}{t('search.s10', "— уточните запрос, чтобы найти остальное")}</p>}
      </div>}
    </div>
  </div>
}

const queryWords = (q: string) => q.toLowerCase().split(/[^\p{L}\p{N}]+/u).filter(w => w.length >= MIN_QUERY)

/**
 * Подсветка: [[слово]] из ответа сервера -> <mark>; для названия - совпадение с началом слов запроса.
 * Текст не вставляется как HTML.
 */
export function Highlighted({ text, words }: { text: string; words?: string[] }) {
  if (words && words.length) {
    const escaped = words.map(w => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    const re = new RegExp(`(${escaped.join('|')})`, 'giu')
    return <>{text.split(re).map((p, i) => i % 2 === 1 ? <mark key={i}>{p}</mark> : <span key={i}>{p}</span>)}</>
  }
  const parts = text.split(/(\[\[.*?\]\])/g)
  return <>{parts.map((p, i) => p.startsWith('[[') && p.endsWith(']]') ? <mark key={i}>{p.slice(2, -2)}</mark> : <span key={i}>{p}</span>)}</>
}
