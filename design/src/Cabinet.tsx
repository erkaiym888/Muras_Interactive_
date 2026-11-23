import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { photos } from './data'
import './features.css'
import { canEditContent, fetchCabinet, fetchProgress, removeFavorite } from './api'
import type { Cabinet as CabinetData, Favorite, Progress, User } from './api'
import { AttemptDetail, ProgressPanel, formatDateTime } from './QuizHistory'
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
  const [progress, setProgress] = useState<Progress | null>(null)
  const [openAttempt, setOpenAttempt] = useState<number | null>(null)
  const userId = user?.id

  useEffect(() => {
    if (userId === undefined) return
    let alive = true
    setStatus('loading')
    fetchCabinet()
      .then(d => { if (alive) { setData(d); setStatus('ready') } })
      .catch(() => { if (alive) setStatus('error') })
    fetchProgress().then(p => { if (alive) setProgress(p) }).catch(() => {})
    return () => { alive = false }
  }, [userId])

  // Заголовок страницы - для программ чтения с экрана; на экране вместо него фото-обложка
  const title = <h1 className="sr-only">Личный кабинет</h1>

  if (!user) {
    return <CabinetPage><main className="catalog">{title}
      <section className="quiz-gate"><h2>Войдите в аккаунт</h2><p>Личный кабинет доступен после входа.</p>
        <div className="quiz-buttons"><button className="btn" onClick={() => go('login')}>Войти</button></div></section>
    </main></CabinetPage>
  }
  if (status === 'loading') return <CabinetPage><main className="catalog">{title}<p role="status" style={note}>Загрузка…</p></main></CabinetPage>
  if (status === 'error' || !data) return <CabinetPage><main className="catalog">{title}<p role="alert" style={note}>Не удалось загрузить кабинет.</p></main></CabinetPage>

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

  return <CabinetPage><main className="catalog">{title}
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
        {progress && progress.attemptsCount > 0 && <section className="cab-card">
          <h2>Прогресс в викторинах</h2>
          <ProgressPanel progress={progress} />
        </section>}
        <section className="cab-card">
          <h2>История викторин</h2>
          {openAttempt !== null ? <AttemptDetail attemptId={openAttempt} onBack={() => setOpenAttempt(null)} />
            : data.history.length === 0 ? <p className="cab-empty">Вы ещё не проходили викторины.</p>
            : <ul className="cab-list">{data.history.map(h => <li key={h.attemptId}>
              <span><button className="cab-link" onClick={() => setOpenAttempt(h.attemptId)}>{h.quizTitle}</button>
                <small>{h.scorePercent}% · +{h.pointsEarned} очков{h.repeatAttempt ? ' · повторно' : ''}</small></span>
              <small>{formatDateTime(h.completedAt)}</small>
            </li>)}</ul>}
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
  </main></CabinetPage>
}

/** Верх кабинета - обложка: та же фотография, что на главной («Открой наследие Кыргызстана»), без затемнения. */
function CabinetPage({ children }: { children: ReactNode }) {
  return <div className="cab-page">
    <div className="cab-cover" aria-hidden="true"
      style={{ backgroundImage: `url(${photos.hero})` }} />
    {children}
  </div>
}
