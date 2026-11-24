import { lrec, t, tf } from './i18n'
import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { fetchRelations } from './api'
import type { RelationKind, RelationNode, Relations } from './api'

/**
 * Связи между разделами (FR-3.3) - ссылками прямо в тексте, как в энциклопедии.
 * Если в описании обряда упомянут «Манас», слово становится ссылкой на его страницу
 * (с учётом падежей: «Манаса», «Манасом»). Связи, которые в тексте не упомянуты,
 * перечисляются одной строкой «Связано также: …» - тоже ссылками.
 * Какие материалы связаны, задаёт редактор в форме обряда; сайт показывает связи с обеих сторон.
 */

export type OpenNode = (node: RelationNode) => void

const nodeKey = (n: RelationNode) => `${n.kind}:${n.id ?? n.key}`
const KIND_LABEL: Record<RelationKind, string> = lrec('relations.kind', { rite: 'обряд', hero: 'герой', work: 'произведение', yurt: 'элемент юрты' })

/** Связанные материалы одним списком (прямые и косвенные). null - ещё загружаются или не удалось. */
export function useRelationLinks(kind: RelationKind, id: number | string | null): RelationNode[] | null {
  const [links, setLinks] = useState<RelationNode[] | null>(null)
  useEffect(() => {
    if (id === null) return
    let alive = true
    setLinks(null)
    fetchRelations(kind, id)
      .then((r: Relations) => { if (alive) setLinks([...r.rites, ...r.heroes, ...r.works, ...r.yurtElements]) })
      .catch(() => { if (alive) setLinks([]) })
    return () => { alive = false }
  }, [kind, id])
  return links
}

const escape = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * Шаблон названия с окончаниями: «Манас» -> Манас, Манаса, Манасом; «Каныкей» -> Каныкей, Каныкеи, Каныкею.
 * Окончание добавляется только к последнему слову; у коротких слов - не больше двух букв.
 */
function titlePattern(title: string) {
  const words = title.trim().split(/\s+/)
  return words.map((w, i) => {
    if (i < words.length - 1) return escape(w)
    const base = w.length > 4 ? w.replace(/[аяоеёйьыиуюэ]$/iu, '') : w
    return escape(base) + (w.length > 4 ? '\\p{L}{0,3}' : '\\p{L}{0,2}')
  }).join('\\s+')
}

/**
 * Создаётся на страницу: помнит, какие связи уже стали ссылками в тексте
 * (каждая - только при первом упоминании) и какие остались для строки «Связано также».
 */
export class Linker {
  private readonly used = new Set<string>()
  private readonly regex: RegExp | null
  private readonly byText: Map<string, RelationNode[]>
  private readonly order: string[]

  private readonly links: RelationNode[]
  readonly onOpen: OpenNode

  constructor(links: RelationNode[], onOpen: OpenNode) {
    this.links = links
    this.onOpen = onOpen
    this.byText = new Map()
    const patterns: { title: string; pattern: string }[] = []
    for (const n of links) {
      const p = titlePattern(n.title)
      if (!this.byText.has(p)) { this.byText.set(p, []); patterns.push({ title: n.title, pattern: p }) }
      this.byText.get(p)!.push(n)
    }
    // Длинные названия первыми, чтобы «Ас берүү» не разбилось на части
    patterns.sort((a, b) => b.title.length - a.title.length)
    this.regex = patterns.length
      ? new RegExp(`(?<!\\p{L})(${patterns.map(p => `(${p.pattern})`).join('|')})(?!\\p{L})`, 'giu')
      : null
    this.order = patterns.map(p => p.pattern)
  }

  /** Текст со ссылками на связанные материалы. */
  text(text: string | null | undefined): ReactNode {
    if (!text) return text ?? null
    if (!this.regex) return text
    const out: ReactNode[] = []
    let last = 0
    this.regex.lastIndex = 0
    for (let m = this.regex.exec(text); m; m = this.regex.exec(text)) {
      const groupIndex = m.slice(2).findIndex(g => g !== undefined)
      const candidates = this.byText.get(this.order[groupIndex]) ?? []
      const node = this.pick(candidates, text, m.index)
      if (!node || this.used.has(nodeKey(node))) continue
      this.used.add(nodeKey(node))
      out.push(text.slice(last, m.index))
      out.push(<a key={`${m.index}-${nodeKey(node)}`} href="#" className={`rel-link k-${node.kind}`}
        title={`${KIND_LABEL[node.kind]}: ${node.title}`}
        onClick={e => { e.preventDefault(); this.onOpen(node) }}>{m[0]}</a>)
      last = m.index + m[0].length
    }
    if (last === 0) return text
    out.push(text.slice(last))
    return out
  }

  /** Одинаковые названия (герой Манас и эпос «Манас»): в кавычках - произведение, иначе - герой. */
  private pick(candidates: RelationNode[], text: string, at: number) {
    const free = candidates.filter(c => !this.used.has(nodeKey(c)))
    if (free.length <= 1) return free[0]
    const quoted = text[at - 1] === '«' || text[at - 1] === '"'
    return free.find(c => c.kind === (quoted ? 'work' : 'hero')) ?? free[0]
  }

  /** Связи, которые в тексте не встретились. */
  rest() {
    return this.links.filter(n => !this.used.has(nodeKey(n)))
  }
}

/** Строка «Связано также: Манас, Түндүк» для связей, не упомянутых в тексте. */
export function SeeAlso({ linker, compact = false }: { linker: Linker; compact?: boolean }) {
  const rest = linker.rest()
  if (rest.length === 0) return null
  return <p className={compact ? 'see-also compact' : 'see-also'}>
    <span>{t('relations.s1', "Связано также:")}</span>{' '}
    {rest.map((n, i) => <span key={nodeKey(n)}>
      {i > 0 && ', '}
      <a href="#" className={`rel-link k-${n.kind}`} title={n.via ? tf('relations.s2', "через «{0}»", n.via) : KIND_LABEL[n.kind]}
        onClick={e => { e.preventDefault(); linker.onOpen(n) }}>{n.title}</a>
      {!compact && <small> ({KIND_LABEL[n.kind]}{n.via ? tf('relations.s3', ", через «{0}»", n.via) : ''})</small>}
    </span>)}
  </p>
}
