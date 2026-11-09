import { useEffect, useState } from 'react'
import { fetchFolkloreDetail, fetchFolkloreItems, fetchFolkloreTypes } from './api'
import type { FolkloreDetail as Detail, FolkloreItem, FolkloreKind, FolkloreType } from './api'

export type FolkloreRef = { kind: FolkloreKind; id: number }
type Status = 'loading' | 'ready' | 'error'

const ALL = 'Все'
const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6" /></svg>
const note = { textAlign: 'center' as const, color: '#716861' }
const noImage = 'linear-gradient(135deg,#6b2a30,#2b1214)'
const kindLabel = (kind: FolkloreKind) => (kind === 'hero' ? 'Герой' : 'Произведение')
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
  const [type, setType] = useState(ALL)
  const [status, setStatus] = useState<Status>('loading')

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
  }, [])

  const shown = type === ALL ? items : items.filter(i => i.type === type)

  return <main className="catalog">
    <div className="page-title"><p className="kicker">ФОЛЬКЛОР · 03</p><h1>Герои и <i>сказания</i></h1><p>Эпос, сказки, легенды и пословицы кыргызского народа: герои и произведения, в которых хранится память о прошлом.</p></div>
    <div className="filters">{[ALL, ...types.map(t => t.name)].map(t => <button className={type === t ? 'active' : ''} aria-pressed={type === t} onClick={() => setType(t)} key={t}>{t}</button>)}</div>
    {status === 'loading' && <p role="status" style={note}>Загрузка…</p>}
    {status === 'error' && <p role="alert" style={note}>Не удалось загрузить раздел. Проверьте, что сервер запущен.</p>}
    {status === 'ready' && shown.length === 0 && <p style={note}>В этой категории пока нет материалов.</p>}
    <div className="card-grid">{shown.map(item => <article className="trad-card" key={`${item.kind}-${item.id}`}>
      <div className="image">
        {item.imageUrl ? <img src={item.imageUrl} alt={item.title} /> : <Placeholder title={item.title} />}
        <span>{kindLabel(item.kind)}</span>
      </div>
      <p>{item.type}</p><h2>{item.title}</h2>
      <div><span>{shorten(item.summary)}</span><button onClick={() => openItem({ kind: item.kind, id: item.id })} aria-label={`Подробнее: ${item.title}`}><Arrow /></button></div>
    </article>)}</div>
  </main>
}

/** Карточка героя или произведения (FR-3.2): имя, тип, описание, связанные карточки, иллюстрации. */
export function FolkloreDetail({ go, item, openItem }: {
  go: (p: 'folklore') => void
  item: FolkloreRef | null
  openItem: (ref: FolkloreRef) => void
}) {
  const [detail, setDetail] = useState<Detail | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const kind = item?.kind
  const id = item?.id

  useEffect(() => {
    if (kind === undefined || id === undefined) { setStatus('error'); return }
    let alive = true
    setStatus('loading')
    window.scrollTo({ top: 0 })
    fetchFolkloreDetail(kind, id)
      .then(d => { if (alive) { setDetail(d); setStatus('ready') } })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [kind, id])

  const back = <button className="back" onClick={() => go('folklore')}>← Назад в каталог</button>

  if (status !== 'ready' || !detail) {
    return <main className="trad-detail">{back}
      {status === 'error' ? <p role="alert" style={note}>Не удалось загрузить карточку.</p> : <p role="status" style={note}>Загрузка…</p>}
    </main>
  }

  const paragraphs = (detail.description ?? '').split(/\n\s*\n/).filter(Boolean)
  const cover = detail.images[0]?.url

  return <main className="trad-detail">{back}
    <div className="trad-hero" style={{ backgroundImage: cover ? `linear-gradient(0deg,rgba(30,18,15,.7),transparent),url(${cover})` : noImage }}>
      <div><p>{detail.type.toUpperCase()}</p><h1>{detail.title}</h1><span>{kindLabel(detail.kind)}</span></div>
    </div>
    <div className="reading">
      <aside><p>Тип<br /><b>{detail.type}</b></p><p>Раздел<br /><b>{kindLabel(detail.kind)}</b></p></aside>
      <article>
        {paragraphs.map((p, i) => <p className={i === 0 ? 'big' : undefined} key={i}>{p}</p>)}
        {detail.related.length > 0 && <>
          <h2>{detail.kind === 'hero' ? 'Связанные произведения' : 'Герои произведения'}</h2>
          <div className="chips" style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            {detail.related.map(r => <button key={`${r.kind}-${r.id}`} onClick={() => openItem({ kind: r.kind, id: r.id })}>{r.title}</button>)}
          </div>
        </>}
        {detail.images.length > 0 && <>
          <h2>Иллюстрации</h2>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            {detail.images.map(img => <figure key={img.url} style={{ margin: 0, flex: '1 1 240px' }}>
              <img src={img.url} alt={img.caption ?? detail.title} style={{ height: 'auto' }} />
              {img.caption && <figcaption style={{ fontSize: 13, color: '#716861' }}>{img.caption}</figcaption>}
            </figure>)}
          </div>
        </>}
      </article>
    </div>
  </main>
}
