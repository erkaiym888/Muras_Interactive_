import { useEffect, useMemo, useState } from 'react'
import { adminLanguages, adminRoles, adminUsers, isAdmin, setUserBlocked, setUserRole, toggleLanguage } from './api'
import type { AdminLanguage, AdminRole, AdminUserRow, Role, User } from './api'
import { initLanguages } from './i18n'
import { errorText } from './EditorFields'

/**
 * Пользователи и роли (FR-7.3). Страница только для администратора; сервер проверяет роль на каждом запросе
 * и не даёт изменить себя или оставить систему без администратора.
 */

const ROLE_TITLE: Record<Role, string> = { USER: 'Пользователь', EDITOR: 'Редактор контента', ADMIN: 'Администратор' }
const ROLES: Role[] = ['USER', 'EDITOR', 'ADMIN']
type StatusFilter = 'ALL' | 'ACTIVE' | 'BLOCKED'

const dateFmt = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })

export function Admin({ user, goLogin }: { user: User | null; goLogin: () => void }) {
  const [rows, setRows] = useState<AdminUserRow[] | null>(null)
  const [roles, setRoles] = useState<AdminRole[]>([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [role, setRole] = useState<Role | 'ALL'>('ALL')
  const [status, setStatus] = useState<StatusFilter>('ALL')
  const [busy, setBusy] = useState<number | null>(null)
  const [confirm, setConfirm] = useState<number | null>(null)
  const [toast, setToast] = useState('')
  const [tab, setTab] = useState<'users' | 'languages'>('users')

  const allowed = isAdmin(user)
  const loadRoles = () => adminRoles().then(setRoles).catch(() => {})
  useEffect(() => {
    if (!allowed) return
    adminUsers().then(setRows).catch(e => setError(errorText(e, 'Не удалось загрузить пользователей.')))
    loadRoles()
  }, [allowed])
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(''), 3500)
    return () => clearTimeout(t)
  }, [toast])

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (rows ?? []).filter(r => (role === 'ALL' || r.role === role) &&
      (status === 'ALL' || (status === 'BLOCKED') === r.blocked) &&
      (!q || r.displayName.toLowerCase().includes(q) || r.email.includes(q)))
  }, [rows, search, role, status])

  if (!user) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">АДМИНИСТРИРОВАНИЕ</p><h1>Нужен вход</h1>
      <p>Раздел доступен только администратору.</p><button className="btn" onClick={goLogin}>Войти</button></div></main>
  }
  if (!allowed) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">АДМИНИСТРИРОВАНИЕ</p><h1>Нет доступа</h1>
      <p>Управлять пользователями и ролями может только администратор.</p></div></main>
  }

  const replace = (row: AdminUserRow) => setRows(rs => (rs ?? []).map(r => r.id === row.id ? row : r))

  const changeRole = async (r: AdminUserRow, next: Role) => {
    setBusy(r.id); setError('')
    try {
      replace(await setUserRole(r.id, next))
      loadRoles()
      setToast(`${r.displayName}: ${ROLE_TITLE[next].toLowerCase()}`)
    } catch (e) { setError(errorText(e, 'Не удалось сменить роль.')) } finally { setBusy(null) }
  }

  const toggleBlock = async (r: AdminUserRow) => {
    setBusy(r.id); setError(''); setConfirm(null)
    try {
      replace(await setUserBlocked(r.id, !r.blocked))
      setToast(r.blocked ? `${r.displayName} разблокирован(а)` : `${r.displayName} заблокирован(а)`)
    } catch (e) { setError(errorText(e, 'Не удалось изменить блокировку.')) } finally { setBusy(null) }
  }

  const blockedCount = (rows ?? []).filter(r => r.blocked).length

  return <main className="editor admin">
    <div className="editor-head">
      <div><p className="kicker">АДМИНИСТРИРОВАНИЕ</p><h1>Пользователи <i>и роли</i></h1></div>
      <p className="editor-who">{user.displayName}<span>Администратор</span></p>
    </div>
    <div className="editor-tabs" role="tablist">
      <button role="tab" aria-selected={tab === 'users'} className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>Пользователи</button>
      <button role="tab" aria-selected={tab === 'languages'} className={tab === 'languages' ? 'active' : ''} onClick={() => setTab('languages')}>Языки</button>
    </div>
    {tab === 'languages' ? <Languages notify={setToast} /> : <>

    <div className="role-cards">
      {(roles.length ? roles : ROLES.map(n => ({ name: n, title: ROLE_TITLE[n], description: '', userCount: 0 }))).map(r =>
        <button key={r.name} className={role === r.name ? `role-card r-${r.name.toLowerCase()} active` : `role-card r-${r.name.toLowerCase()}`}
          aria-pressed={role === r.name} onClick={() => setRole(role === r.name ? 'ALL' : r.name)}>
          <b>{r.userCount}</b><span>{r.title}</span><small>{r.description}</small>
        </button>)}
    </div>

    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Найти по имени или почте" aria-label="Поиск" />
      <select value={role} onChange={e => setRole(e.target.value as Role | 'ALL')} aria-label="Роль">
        <option value="ALL">Все роли · {rows?.length ?? 0}</option>
        {ROLES.map(r => <option key={r} value={r}>{ROLE_TITLE[r]}</option>)}
      </select>
      <select value={status} onChange={e => setStatus(e.target.value as StatusFilter)} aria-label="Состояние">
        <option value="ALL">Активные и заблокированные</option>
        <option value="ACTIVE">Только активные</option>
        <option value="BLOCKED">Заблокированные · {blockedCount}</option>
      </select>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">Никого не найдено.</p>}
    {shown.length > 0 && <div className="editor-table users" role="table" aria-label="Пользователи">
      <div className="row head" role="row"><span role="columnheader">Пользователь</span><span role="columnheader">Роль</span><span role="columnheader">Очки</span><span role="columnheader">С нами с</span><span role="columnheader"><span className="sr-only">Действия</span></span></div>
      {shown.map(r => <div className={r.blocked ? 'row is-blocked' : 'row'} role="row" key={r.id}>
        <span role="cell" className="title-cell">
          {r.avatarUrl
            ? <img className="user-initial photo" src={r.avatarUrl} alt="" />
            : <i className="user-initial" aria-hidden="true">{r.displayName.trim().charAt(0).toUpperCase() || '?'}</i>}
          <span><b>{r.displayName}{r.me && <em className="me-tag">это вы</em>}{r.blocked && <em className="badge blocked-tag">Заблокирован</em>}</b><small>{r.email}</small></span>
        </span>
        <span role="cell">
          {r.me ? <em className={`role-pill r-${r.role.toLowerCase()}`} title="Свою роль изменить нельзя">{ROLE_TITLE[r.role]}</em>
            : <select className={`role-select r-${r.role.toLowerCase()}`} value={r.role} disabled={busy === r.id}
              aria-label={`Роль: ${r.displayName}`} onChange={e => changeRole(r, e.target.value as Role)}>
              {ROLES.map(x => <option key={x} value={x}>{ROLE_TITLE[x]}</option>)}
            </select>}
        </span>
        <span role="cell" className="num">{r.totalPoints}</span>
        <span role="cell" className="date">{dateFmt.format(new Date(r.registeredAt))}</span>
        <span role="cell" className="row-actions">
          {r.me ? null : confirm === r.id ? <>
            <span className="confirm-text">Закрыть доступ?</span>
            <button className="danger" onClick={() => toggleBlock(r)}>Да, заблокировать</button>
            <button onClick={() => setConfirm(null)}>Отмена</button>
          </> : r.blocked
            ? <button disabled={busy === r.id} onClick={() => toggleBlock(r)}>Разблокировать</button>
            : <button className="danger-link" disabled={busy === r.id} onClick={() => setConfirm(r.id)}>Заблокировать</button>}
        </span>
      </div>)}
    </div>}
    <p className="admin-note">Новая роль и блокировка действуют сразу: заблокированный пользователь выходит из аккаунта и не может войти снова.</p>
    </>}
    <div className={toast ? 'editor-toast show' : 'editor-toast'} role="status" aria-live="polite">{toast}</div>
  </main>
}

