import { useEffect, useRef, useState } from 'react'
import { t, tf, useI18n } from './i18n'
import { FavoriteButton } from './Favorite'
import type { User } from './api'
import { trackView } from './Rewards'
import { fetchFolkloreDetail, fetchFolkloreItems, fetchFolkloreTypes } from './api'
import type { FolkloreDetail as Detail, FolkloreItem, FolkloreKind, FolkloreType } from './api'
import { Linker, SeeAlso, useRelationLinks } from './Relations'
import type { OpenNode } from './Relations'

export type FolkloreRef = { kind: FolkloreKind; id: number }
type Status = 'loading' | 'ready' | 'error'

const ALL = 'Все'
const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6" /></svg>
const note = { textAlign: 'center' as const, color: '#716861' }
const noImage = 'linear-gradient(135deg,#6b2a30,#2b1214)'
const kindLabel = (kind: FolkloreKind) => (kind === 'hero' ? t('folklore.s1', "Герой") : t('folklore.s2', "Произведение"))
const shorten = (text: string | null, max = 150) =>
  !text ? '' : text.length > max ? `${text.slice(0, max).trimEnd()}…` : text

/** Заглушка, пока у карточки нет иллюстрации: первая буква названия. */
function Placeholder({ title }: { title: string }) {
  return <div role="img" aria-label={title} style={{ height: 'clamp(220px,40vw,360px)', display: 'grid', placeItems: 'center', background: noImage, color: '#e4c98f', fontSize: 96 }}>{title.charAt(0)}</div>
}

