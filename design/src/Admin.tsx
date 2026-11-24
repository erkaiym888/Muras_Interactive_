import { useEffect, useMemo, useState } from 'react'
import { adminLanguages, adminRoles, adminUsers, isAdmin, setUserBlocked, setUserRole, toggleLanguage } from './api'
import type { AdminLanguage, AdminRole, AdminUserRow, Role, User } from './api'
import { initLanguages, locale, lrec, t, tf, ts } from './i18n'
import { errorText } from './EditorFields'

/**
 * Пользователи и роли (FR-7.3). Страница только для администратора; сервер проверяет роль на каждом запросе
 * и не даёт изменить себя или оставить систему без администратора.
 */

const ROLE_TITLE: Record<Role, string> = lrec('admin.role', { USER: 'Пользователь', EDITOR: 'Редактор контента', ADMIN: 'Администратор' })
const ROLES: Role[] = ['USER', 'EDITOR', 'ADMIN']
type StatusFilter = 'ALL' | 'ACTIVE' | 'BLOCKED'

const dateFmt = () => new Intl.DateTimeFormat(locale(), { day: 'numeric', month: 'short', year: 'numeric' })

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
    adminUsers().then(setRows).catch(e => setError(errorText(e, t('admin.s1', "Не удалось загрузить пользователей."))))
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
    return <main className="editor"><div className="editor-gate"><p className="kicker">{t('admin.s2', "АДМИНИСТРИРОВАНИЕ")}</p><h1>{t('admin.s3', "Нужен вход")}</h1>
      <p>{t('admin.s4', "Раздел доступен только администратору.")}</p><button className="btn" onClick={goLogin}>{t('admin.s5', "Войти")}</button></div></main>
  }
  if (!allowed) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">{t('admin.s2', "АДМИНИСТРИРОВАНИЕ")}</p><h1>{t('admin.s6', "Нет доступа")}</h1>
      <p>{t('admin.s7', "Управлять пользователями и ролями может только администратор.")}</p></div></main>
  }

  const replace = (row: AdminUserRow) => setRows(rs => (rs ?? []).map(r => r.id === row.id ? row : r))

  const changeRole = async (r: AdminUserRow, next: Role) => {
    setBusy(r.id); setError('')
    try {
      replace(await setUserRole(r.id, next))
      loadRoles()
      setToast(`${r.displayName}: ${ROLE_TITLE[next].toLowerCase()}`)
    } catch (e) { setError(errorText(e, t('admin.s8', "Не удалось сменить роль."))) } finally { setBusy(null) }
  }

  const toggleBlock = async (r: AdminUserRow) => {
    setBusy(r.id); setError(''); setConfirm(null)
    try {
      replace(await setUserBlocked(r.id, !r.blocked))
      setToast(r.blocked ? tf('admin.s9', "{0} разблокирован(а)", r.displayName) : tf('admin.s10', "{0} заблокирован(а)", r.displayName))
    } catch (e) { setError(errorText(e, t('admin.s11', "Не удалось изменить блокировку."))) } finally { setBusy(null) }
  }

  const blockedCount = (rows ?? []).filter(r => r.blocked).length

  return <main className="editor admin">
    <div className="editor-head">
      <div><p className="kicker">{t('admin.s2', "АДМИНИСТРИРОВАНИЕ")}</p><h1>{t('admin.s12', "Пользователи")}{' '}<i>{t('admin.s13', "и роли")}</i></h1></div>
      <p className="editor-who">{user.displayName}<span>{t('admin.s14', "Администратор")}</span></p>
    </div>
    <div className="editor-tabs" role="tablist">
      <button role="tab" aria-selected={tab === 'users'} className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>{t('admin.s12', "Пользователи")}</button>
      <button role="tab" aria-selected={tab === 'languages'} className={tab === 'languages' ? 'active' : ''} onClick={() => setTab('languages')}>{t('admin.s15', "Языки")}</button>
    </div>
    {tab === 'languages' ? <Languages notify={setToast} /> : <>

    <div className="role-cards">
      {(roles.length ? roles : ROLES.map(n => ({ name: n, title: ROLE_TITLE[n], description: '', userCount: 0 }))).map(r =>
        <button key={r.name} className={role === r.name ? `role-card r-${r.name.toLowerCase()} active` : `role-card r-${r.name.toLowerCase()}`}
          aria-pressed={role === r.name} onClick={() => setRole(role === r.name ? 'ALL' : r.name)}>
          <b>{r.userCount}</b><span>{ts(r.title)}</span><small>{ts(r.description)}</small>
        </button>)}
    </div>

    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder={t('admin.s16', "Найти по имени или почте")} aria-label={t('admin.s17', "Поиск")} />
      <select value={role} onChange={e => setRole(e.target.value as Role | 'ALL')} aria-label={t('admin.s18', "Роль")}>
        <option value="ALL">{t('admin.s19', "Все роли ·")}{' '}{rows?.length ?? 0}</option>
        {ROLES.map(r => <option key={r} value={r}>{ROLE_TITLE[r]}</option>)}
      </select>
      <select value={status} onChange={e => setStatus(e.target.value as StatusFilter)} aria-label={t('admin.s20', "Состояние")}>
        <option value="ALL">{t('admin.s21', "Активные и заблокированные")}</option>
        <option value="ACTIVE">{t('admin.s22', "Только активные")}</option>
        <option value="BLOCKED">{t('admin.s23', "Заблокированные ·")}{' '}{blockedCount}</option>
      </select>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">{t('admin.s24', "Загрузка…")}</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">{t('admin.s25', "Никого не найдено.")}</p>}
    {shown.length > 0 && <div className="editor-table users" role="table" aria-label={t('admin.s12', "Пользователи")}>
      <div className="row head" role="row"><span role="columnheader">{t('admin.s26', "Пользователь")}</span><span role="columnheader">{t('admin.s18', "Роль")}</span><span role="columnheader">{t('admin.s27', "Очки")}</span><span role="columnheader">{t('admin.s28', "С нами с")}</span><span role="columnheader"><span className="sr-only">{t('admin.s29', "Действия")}</span></span></div>
      {shown.map(r => <div className={r.blocked ? 'row is-blocked' : 'row'} role="row" key={r.id}>
        <span role="cell" className="title-cell">
          {r.avatarUrl
            ? <img className="user-initial photo" src={r.avatarUrl} alt="" />
            : <i className="user-initial" aria-hidden="true">{r.displayName.trim().charAt(0).toUpperCase() || '?'}</i>}
          <span><b>{r.displayName}{r.me && <em className="me-tag">{t('admin.s30', "это вы")}</em>}{r.blocked && <em className="badge blocked-tag">{t('admin.s31', "Заблокирован")}</em>}</b><small>{r.email}</small></span>
        </span>
        <span role="cell">
          {r.me ? <em className={`role-pill r-${r.role.toLowerCase()}`} title={t('admin.s32', "Свою роль изменить нельзя")}>{ROLE_TITLE[r.role]}</em>
            : <select className={`role-select r-${r.role.toLowerCase()}`} value={r.role} disabled={busy === r.id}
              aria-label={tf('admin.s33', "Роль: {0}", r.displayName)} onChange={e => changeRole(r, e.target.value as Role)}>
              {ROLES.map(x => <option key={x} value={x}>{ROLE_TITLE[x]}</option>)}
            </select>}
        </span>
        <span role="cell" className="num">{r.totalPoints}</span>
        <span role="cell" className="date">{dateFmt().format(new Date(r.registeredAt))}</span>
        <span role="cell" className="row-actions">
          {r.me ? null : confirm === r.id ? <>
            <span className="confirm-text">{t('admin.s34', "Закрыть доступ?")}</span>
            <button className="danger" onClick={() => toggleBlock(r)}>{t('admin.s35', "Да, заблокировать")}</button>
            <button onClick={() => setConfirm(null)}>{t('admin.s36', "Отмена")}</button>
          </> : r.blocked
            ? <button disabled={busy === r.id} onClick={() => toggleBlock(r)}>{t('admin.s37', "Разблокировать")}</button>
            : <button className="danger-link" disabled={busy === r.id} onClick={() => setConfirm(r.id)}>{t('admin.s38', "Заблокировать")}</button>}
        </span>
      </div>)}
    </div>}
    <p className="admin-note">{t('admin.s39', "Новая роль и блокировка действуют сразу: заблокированный пользователь выходит из аккаунта и не может войти снова.")}</p>
    </>}
    <div className={toast ? 'editor-toast show' : 'editor-toast'} role="status" aria-live="polite">{toast}</div>
  </main>
}

