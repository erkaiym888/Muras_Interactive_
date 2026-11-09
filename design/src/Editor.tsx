import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  ApiError, canEditContent, createCategory, createRite, deleteCategory, deleteRite, editorCategories,
  editorRite, editorRites, renameCategory, updateRite,
} from './api'
import type { EditorCategory, EditorRiteRow, RiteForm, RiteStatus, User } from './api'
import { Field, ImagesField, errorText, imageErrors } from './EditorFields'
import { FolkloreEditor } from './FolkloreEditor'
import type { FolkloreRef } from './Folklore'

/**
 * Редактор контента (FR-7.1): добавление, просмотр, изменение и удаление обрядов и категорий.
 * Страница видна только ролям EDITOR и ADMIN; сервер дополнительно проверяет роль на каждом запросе.
 */

const STATUS: Record<RiteStatus, string> = { PUBLISHED: 'Опубликован', DRAFT: 'Черновик', HIDDEN: 'Скрыт' }
const STATUS_HINT: Record<RiteStatus, string> = {
  PUBLISHED: 'Виден всем посетителям в каталоге.',
  DRAFT: 'Виден только в редакторе, пока вы работаете над текстом.',
  HIDDEN: 'Убран из каталога, но не удалён.',
}

const emptyForm = (): RiteForm => ({
  title: '', categoryId: null, status: 'DRAFT', summary: '', description: '', symbolism: '', sources: '',
  steps: [{ title: '', text: '' }], images: [],
})

/** Сервер присылает null в пустых полях - в форме нужны строки. */
const normalize = (f: RiteForm): RiteForm => ({
  ...f,
  summary: f.summary ?? '', description: f.description ?? '', symbolism: f.symbolism ?? '', sources: f.sources ?? '',
  steps: (f.steps ?? []).map(s => ({ title: s.title ?? '', text: s.text ?? '' })),
  images: (f.images ?? []).map(i => ({ url: i.url ?? '', caption: i.caption ?? '' })),
})

type View = { tab: 'rites' } | { tab: 'form'; id: number | null } | { tab: 'folklore' } | { tab: 'categories' }

export function Editor({ user, goLogin, openRite, openFolklore }: {
  user: User | null; goLogin: () => void; openRite: (id: number) => void; openFolklore: (ref: FolkloreRef) => void
}) {
  const [view, setView] = useState<View>({ tab: 'rites' })
  const [toast, setToast] = useState('')

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(''), 3500)
    return () => clearTimeout(t)
  }, [toast])

  if (!user) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">УПРАВЛЕНИЕ СОДЕРЖИМЫМ</p><h1>Нужен вход</h1>
      <p>Редактор доступен только сотрудникам с ролью редактора контента.</p><button className="btn" onClick={goLogin}>Войти</button></div></main>
  }
  if (!canEditContent(user)) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">УПРАВЛЕНИЕ СОДЕРЖИМЫМ</p><h1>Нет доступа</h1>
      <p>У вашей учётной записи нет роли редактора контента. Обратитесь к администратору.</p></div></main>
  }

  const saved = (text: string) => { setToast(text); setView({ tab: 'rites' }) }
  const ritesTab = view.tab === 'rites' || view.tab === 'form'

  return <main className="editor">
    <div className="editor-head">
      <div><p className="kicker">УПРАВЛЕНИЕ СОДЕРЖИМЫМ</p><h1>Редактор <i>контента</i></h1></div>
      <p className="editor-who">{user.displayName}<span>{user.role === 'ADMIN' ? 'Администратор' : 'Редактор контента'}</span></p>
    </div>
    <div className="editor-tabs" role="tablist">
      <button role="tab" aria-selected={ritesTab} className={ritesTab ? 'active' : ''} onClick={() => setView({ tab: 'rites' })}>Обряды</button>
      <button role="tab" aria-selected={view.tab === 'categories'} className={view.tab === 'categories' ? 'active' : ''} onClick={() => setView({ tab: 'categories' })}>Категории обрядов</button>
      <button role="tab" aria-selected={view.tab === 'folklore'} className={view.tab === 'folklore' ? 'active' : ''} onClick={() => setView({ tab: 'folklore' })}>Фольклор</button>
    </div>
    {view.tab === 'rites' && <RiteList onEdit={id => setView({ tab: 'form', id })} onCreate={() => setView({ tab: 'form', id: null })} openRite={openRite} notify={setToast} />}
    {view.tab === 'form' && <RiteEditor id={view.id} onCancel={() => setView({ tab: 'rites' })} onSaved={saved} />}
    {view.tab === 'categories' && <Categories notify={setToast} />}
    {view.tab === 'folklore' && <FolkloreEditor notify={setToast} openFolklore={openFolklore} />}
    <div className={toast ? 'editor-toast show' : 'editor-toast'} role="status" aria-live="polite">{toast}</div>
  </main>
}