/** Каталог героев и произведений с фильтром по типам из справочника (FR-3.1). */
export function Folklore({ openItem }: { openItem: (ref: FolkloreRef) => void }) {
  const [items, setItems] = useState<FolkloreItem[]>([])
  const [types, setTypes] = useState<FolkloreType[]>([])
  // Фильтр хранится по id типа: при смене языка название типа меняется, а выбор остаётся (FR-8.2)
  const [typeId, setTypeId] = useState<number | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const { lang } = useI18n()

  useEffect(() => {
    let alive = true
    Promise.all([fetchFolkloreItems(), fetchFolkloreTypes()])
      .then(([i, t]) => {
        if (!alive) return
        setItems(i)
        setTypes(t)
        setStatus('ready')
      })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [lang])

  const typeName = types.find(x => x.id === typeId)?.name
  const shown = typeName === undefined ? items : items.filter(i => i.type === typeName)

  return <main className="catalog">
    <div className="page-title"><p className="kicker">{t('folklore.kicker', 'ФОЛЬКЛОР · 03')}</p><h1>{t('folklore.title', 'Герои и')} <i>{t('folklore.titleAccent', 'сказания')}</i></h1><p>{t('folklore.lead', 'Эпос, сказки, легенды и пословицы кыргызского народа: герои и произведения, в которых хранится память о прошлом.')}</p></div>
    <div className="filters">
      <button className={typeId === null ? 'active' : ''} aria-pressed={typeId === null} onClick={() => setTypeId(null)}>{t('folklore.all', ALL)}</button>
      {types.map(x => <button className={typeId === x.id ? 'active' : ''} aria-pressed={typeId === x.id} onClick={() => setTypeId(x.id)} key={x.id}>{x.name}</button>)}
    </div>
    {status === 'loading' && <p role="status" style={note}>{t('folklore.s3', "Загрузка…")}</p>}
    {status === 'error' && <p role="alert" style={note}>{t('folklore.s4', "Не удалось загрузить раздел. Проверьте, что сервер запущен.")}</p>}
    {status === 'ready' && shown.length === 0 && <p style={note}>{t('folklore.s5', "В этой категории пока нет материалов.")}</p>}
    <div className="card-grid">{shown.map(item => <article className="trad-card" key={`${item.kind}-${item.id}`}>
      <div className="image">
        {item.imageUrl ? <img src={item.imageUrl} alt={item.title} /> : <Placeholder title={item.title} />}
        <span>{kindLabel(item.kind)}</span>
      </div>
      <p>{item.type}</p><h2>{item.title}</h2>
      <div><span>{shorten(item.summary)}</span><button onClick={() => openItem({ kind: item.kind, id: item.id })} aria-label={tf('folklore.s6', "Подробнее: {0}", item.title)}><Arrow /></button></div>
    </article>)}</div>
  </main>
}

/** Карточка героя или произведения (FR-3.2): имя, тип, описание, связанные карточки, иллюстрации. */
export function FolkloreDetail({ go, item, openNode, user }: {
  go: (p: 'folklore') => void
  item: FolkloreRef | null
  openNode: OpenNode
  user: User | null
}) {
  const [detail, setDetail] = useState<Detail | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const kind = item?.kind
  const id = item?.id
  const { lang } = useI18n()
  const loaded = useRef('')
  const links = useRelationLinks(kind ?? 'hero', id ?? null)

  useEffect(() => {
    if (kind === undefined || id === undefined) { setStatus('error'); return }
    let alive = true
    // FR-8.2: при смене языка остаёмся на том же месте страницы, наверх - только при переходе к другой карточке
    if (loaded.current !== `${kind}-${id}`) { setStatus('loading'); window.scrollTo({ top: 0 }) }
    loaded.current = `${kind}-${id}`
    fetchFolkloreDetail(kind, id)
      .then(d => { if (alive) { setDetail(d); setStatus('ready'); trackView(user, kind === 'hero' ? 'HERO' : 'WORK', id) } })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [kind, id, lang])

  const back = <button className="back" onClick={() => go('folklore')}>← {t('common.backToCatalog', 'Назад в каталог')}</button>

  if (status !== 'ready' || !detail) {
    return <main className="trad-detail">{back}
      {status === 'error' ? <p role="alert" style={note}>{t('folklore.s7', "Не удалось загрузить карточку.")}</p> : <p role="status" style={note}>{t('folklore.s3', "Загрузка…")}</p>}
    </main>
  }

  const paragraphs = (detail.description ?? '').split(/\n\s*\n/).filter(Boolean)
  // Связи (FR-3.2, FR-3.3): упомянутые в тексте произведения, герои, обряды - ссылками
  const linker = new Linker(links ?? [], openNode)
  const cover = detail.images[0]?.url

  return <main className="trad-detail">{back}
    <FavoriteButton user={user} itemType={detail.kind === 'hero' ? 'HERO' : 'WORK'} itemId={detail.id} />
    <div className="trad-hero" style={{ backgroundImage: cover ? `linear-gradient(0deg,rgba(30,18,15,.7),transparent),url(${cover})` : noImage }}>
      <div><p>{detail.type.toUpperCase()}</p><h1>{detail.title}</h1><span>{kindLabel(detail.kind)}</span></div>
    </div>
    <div className="reading">
      <aside><p>{t('folklore.s8', "Тип")}<br /><b>{detail.type}</b></p><p>{t('folklore.s9', "Раздел")}<br /><b>{kindLabel(detail.kind)}</b></p></aside>
      <article>
        {paragraphs.map((p, i) => <p className={i === 0 ? 'big' : undefined} key={i}>{linker.text(p)}</p>)}
        {detail.images.length > 0 && <>
          <h2>{t('folklore.s10', "Иллюстрации")}</h2>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            {detail.images.map(img => <figure key={img.url} style={{ margin: 0, flex: '1 1 240px' }}>
              <img src={img.url} alt={img.caption ?? detail.title} style={{ height: 'auto' }} />
              {img.caption && <figcaption style={{ fontSize: 13, color: '#716861' }}>{img.caption}</figcaption>}
            </figure>)}
          </div>
        </>}
        <SeeAlso linker={linker} />
      </article>
    </div>
  </main>
}