/** Языки сайта (FR-8.1): включение без изменения кода. Русский - основной, его выключить нельзя. */
function Languages({ notify }: { notify: (t: string) => void }) {
  const [rows, setRows] = useState<AdminLanguage[] | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState<string | null>(null)

  useEffect(() => { adminLanguages().then(setRows).catch(e => setError(errorText(e, 'Не удалось загрузить языки.'))) }, [])

  const toggle = async (l: AdminLanguage) => {
    setBusy(l.code); setError('')
    try {
      const updated = await toggleLanguage(l.code, !l.enabled)
      setRows(rs => (rs ?? []).map(r => r.code === l.code ? updated : r))
      notify(updated.enabled ? `${l.name} язык включён — на сайте появился переключатель` : `${l.name} язык выключен`)
      initLanguages()
    } catch (e) { setError(errorText(e, 'Не удалось изменить язык.')) } finally { setBusy(null) }
  }

  return <section>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {rows && <div className="lang-list">{rows.map(l => <div key={l.code} className={l.enabled ? 'lang-row on' : 'lang-row'}>
      <span className="lang-code">{l.code}</span>
      <span className="lang-name"><b>{l.name}</b><small>{l.nativeName}{l.defaultLanguage ? ' · основной язык' : ` · переводов: ${l.translationCount}`}</small></span>
      <span className="lang-state">{l.enabled ? 'Включён' : 'Выключен'}</span>
      {l.defaultLanguage ? <span className="lang-lock">Всегда включён</span>
        : <button className={l.enabled ? 'switch-btn on' : 'switch-btn'} role="switch" aria-checked={l.enabled} disabled={busy === l.code}
          aria-label={`${l.name} язык`} onClick={() => toggle(l)}><i /></button>}
    </div>)}</div>}
    <div className="lang-steps">
      <b>Как подключить язык без изменения кода</b>
      <ol>
        <li>Переведите материалы в «Редактор → Переводы» — без перевода показывается русский текст.</li>
        <li>Положите словарь интерфейса <code>public/locales/ky.json</code> (копия <code>ru.json</code> с переводом).</li>
        <li>Включите язык здесь — на сайте появится переключатель в меню.</li>
      </ol>
    </div>
  </section>
}