// ---------- Список обрядов ----------

function RiteList({ onEdit, onCreate, openRite, notify }: {
  onEdit: (id: number) => void; onCreate: () => void; openRite: (id: number) => void; notify: (t: string) => void
}) {
  const [rows, setRows] = useState<EditorRiteRow[] | null>(null)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<RiteStatus | 'ALL'>('ALL')
  const [confirm, setConfirm] = useState<number | null>(null)

  const load = () => editorRites().then(setRows).catch(e => setError(errorText(e, 'Не удалось загрузить обряды.')))
  useEffect(() => { load() }, [])

  const shown = useMemo(() => (rows ?? []).filter(r =>
    (status === 'ALL' || r.status === status) &&
    (r.title.toLowerCase().includes(search.trim().toLowerCase()) || r.category.toLowerCase().includes(search.trim().toLowerCase()))
  ), [rows, search, status])

  const remove = async (row: EditorRiteRow) => {
    try {
      await deleteRite(row.id)
      setConfirm(null)
      notify(`«${row.title}» удалён`)
      load()
    } catch (e) { setError(errorText(e, 'Не удалось удалить обряд.')) }
  }

  const counts = (s: RiteStatus) => (rows ?? []).filter(r => r.status === s).length

  return <section>
    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Найти по названию или категории" aria-label="Поиск" />
      <select value={status} onChange={e => setStatus(e.target.value as RiteStatus | 'ALL')} aria-label="Статус">
        <option value="ALL">Все статусы · {rows?.length ?? 0}</option>
        {(Object.keys(STATUS) as RiteStatus[]).map(s => <option key={s} value={s}>{STATUS[s]} · {counts(s)}</option>)}
      </select>
      <button className="btn" onClick={onCreate}>+ Добавить обряд</button>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">{rows.length === 0 ? 'Обрядов пока нет. Добавьте первый.' : 'Ничего не найдено.'}</p>}
    {shown.length > 0 && <div className="editor-table" role="table" aria-label="Обряды">
      <div className="row head" role="row"><span role="columnheader">Обряд</span><span role="columnheader">Статус</span><span role="columnheader">Этапов</span><span role="columnheader"><span className="sr-only">Действия</span></span></div>
      {shown.map(r => <div className="row" role="row" key={r.id}>
        <span role="cell" className="title-cell">
          {r.imageUrl ? <img src={r.imageUrl} alt="" /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
          <span><b>{r.title}</b><small>{r.category}</small></span>
        </span>
        <span role="cell"><em className={`badge ${r.status.toLowerCase()}`}>{STATUS[r.status]}</em></span>
        <span role="cell" className="num">{r.stepCount}</span>
        <span role="cell" className="row-actions">
          {confirm === r.id ? <>
            <span className="confirm-text">Удалить навсегда?</span>
            <button className="danger" onClick={() => remove(r)}>Да, удалить</button>
            <button onClick={() => setConfirm(null)}>Отмена</button>
          </> : <>
            {r.status === 'PUBLISHED' && <button onClick={() => openRite(r.id)}>На сайте</button>}
            <button onClick={() => onEdit(r.id)}>Изменить</button>
            <button className="danger-link" onClick={() => setConfirm(r.id)} aria-label={`Удалить ${r.title}`}>Удалить</button>
          </>}
        </span>
      </div>)}
    </div>}
  </section>
}

// ---------- Форма обряда ----------

function RiteEditor({ id, onCancel, onSaved }: { id: number | null; onCancel: () => void; onSaved: (text: string) => void }) {
  const [form, setForm] = useState<RiteForm | null>(id === null ? emptyForm() : null)
  const [categories, setCategories] = useState<EditorCategory[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    editorCategories().then(setCategories).catch(e => setFormError(errorText(e, 'Не удалось загрузить категории.')))
    if (id !== null) editorRite(id).then(f => setForm(normalize(f))).catch(e => setFormError(errorText(e, 'Не удалось загрузить обряд.')))
  }, [id])

  if (!form) return <section><button className="back" onClick={onCancel}>← К списку обрядов</button>
    {formError ? <p className="editor-error" role="alert">{formError}</p> : <p className="editor-empty" role="status">Загрузка…</p>}</section>

  const set = <K extends keyof RiteForm>(key: K, value: RiteForm[K]) => {
    setForm({ ...form, [key]: value })
    // Ошибка поля исчезает, как только его исправляют
    if (errors[key as string]) setErrors(({ [key as string]: _, ...rest }) => rest)
  }
  const setStep = (i: number, patch: Partial<RiteForm['steps'][number]>) => set('steps', form.steps.map((s, j) => j === i ? { ...s, ...patch } : s))
  const moveStep = (i: number, d: number) => {
    const steps = [...form.steps]
    ;[steps[i], steps[i + d]] = [steps[i + d], steps[i]]
    set('steps', steps)
  }

  const validate = () => {
    const e: Record<string, string> = {}
    if (!form.title.trim()) e.title = 'Введите название'
    if (form.categoryId === null) e.categoryId = 'Выберите категорию'
    if (form.summary.length > 500) e.summary = 'Не длиннее 500 символов'
    form.steps.forEach((s, i) => { if (!s.text.trim()) e[`steps[${i}].text`] = 'Опишите этап или удалите его' })
    Object.assign(e, imageErrors(form.images))
    if (form.status === 'PUBLISHED' && !form.summary.trim()) e.summary = 'Для публикации нужна аннотация — она видна в каталоге'
    return e
  }

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    const e = validate()
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) { setFormError('Проверьте выделенные поля.'); return }
    setSaving(true)
    try {
      const saved = id === null ? await createRite(form) : await updateRite(id, form)
      onSaved(id === null ? `Обряд «${saved.title}» добавлен` : `Изменения в «${saved.title}» сохранены`)
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) {
        setErrors(err.fieldErrors)
        setFormError('Проверьте выделенные поля.')
      } else setFormError(errorText(err, 'Не удалось сохранить. Попробуйте ещё раз.'))
    } finally { setSaving(false) }
  }

  return <form className="rite-form" onSubmit={submit} noValidate>
    <div className="form-top">
      <button type="button" className="back" onClick={onCancel}>← К списку обрядов</button>
      <h2>{id === null ? 'Новый обряд' : form.title || 'Без названия'}</h2>
    </div>
    <div className="form-grid">
      <div className="form-main">
        <Field label="Название *" error={errors.title}><input value={form.title} onChange={e => set('title', e.target.value)} maxLength={200} placeholder="Например, Бешик той" /></Field>
        <Field label={`Аннотация · ${form.summary.length}/500`} error={errors.summary} hint="Одно-два предложения для карточки в каталоге.">
          <textarea rows={2} value={form.summary} onChange={e => set('summary', e.target.value)} maxLength={500} /></Field>
        <Field label="Описание" error={errors.description} hint="Абзацы разделяйте пустой строкой."><textarea rows={7} value={form.description} onChange={e => set('description', e.target.value)} /></Field>
        <Field label="Символика и культурный смысл" error={errors.symbolism}><textarea rows={3} value={form.symbolism} onChange={e => set('symbolism', e.target.value)} /></Field>
        <Field label="Источники" error={errors.sources} hint="Книги, статьи, этнографические работы."><textarea rows={3} value={form.sources} onChange={e => set('sources', e.target.value)} /></Field>

        <fieldset className="steps">
          <legend>Этапы обряда <span>{form.steps.length}</span></legend>
          {form.steps.map((s, i) => <div className="step" key={i}>
            <b className="step-no">{String(i + 1).padStart(2, '0')}</b>
            <div>
              <input value={s.title} onChange={e => setStep(i, { title: e.target.value })} placeholder="Название этапа (необязательно)" aria-label={`Название этапа ${i + 1}`} maxLength={200} />
              <textarea rows={2} value={s.text} onChange={e => setStep(i, { text: e.target.value })} placeholder="Что происходит на этом этапе" aria-label={`Описание этапа ${i + 1}`} className={errors[`steps[${i}].text`] ? 'invalid' : ''} />
              {errors[`steps[${i}].text`] && <small className="err">{errors[`steps[${i}].text`]}</small>}
            </div>
            <div className="step-tools">
              <button type="button" disabled={i === 0} onClick={() => moveStep(i, -1)} aria-label={`Поднять этап ${i + 1}`}>↑</button>
              <button type="button" disabled={i === form.steps.length - 1} onClick={() => moveStep(i, 1)} aria-label={`Опустить этап ${i + 1}`}>↓</button>
              <button type="button" onClick={() => set('steps', form.steps.filter((_, j) => j !== i))} aria-label={`Удалить этап ${i + 1}`}>×</button>
            </div>
          </div>)}
          <button type="button" className="add" onClick={() => set('steps', [...form.steps, { title: '', text: '' }])}>+ Добавить этап</button>
        </fieldset>
      </div>

      <aside className="form-side">
        <div className="side-card">
          <span className="side-label">Статус</span>
          {(Object.keys(STATUS) as RiteStatus[]).map(s => <label className={form.status === s ? 'status-opt active' : 'status-opt'} key={s}>
            <input type="radio" name="status" checked={form.status === s} onChange={() => set('status', s)} />
            <span><b><em className={`badge ${s.toLowerCase()}`}>{STATUS[s]}</em></b><small>{STATUS_HINT[s]}</small></span>
          </label>)}
        </div>
        <div className="side-card">
          <Field label="Категория *" error={errors.categoryId}>
            <select value={form.categoryId ?? ''} onChange={e => set('categoryId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">— выберите —</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
        </div>
        <ImagesField images={form.images} onChange={images => set('images', images)} errors={errors} />
      </aside>
    </div>
    <div className="form-actions">
      {formError && <p className="editor-error" role="alert">{formError}</p>}
      <button type="button" className="btn ghost" onClick={onCancel}>Отмена</button>
      <button type="submit" className="btn" disabled={saving}>{saving ? 'Сохранение…' : id === null ? 'Добавить обряд' : 'Сохранить изменения'}</button>
    </div>
  </form>
}

// ---------- Категории ----------

function Categories({ notify }: { notify: (t: string) => void }) {
  const [rows, setRows] = useState<EditorCategory[] | null>(null)
  const [name, setName] = useState('')
  const [editing, setEditing] = useState<{ id: number; name: string } | null>(null)
  const [error, setError] = useState('')

  const load = () => editorCategories().then(setRows).catch(e => setError(errorText(e, 'Не удалось загрузить категории.')))
  useEffect(() => { load() }, [])

  const fail = (e: unknown, fallback: string) => {
    setError(e instanceof ApiError && e.fieldErrors.name ? e.fieldErrors.name : errorText(e, fallback))
  }

  const add = async (ev: FormEvent) => {
    ev.preventDefault()
    if (!name.trim()) { setError('Введите название категории'); return }
    try { await createCategory(name.trim()); notify(`Категория «${name.trim()}» добавлена`); setName(''); setError(''); load() }
    catch (e) { fail(e, 'Не удалось добавить категорию.') }
  }
  const rename = async () => {
    if (!editing || !editing.name.trim()) return
    try { await renameCategory(editing.id, editing.name.trim()); notify('Категория переименована'); setEditing(null); setError(''); load() }
    catch (e) { fail(e, 'Не удалось переименовать категорию.') }
  }
  const remove = async (c: EditorCategory) => {
    try { await deleteCategory(c.id); notify(`Категория «${c.name}» удалена`); setError(''); load() }
    catch (e) { fail(e, 'Не удалось удалить категорию.') }
  }

  return <section className="categories">
    <form className="editor-toolbar" onSubmit={add}>
      <input value={name} onChange={e => setName(e.target.value)} placeholder="Новая категория, например «Обряды взросления»" maxLength={100} aria-label="Название новой категории" />
      <button className="btn" type="submit">+ Добавить</button>
    </form>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {rows && <ul className="cat-list">{rows.map(c => <li key={c.id}>
      {editing?.id === c.id
        ? <input autoFocus value={editing.name} onChange={e => setEditing({ id: c.id, name: e.target.value })} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); rename() } if (e.key === 'Escape') setEditing(null) }} aria-label="Новое название" maxLength={100} />
        : <b>{c.name}</b>}
      <span className="count">{c.riteCount} {plural(c.riteCount)}</span>
      <span className="row-actions">
        {editing?.id === c.id
          ? <><button onClick={rename}>Сохранить</button><button onClick={() => setEditing(null)}>Отмена</button></>
          : <><button onClick={() => setEditing({ id: c.id, name: c.name })}>Переименовать</button>
            <button className="danger-link" disabled={c.riteCount > 0} title={c.riteCount > 0 ? 'Сначала перенесите или удалите обряды этой категории' : undefined} onClick={() => remove(c)}>Удалить</button></>}
      </span>
    </li>)}</ul>}
  </section>
}

function plural(n: number) {
  const m10 = n % 10, m100 = n % 100
  if (m10 === 1 && m100 !== 11) return 'обряд'
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return 'обряда'
  return 'обрядов'
}
