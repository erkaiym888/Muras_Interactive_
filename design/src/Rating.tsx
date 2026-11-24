import { locale, t, ts } from './i18n'
import { useEffect, useState } from 'react'
import { fetchRating } from './api'
import type { Rating as RatingData, RatingPeriod, RatingRow, User } from './api'
import { pointsWord } from './QuizResult'
import { LevelAvatar } from './Rewards'

/**
 * Рейтинг (FR-5.5, Приложение Б.4): общий - по очкам за всё время, недельный - за текущую неделю
 * (с понедельника по воскресенье по Бишкеку). Видны только отображаемое имя, уровень и очки.
 */

type Status = 'loading' | 'ready' | 'error'
const note = { textAlign: 'center' as const, color: '#716861' }

const day = (iso: string) => new Date(`${iso}T00:00:00+06:00`).toLocaleDateString(locale(), { day: 'numeric', month: 'long', timeZone: 'Asia/Bishkek' })

function Row({ r }: { r: RatingRow }) {
  return <li className={r.me ? 'me' : ''}>
    <span className="rk-place">{r.place}</span>
    <LevelAvatar name={r.displayName} level={r.level} size="sm" />
    <span className="rk-name"><b>{r.displayName}{r.me && <em>{' '}{t('rating.s1', "· вы")}</em>}</b><small>{t('rating.s2', "Уровень")}{' '}{r.level} · {ts(r.levelTitle)}</small></span>
    <span className="rk-points">{r.points} <small>{pointsWord(r.points)}</small></span>
  </li>
}

export function Rating({ user, goLogin }: { user: User | null; goLogin: () => void }) {
  const [period, setPeriod] = useState<RatingPeriod>('all')
  const [data, setData] = useState<RatingData | null>(null)
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    let alive = true
    setStatus('loading')
    fetchRating(period).then(d => { if (alive) { setData(d); setStatus('ready') } }).catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [period, user?.id])

  const top = data?.rows.slice(0, 3) ?? []
  const podium = [top[1], top[0], top[2]].filter(Boolean) as RatingRow[]

  return <main className="catalog rating-page">
    <div className="page-title"><p className="kicker">{t('rating.s3', "ГЕЙМИФИКАЦИЯ · РЕЙТИНГ")}</p><h1>{t('rating.s4', "Рейтинг")}{' '}<i>{t('rating.s5', "знатоков")}</i></h1>
      <p>{t('rating.s6', "Очки начисляются за викторины, изучение материалов, ежедневные визиты и значки.")}</p></div>

    <div className="rk-tabs" role="tablist" aria-label={t('rating.s7', "Период")}>
      <button role="tab" aria-selected={period === 'all'} className={period === 'all' ? 'active' : ''} onClick={() => setPeriod('all')}>{t('rating.s8', "За всё время")}</button>
      <button role="tab" aria-selected={period === 'week'} className={period === 'week' ? 'active' : ''} onClick={() => setPeriod('week')}>{t('rating.s9', "Эта неделя")}</button>
    </div>
    {period === 'week' && data?.weekStart && data.weekEnd && <p className="rk-week">{t('rating.s10', "Неделя с")}{' '}{day(data.weekStart)}{' '}{t('rating.s11', "по")}{' '}{day(data.weekEnd)}{' '}{t('rating.s12', "· лидер недели получает значок «Лидер недели» (+100)")}</p>}

    {status === 'loading' && <p role="status" style={note}>{t('rating.s13', "Загрузка…")}</p>}
    {status === 'error' && <p role="alert" style={note}>{t('rating.s14', "Не удалось загрузить рейтинг.")}</p>}
    {status === 'ready' && data && data.rows.length === 0 && <p style={note}>{t('rating.s15', "Пока никого нет.")}</p>}

    {status === 'ready' && data && data.rows.length > 0 && <>
      <ol className="rk-podium" aria-label={t('rating.s16', "Тройка лидеров")}>{podium.map(r => <li key={`${r.place}-${r.displayName}`} className={`p${r.place}${r.me ? ' me' : ''}`}>
        <LevelAvatar name={r.displayName} level={r.level} />
        <b>{r.displayName}</b>
        <small>{ts(r.levelTitle)}</small>
        <span className="rk-pedestal"><em>{r.place}</em>{r.points} {pointsWord(r.points)}</span>
      </li>)}</ol>

      <ol className="rk-list" aria-label={t('rating.s4', "Рейтинг")}>{data.rows.map((r, i) => <Row key={i} r={r} />)}</ol>
      {data.me && <><p className="rk-gap">…</p><ol className="rk-list"><Row r={data.me} /></ol></>}
      <p className="rk-total">{t('rating.s17', "Участников:")}{' '}{data.participants}</p>
    </>}
    {!user && <p className="rk-join">{t('rating.s18', "Хотите попасть в рейтинг?")}{' '}<button className="text-link" onClick={goLogin}>{t('rating.s19', "Войдите или зарегистрируйтесь")}</button></p>}
  </main>
}