/** Языки сайта (FR-8.1): включение без изменения кода. Русский - основной, его выключить нельзя. */
function Languages({ notify }: { notify: (t: string) => void }) {
  const [rows, setRows] = useState<AdminLanguage[] | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState<string | null>(null)

  useEffect(() => { adminLanguages().then(setRows).catch(e => setError(errorText(e, t('admin.s40', "Не удалось загрузить языки.")))) }, [])

  const toggle = async (l: AdminLanguage) => {
    setBusy(l.code); setError('')
    try {
      const updated = await toggleLanguage(l.code, !l.enabled)
      setRows(rs => (rs ?? []).map(r => r.code === l.code ? updated : r))
      notify(updated.enabled ? tf('admin.s41', "{0} язык включён — на сайте появился переключатель", l.name) : tf('admin.s42', "{0} язык выключен", l.name))
      initLanguages()
    } catch (e) { setError(errorText(e, t('admin.s43', "Не удалось изменить язык."))) } finally { setBusy(null) }
  }

  return <section>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">{t('admin.s24', "Загрузка…")}</p>}
    {rows && <div className="lang-list">{rows.map(l => <div key={l.code} className={l.enabled ? 'lang-row on' : 'lang-row'}>
      <span className="lang-code">{l.code}</span>
      <span className="lang-name"><b>{l.name}</b><small>{l.nativeName}{l.defaultLanguage ? t('admin.s44', " · основной язык") : tf('admin.s45', " · переводов: {0}", l.translationCount)}</small></span>
      <span className="lang-state">{l.enabled ? t('admin.s46', "Включён") : t('admin.s47', "Выключен")}</span>
      {l.defaultLanguage ? <span className="lang-lock">{t('admin.s48', "Всегда включён")}</span>
        : <button className={l.enabled ? 'switch-btn on' : 'switch-btn'} role="switch" aria-checked={l.enabled} disabled={busy === l.code}
          aria-label={tf('admin.s49', "{0} язык", l.name)} onClick={() => toggle(l)}><i /></button>}
    </div>)}</div>}
    <div className="lang-steps">
      <b>{t('admin.s50', "Как подключить язык без изменения кода")}</b>
      <ol>
        <li>{t('admin.s51', "Переведите материалы в «Редактор → Переводы» — без перевода показывается русский текст.")}</li>
        <li>{t('admin.s52', "Положите словарь интерфейса")}{' '}<code>public/locales/ky.json</code>{' '}{t('admin.s53', "(копия")}{' '}<code>ru.json</code>{' '}{t('admin.s54', "с переводом).")}</li>
        <li>{t('admin.s55', "Включите язык здесь — на сайте появится переключатель в меню.")}</li>
      </ol>
    </div>
  </section>
}
