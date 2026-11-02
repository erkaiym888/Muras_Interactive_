import { useEffect, useState } from 'react'
import { photos } from './data'
import { fetchCategories, fetchRite, fetchRites } from './api'
import type { RiteCategory, RiteDetail, RiteSummary } from './api'

type Status = 'loading' | 'ready' | 'error'
type Go = (page: 'traditions' | 'tradition-detail') => void

const ALL = 'Все традиции'
const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6" /></svg>
const note = { textAlign: 'center' as const, color: '#716861' }

/** Каталог обрядов: данные приходят из базы через /api/rites. */
export function Traditions({ openRite }: { openRite: (id: number) => void }) {
  const [rites, setRites] = useState<RiteSummary[]>([])
  const [categories, setCategories] = useState<RiteCategory[]>([])
  const [cat, setCat] = useState(ALL)
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    let alive = true
    Promise.all([fetchRites(), fetchCategories()])
      .then(([r, c]) => {
        if (!alive) return
        setRites(r)
        setCategories(c)
        setStatus('ready')
      })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [])

  const shown = cat === ALL ? rites : rites.filter(t => t.category === cat)

  return <main className="catalog">
    <div className="page-title"><p className="kicker">КУЛЬТУРНЫЙ АРХИВ · 02</p><h1>Традиции и <i>обряды</i></h1><p>Живые ритуалы, сопровождающие человека от рождения и объединяющие кыргызские семьи.</p></div>
    <div className="filters">{[ALL, ...categories.map(c => c.name)].map(c => <button className={cat === c ? 'active' : ''} onClick={() => setCat(c)} key={c}>{c}</button>)}</div>
    {status === 'loading' && <p role="status" style={note}>Загрузка…</p>}
    {status === 'error' && <p role="alert" style={note}>Не удалось загрузить традиции. Проверьте, что сервер запущен.</p>}
    {status === 'ready' && shown.length === 0 && <p style={note}>В этой категории пока нет материалов.</p>}
    <div className="card-grid">{shown.map((t, i) => <article className="trad-card" key={t.id}>
      <div className="image"><img src={t.imageUrl ?? photos.hero} alt={t.title} /><span>{String(i + 1).padStart(2, '0')}</span></div>
      <p>{t.category}</p><h2>{t.title}</h2>
      <div><span>{t.summary}</span><button onClick={() => openRite(t.id)} aria-label={`Подробнее: ${t.title}`}><Arrow /></button></div>
    </article>)}</div>
  </main>
}

/** Карточка обряда (FR-2.2): название, категория, описание, этапы, символика, изображения, источники. */
export function TraditionDetail({ go, riteId }: { go: Go; riteId: number | null }) {
  const [rite, setRite] = useState<RiteDetail | null>(null)
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    if (riteId === null) { setStatus('error'); return }
    let alive = true
    setStatus('loading')
    fetchRite(riteId)
      .then(r => { if (alive) { setRite(r); setStatus('ready') } })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [riteId])

  const back = <button className="back" onClick={() => go('traditions')}>← Назад в каталог</button>

  if (status !== 'ready' || !rite) {
    return <main className="trad-detail">{back}
      {status === 'error' ? <p role="alert" style={note}>Не удалось загрузить обряд.</p> : <p role="status" style={note}>Загрузка…</p>}
    </main>
  }

  const paragraphs = (rite.description ?? rite.summary).split(/\n\s*\n/).filter(Boolean)
  const image = rite.images[0]?.url ?? photos.hero

  return <main className="trad-detail">{back}
    <div className="trad-hero" style={{ backgroundImage: `linear-gradient(0deg,rgba(30,18,15,.7),transparent),url(${image})` }}>
      <div><p>{rite.category.toUpperCase()}</p><h1>{rite.title}</h1><span>{rite.summary}</span></div>
    </div>
    <div className="reading">
      <aside><p>Категория<br /><b>{rite.category}</b></p><p>Этапов<br /><b>{rite.steps.length}</b></p></aside>
      <article>
        {paragraphs.map((p, i) => <p className={i === 0 ? 'big' : undefined} key={i}>{p}</p>)}
        {rite.steps.length > 0 && <><h2>Этапы обряда</h2><ol>{rite.steps.map(s => <li key={s.order}>{s.title && <b>{s.title}</b>}<span>{s.text}</span></li>)}</ol></>}
        {rite.symbolism && <><h2>Культурный смысл</h2><p>{rite.symbolism}</p></>}
        {rite.sources && <div className="sources"><b>Источники</b><p>{rite.sources}</p></div>}
      </article>
    </div>
  </main>
}
