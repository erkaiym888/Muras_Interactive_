import { useEffect, useState } from 'react'
import { postView } from './api'
import type { Achievement, Awards, Level, RewardEvent, User, ViewType } from './api'

/**
 * Награды (FR-5.3): значки, рамки уровней, всплывающие сообщения о новых очках и значках.
 */

// ---------- Шина событий: любое начисление показывается всплывающим сообщением ----------

type Listener = (e: RewardEvent) => void
const listeners = new Set<Listener>()

export function emitReward(e: RewardEvent | null | undefined) {
  if (!e || (e.pointsEarned <= 0 && e.newAchievements.length === 0 && !e.levelUp)) return
  listeners.forEach(l => l(e))
}

/** Отметить открытие материала: очки за первое открытие и значки исследования. Гостям ничего. */
export function trackView(user: User | null, type: ViewType, key: string | number) {
  if (!user) return
  postView(type, key).then(emitReward).catch(() => { /* не мешаем чтению из-за ошибки начисления */ })
}

type Toast = { id: number; kind: 'points' | 'badge' | 'level'; title: string; text: string; achievement?: Achievement }
let nextId = 1

export function RewardToasts() {
  const [toasts, setToasts] = useState<Toast[]>([])
  useEffect(() => {
    const onReward: Listener = e => {
      const items: Toast[] = []
      const plain = e.points.filter(p => p.code !== 'ACHIEVEMENT' && p.points > 0)
      if (plain.length) {
        // Очки подряд (например, открытие нескольких элементов юрты) копятся в одном сообщении
        const sum = plain.reduce((s, p) => s + p.points, 0)
        const label = [...new Set(plain.map(p => p.label))].join(' · ')
        const id = nextId++
        setToasts(t => {
          const prev = t.find(x => x.kind === 'points')
          const total = sum + (prev ? Number(prev.title.slice(1)) : 0)
          const text = prev && !prev.text.includes(label) ? `${prev.text} · ${label}` : label
          return [...t.filter(x => x.kind !== 'points'), { id, kind: 'points', title: `+${total}`, text }]
        })
        setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3500)
      }
      e.newAchievements.forEach(a => items.push({ id: nextId++, kind: 'badge', title: `Новый значок «${a.title}»`, text: `${a.condition} · +${a.bonus}`, achievement: a }))
      if (e.levelUp) items.push({ id: nextId++, kind: 'level', title: `Уровень ${e.levelUp.number}: ${e.levelUp.title}`, text: e.levelUp.reward ?? 'Новый уровень!' })
      setToasts(t => [...t, ...items])
      items.forEach(it => setTimeout(() => setToasts(t => t.filter(x => x.id !== it.id)), it.kind === 'points' ? 3500 : 6500))
    }
    listeners.add(onReward)
    return () => { listeners.delete(onReward) }
  }, [])
  return <div className="reward-toasts" role="status" aria-live="polite">
    {toasts.map(t => <div key={t.id} className={`reward-toast rt-${t.kind}`}>
      {t.kind === 'badge' && t.achievement ? <BadgeIcon code={t.achievement.code} earned /> : t.kind === 'level' ? <span className="rt-up">▲</span> : <span className="rt-pts">{t.title}</span>}
      <div><b>{t.kind === 'points' ? 'Очки начислены' : t.title}</b><small>{t.text}</small></div>
      <button onClick={() => setToasts(x => x.filter(y => y.id !== t.id))} aria-label="Закрыть">×</button>
    </div>)}
  </div>
}

// ---------- Иконки значков ----------

const ICONS: Record<string, string> = {
  FIRST_STEP: 'M9 6.5c1.4 0 2 1.6 2 3.3S10.2 13 9 13 7 11.6 7 9.8 7.6 6.5 9 6.5zM15 11c1.4 0 2 1.6 2 3.3S16.2 17.5 15 17.5s-2-1.4-2-3.2.6-3.3 2-3.3z',
  YURT_MASTER: 'M4 17h16M5.5 17v-5.5L12 7l6.5 4.5V17M10 17v-3.5h4V17M9 9.5 12 7l3 2.5',
  RITES_EXPERT: 'M12 5v14M5 12h14M7 7l10 10M17 7 7 17M12 9a3 3 0 1 1 0 6 3 3 0 0 1 0-6z',
  STORYTELLER: 'M4 6.5c2.7-.8 5.3-.6 8 1v11c-2.7-1.6-5.3-1.8-8-1zM20 6.5c-2.7-.8-5.3-.6-8 1v11c2.7-1.6 5.3-1.8 8-1z',
  PATHFINDER: 'M10.5 5.5a5 5 0 1 1 0 10 5 5 0 0 1 0-10zM14.2 14.2 19 19',
  QUEST_START: 'M6 20V4.5M6 5h11l-2.5 3.5L17 12H6',
  FLAWLESS: 'M12 4.5l2.3 4.7 5.2.8-3.8 3.6.9 5.2L12 16.4l-4.6 2.4.9-5.2-3.8-3.6 5.2-.8z',
  SHARP_EYE: 'M12 4.5a7.5 7.5 0 1 1 0 15 7.5 7.5 0 0 1 0-15zM12 8.5a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7zM12 11.5v1',
  ERUDITE: 'M3.5 9.5 12 5.5l8.5 4-8.5 4zM7 11.3v4.2c3 2 7 2 10 0v-4.2M20.5 9.5v5',
  THREE_TOPICS: 'M12 5a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM7 12a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM17 12a3 3 0 1 1 0 6 3 3 0 0 1 0-6z',
  NO_HINTS: 'M5.5 12.5l4 4 9-9',
  HEARTH_FIRE: 'M12 4c3 3.5 5 6 5 9a5 5 0 0 1-10 0c0-1.6.7-3 1.8-4.2.2 1.7 1 2.7 2.2 3.2C10.6 9.5 11 6.8 12 4z',
  HEARTH_WEEK: 'M8.5 6c2.2 2.6 3.5 4.4 3.5 6.6a3.5 3.5 0 0 1-7 0C5 10.8 6.4 8.8 8.5 6zM15.5 9c2 2.3 3.5 4 3.5 6a3.5 3.5 0 0 1-7 0c0-1.9 1.4-3.6 3.5-6z',
  TRADITION_KEEPER: 'M12 19v-6M12 13c-1.5-3-5-3.5-6.5-1.5S6 16 8 15.5s1.5-3-.5-3M12 13c1.5-3 5-3.5 6.5-1.5S18 16 16 15.5s-1.5-3 .5-3M12 13V5',
  WEEK_LEADER: 'M4.5 17.5h15M5 15.5l-1-8 4.5 3.5L12 5l3.5 6L20 7.5l-1 8z',
}

