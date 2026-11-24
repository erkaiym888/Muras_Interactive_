import { locale, lrec, t, tf, ts } from './i18n'
import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { photos } from './data'
import './features.css'
import { canEditContent, fetchAwards, fetchCabinet, fetchProgress, removeFavorite } from './api'
import type { Awards, Cabinet as CabinetData, Favorite, Progress, User } from './api'
import { AwardsPanel, LevelAvatar, LevelCard } from './Rewards'
import { AttemptDetail, ProgressPanel, formatDateTime } from './QuizHistory'
import type { FolkloreRef } from './Folklore'
import { ProfileEdit } from './ProfileEdit'

type Status = 'loading' | 'ready' | 'error'

const ROLE: Record<string, string> = lrec('cabinet.role', { ADMIN: 'Администратор', EDITOR: 'Редактор контента', USER: 'Посетитель' })
const note = { textAlign: 'center' as const, color: '#716861' }
const formatDate = (iso: string) => new Date(iso).toLocaleDateString(locale(), { day: 'numeric', month: 'long', year: 'numeric' })

/** Личный кабинет (FR-6.2): профиль, очки, награды, история викторин и избранное. Доступен любой роли. */
export function Cabinet({ user, go, openRite, openItem, onUser }: {
  user: User | null
  /** Профиль изменён - обновить пользователя во всём приложении (меню, кабинет) */
  onUser: (u: User) => void
  go: (p: 'login' | 'editor') => void
  openRite: (id: number) => void
  openItem: (ref: FolkloreRef) => void
}) {
  const [data, setData] = useState<CabinetData | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const [editing, setEditing] = useState(false)
  const [progress, setProgress] = useState<Progress | null>(null)
  const [awards, setAwards] = useState<Awards | null>(null)
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
    fetchAwards().then(a => { if (alive) setAwards(a) }).catch(() => {})
    return () => { alive = false }
  }, [userId])

  // Заголовок страницы - для программ чтения с экрана; на экране вместо него фото-обложка
  const title = <h1 className="sr-only">{t('cabinet.s1', "Личный кабинет")}</h1>

  if (!user) {
    return <CabinetPage><main className="catalog">{title}
      <section className="quiz-gate"><h2>{t('cabinet.s2', "Войдите в аккаунт")}</h2><p>{t('cabinet.s3', "Личный кабинет доступен после входа.")}</p>
        <div className="quiz-buttons"><button className="btn" onClick={() => go('login')}>{t('cabinet.s4', "Войти")}</button></div></section>
    </main></CabinetPage>
  }
  if (status === 'loading') return <CabinetPage><main className="catalog">{title}<p role="status" style={note}>{t('cabinet.s5', "Загрузка…")}</p></main></CabinetPage>
  if (status === 'error' || !data) return <CabinetPage><main className="catalog">{title}<p role="alert" style={note}>{t('cabinet.s6', "Не удалось загрузить кабинет.")}</p></main></CabinetPage>

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
  // Профиль показываем из актуального пользователя: после редактирования он меняется сразу
  const profile = { ...data.profile, displayName: user.displayName, email: user.email, avatarUrl: user.avatarUrl ?? null }

  return <CabinetPage><main className="catalog">{title}
    <div className="cab-grid">
      <aside className="cab-card cab-profile">
        <LevelAvatar name={profile.displayName} level={awards?.level.number ?? 1} avatarUrl={profile.avatarUrl} />
        <h2>{profile.displayName}</h2>
        <p>{profile.email}</p>
        <span className="cab-role">{ROLE[profile.role] ?? profile.role}</span>
        <div className="cab-points"><b>{profile.totalPoints}</b><span>{t('cabinet.s7', "очков")}</span></div>
        <p className="cab-since">{t('cabinet.s8', "С нами с")}{' '}{formatDate(profile.registeredAt)}</p>
        {awards && <LevelCard awards={awards} />}
        <button className="btn ghost cab-edit" onClick={() => setEditing(true)}>{t('cabinet.s9', "Редактировать профиль")}</button>
      </aside>
      <div>
        {canEditContent(user) && <section className="cab-card">
          <h2>{t('cabinet.s10', "Управление содержимым")}</h2>
          <p className="cab-note">{t('cabinet.s11', "У вашей роли есть доступ к редактору контента.")}</p>
          <button className="btn" onClick={() => go('editor')}>{t('cabinet.s12', "Открыть редактор")}</button>
        </section>}
        <section className="cab-card">
          <h2>{t('cabinet.s13', "Награды")}</h2>
          {awards ? <AwardsPanel awards={awards} />
            : data.awards.length === 0 ? <p className="cab-empty">{t('cabinet.s14', "Наград пока нет. Они появятся за прохождение викторин и изучение материалов.")}</p>
            : <ul className="cab-list">{data.awards.map(a => <li key={a.title}><span><b>{ts(a.title)}</b><small>{ts(a.description)}</small></span><small>{formatDate(a.awardedAt)}</small></li>)}</ul>}
        </section>
        {progress && progress.attemptsCount > 0 && <section className="cab-card">
          <h2>{t('cabinet.s15', "Прогресс в викторинах")}</h2>
          <ProgressPanel progress={progress} />
        </section>}
        <section className="cab-card">
          <h2>{t('cabinet.s16', "История викторин")}</h2>
          {openAttempt !== null ? <AttemptDetail attemptId={openAttempt} onBack={() => setOpenAttempt(null)} />
            : data.history.length === 0 ? <p className="cab-empty">{t('cabinet.s17', "Вы ещё не проходили викторины.")}</p>
            : <ul className="cab-list">{data.history.map(h => <li key={h.attemptId}>
              <span><button className="cab-link" onClick={() => setOpenAttempt(h.attemptId)}>{h.quizTitle}</button>
                <small>{h.scorePercent}% · +{h.pointsEarned}{' '}{t('cabinet.s7', "очков")}{h.repeatAttempt ? t('cabinet.s18', " · повторно") : ''}</small></span>
              <small>{formatDateTime(h.completedAt)}</small>
            </li>)}</ul>}
        </section>
        <section className="cab-card">
          <h2>{t('cabinet.s19', "Избранное")}</h2>
          {data.favorites.length === 0 ? <p className="cab-empty">{t('cabinet.s20', "В избранном пока пусто. Нажмите «В избранное» на странице обряда, героя или произведения.")}</p>
            : <ul className="cab-list">{data.favorites.map(f => <li key={`${f.itemType}-${f.itemId}`}>
              <span><button className="cab-link" onClick={() => open(f)}>{f.title}</button><small>{f.subtitle}</small></span>
              <button className="cab-x" onClick={() => remove(f)} aria-label={tf('cabinet.s21', "Убрать из избранного: {0}", f.title)}>×</button>
            </li>)}</ul>}
        </section>
      </div>
    </div>
    {editing && <ProfileEdit user={user} onUser={onUser} onClose={() => setEditing(false)} />}
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
