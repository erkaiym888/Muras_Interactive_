import { useEffect, useState } from 'react'
import './features.css'
import { canEditContent, fetchCabinet, removeFavorite } from './api'
import type { Cabinet as CabinetData, Favorite, User } from './api'
import type { FolkloreRef } from './Folklore'

type Status = 'loading' | 'ready' | 'error'

const ROLE: Record<string, string> = { ADMIN: 'Администратор', EDITOR: 'Редактор контента', USER: 'Посетитель' }
const note = { textAlign: 'center' as const, color: '#716861' }
const formatDate = (iso: string) => new Date(iso).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })

/** Личный кабинет (FR-6.2): профиль, очки, награды, история викторин и избранное. Доступен любой роли. */
export function Cabinet({ user, go, openRite, openItem }: {
  user: User | null
  go: (p: 'login' | 'editor') => void
  openRite: (id: number) => void
  openItem: (ref: FolkloreRef) => void
}) {
  const [data, setData] = useState<CabinetData | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const userId = user?.id

  useEffect(() => {
    if (userId === undefined) return
    let alive = true
    setStatus('loading')
    fetchCabinet()
      .then(d => { if (alive) { setData(d); setStatus('ready') } })
      .catch(() => { if (alive) setStatus('error') })
    return () => { alive = false }
  }, [userId])

  const title = <div className="page-title"><p className="kicker">ЛИЧНЫЙ КАБИНЕТ</p><h1>Ваш <i>профиль</i></h1></div>

  if (!user) {
    return <main className="catalog">{title}
      <section className="quiz-gate"><h2>Войдите в аккаунт</h2><p>Личный кабинет доступен после входа.</p>
        <div className="quiz-buttons"><button className="btn" onClick={() => go('login')}>Войти</button></div></section>
    </main>
  }
  if (status === 'loading') return <main className="catalog">{title}<p role="status" style={note}>Загрузка…</p></main>
  if (status === 'error' || !data) return <main className="catalog">{title}<p role="alert" style={note}>Не удалось загрузить кабинет.</p></main>

  const open = (f: Favorite) => {
    if (f.itemType === 'RITE') openRite(f.itemId)
    else openItem({ kind: f.itemType === 'HERO' ? 'hero' : 'work', id: f.itemId })
  }
  const remove = async (f: Favorite) => {
    try {
      await removeFavorite(f.itemType, f.itemId)
      setData({ ...data, favorites: data.favorites.filter(x => !(x.itemType === f.itemType && x.itemId === f.itemId)) })
    } catch { /* список останется прежним */ }
  }
  const { profile } = data

  return <main className="catalog">{title}
    <div className="cab-grid">
      <aside className="cab-card cab-profile">
        <div className="avatar-lg" aria-hidden="true">{profile.displayName.trim().charAt(0).toUpperCase()}</div>
        <h2>{profile.displayName}</h2>
        <p>{profile.email}</p>
        <span className="cab-role">{ROLE[profile.role] ?? profile.role}</span>
        <div className="cab-points"><b>{profile.totalPoints}</b><span>очков</span></div>
        <p className="cab-since">С нами с {formatDate(profile.registeredAt)}</p>
      </aside>
      <div>
        {canEditContent(user) && <section className="cab-card">
          <h2>Управление содержимым</h2>
          <p className="cab-note">У вашей роли есть доступ к редактору контента.</p>
          <button className="btn" onClick={() => go('editor')}>Открыть редактор</button>
        </section>}
        <section className="cab-card">
          <h2>Награды</h2>
          {data.awards.length === 0 ? <p className="cab-empty">Наград пока нет. Они появятся за прохождение викторин и изучение материалов.</p>
            : <ul className="cab-list">{data.awards.map(a => <li key={a.title}><span><b>{a.title}</b><small>{a.description}</small></span><small>{formatDate(a.awardedAt)}</small></li>)}</ul>}
        </section>
        <section className="cab-card">
          <h2>История викторин</h2>
          {data.history.length === 0 ? <p className="cab-empty">Вы ещё не проходили викторины.</p>
            : <ul className="cab-list">{data.history.map((h, i) => <li key={i}><span><b>{h.quizTitle}</b><small>{h.scorePercent}% · +{h.pointsEarned} очков</small></span><small>{formatDate(h.completedAt)}</small></li>)}</ul>}
        </section>
        <section className="cab-card">
          <h2>Избранное</h2>
          {data.favorites.length === 0 ? <p className="cab-empty">В избранном пока пусто. Нажмите «В избранное» на странице обряда, героя или произведения.</p>
            : <ul className="cab-list">{data.favorites.map(f => <li key={`${f.itemType}-${f.itemId}`}>
              <span><button className="cab-link" onClick={() => open(f)}>{f.title}</button><small>{f.subtitle}</small></span>
              <button className="cab-x" onClick={() => remove(f)} aria-label={`Убрать из избранного: ${f.title}`}>×</button>
            </li>)}</ul>}
        </section>
      </div>
    </div>
  </main>
}