export function BadgeIcon({ code, earned }: { code: string; earned: boolean }) {
  return <span className={earned ? 'badge-ico earned' : 'badge-ico'} aria-hidden="true">
    <svg viewBox="0 0 24 24"><path d={ICONS[code] ?? ICONS.FLAWLESS} /></svg>
  </span>
}

// ---------- Аватар с рамкой уровня ----------

/** Аватар с рамкой уровня: фото пользователя (FR-6.2) или первая буква имени. */
export function LevelAvatar({ name, level, size = 'lg', avatarUrl }: { name: string; level: number; size?: 'lg' | 'sm'; avatarUrl?: string | null }) {
  return <span className={`level-avatar l${level} ${size}`} aria-hidden="true">
    {avatarUrl ? <img className="level-photo" src={avatarUrl} alt="" /> : <span>{name.trim().charAt(0).toUpperCase()}</span>}
  </span>
}

// ---------- Уровень и значки в личном кабинете ----------

export function LevelCard({ awards }: { awards: Awards }) {
  const { level, nextLevel } = awards
  return <div className="level-card">
    <p className="kicker">Уровень {level.number} из {awards.levels.length}</p>
    <h3>{level.title}</h3>
    {level.reward && <p className="lc-reward">{level.reward}</p>}
    <div className="lc-bar" role="progressbar" aria-label="Прогресс до следующего уровня" aria-valuemin={0} aria-valuemax={100} aria-valuenow={awards.progressPercent}><i style={{ width: `${awards.progressPercent}%` }} /></div>
    <p className="lc-next">{nextLevel ? <>Ещё <b>{awards.pointsToNext}</b> до уровня «{nextLevel.title}»</> : 'Достигнут высший уровень'}</p>
    {awards.loginStreak > 1 && <p className="lc-streak">Серия входов: <b>{awards.loginStreak}</b> {awards.loginStreak < 5 ? 'дня' : 'дней'} подряд</p>}
  </div>
}

export function AwardsPanel({ awards }: { awards: Awards }) {
  const groups = [...new Set(awards.achievements.map(a => a.group))]
  const earned = awards.achievements.filter(a => a.earned).length
  return <div className="awards-panel">
    <p className="ap-count"><b>{earned}</b> из {awards.achievements.length} значков</p>
    {groups.map(g => <div className="ap-group" key={g}>
      <h3>{g}</h3>
      <ul className="badge-grid">{awards.achievements.filter(a => a.group === g).map(a => <li key={a.code} className={a.earned ? 'earned' : 'locked'}>
        <BadgeIcon code={a.code} earned={a.earned} />
        <b>{a.title}</b>
        <small>{a.condition}</small>
        <em>{a.earned && a.awardedAt ? new Date(a.awardedAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' }) : `+${a.bonus}`}</em>
      </li>)}</ul>
    </div>)}
    <details className="levels-ladder">
      <summary>Все уровни</summary>
      <ol>{awards.levels.map(l => <li key={l.number} className={l.number === awards.level.number ? 'current' : l.number < awards.level.number ? 'done' : ''}>
        <LevelAvatar name={String(l.number)} level={l.number} size="sm" />
        <span><b>{l.title}</b><small>{l.minPoints} очков · {l.reward}</small></span>
      </li>)}</ol>
    </details>
  </div>
}

/** Блок «Новые награды» на экране результата викторины. */
export function NewRewards({ achievements, levelUp }: { achievements: Achievement[]; levelUp: Level | null | undefined }) {
  if (!achievements.length && !levelUp) return null
  return <div className="new-rewards">
    <h3>Новые награды</h3>
    {levelUp && <p className="nr-level"><LevelAvatar name={String(levelUp.number)} level={levelUp.number} size="sm" /><span><b>Новый уровень: {levelUp.title}</b><small>{levelUp.reward}</small></span></p>}
    <ul>{achievements.map(a => <li key={a.code}><BadgeIcon code={a.code} earned /><span><b>«{a.title}»</b><small>{a.condition}</small></span><em>+{a.bonus}</em></li>)}</ul>
  </div>
}
