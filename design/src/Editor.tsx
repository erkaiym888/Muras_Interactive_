import { t, tf, tp } from './i18n'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  ApiError, canEditContent, createCategory, createRite, deleteCategory, deleteRite, editorCategories,
  editorRite, editorRites, renameCategory, setRiteStatus, updateRite,
} from './api'
import type { EditorCategory, EditorRiteRow, RiteForm, RiteStatus, User } from './api'
import { RelationsField } from './EditorRelations'
import { Field, ImagesField, STATUS, STATUSES, StatusOptions, StatusSelect, errorText, imageErrors } from './EditorFields'
import { FolkloreEditor } from './FolkloreEditor'
import { Translations } from './Translations'
import type { FolkloreRef } from './Folklore'

/**
 * Редактор контента (FR-7.1): добавление, просмотр, изменение и удаление обрядов и категорий.
 * Страница видна только ролям EDITOR и ADMIN; сервер дополнительно проверяет роль на каждом запросе.
 */


const emptyForm = (): RiteForm => ({
  title: '', categoryId: null, status: 'DRAFT', summary: '', description: '', symbolism: '', sources: '',
  steps: [{ title: '', text: '' }], images: [], yurtElementKeys: [], heroIds: [], workIds: [],
})

/** Сервер присылает null в пустых полях - в форме нужны строки. */
const normalize = (f: RiteForm): RiteForm => ({
  ...f,
  summary: f.summary ?? '', description: f.description ?? '', symbolism: f.symbolism ?? '', sources: f.sources ?? '',
  steps: (f.steps ?? []).map(s => ({ title: s.title ?? '', text: s.text ?? '' })),
  images: (f.images ?? []).map(i => ({ url: i.url ?? '', caption: i.caption ?? '' })),
  yurtElementKeys: f.yurtElementKeys ?? [], heroIds: f.heroIds ?? [], workIds: f.workIds ?? [],
})

type View = { tab: 'rites' } | { tab: 'form'; id: number | null } | { tab: 'folklore' } | { tab: 'categories' } | { tab: 'translations' }

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
    return <main className="editor"><div className="editor-gate"><p className="kicker">{t('editor.s1', "УПРАВЛЕНИЕ СОДЕРЖИМЫМ")}</p><h1>{t('editor.s2', "Нужен вход")}</h1>
      <p>{t('editor.s3', "Редактор доступен только сотрудникам с ролью редактора контента.")}</p><button className="btn" onClick={goLogin}>{t('editor.s4', "Войти")}</button></div></main>
  }
  if (!canEditContent(user)) {
    return <main className="editor"><div className="editor-gate"><p className="kicker">{t('editor.s1', "УПРАВЛЕНИЕ СОДЕРЖИМЫМ")}</p><h1>{t('editor.s5', "Нет доступа")}</h1>
      <p>{t('editor.s6', "У вашей учётной записи нет роли редактора контента. Обратитесь к администратору.")}</p></div></main>
  }

  const saved = (text: string) => { setToast(text); setView({ tab: 'rites' }) }
  const ritesTab = view.tab === 'rites' || view.tab === 'form'

  return <main className="editor">
    <div className="editor-head">
      <div><p className="kicker">{t('editor.s1', "УПРАВЛЕНИЕ СОДЕРЖИМЫМ")}</p><h1>{t('editor.s7', "Редактор")}{' '}<i>{t('editor.s8', "контента")}</i></h1></div>
      <p className="editor-who">{user.displayName}<span>{user.role === 'ADMIN' ? t('editor.s9', "Администратор") : t('editor.s10', "Редактор контента")}</span></p>
    </div>
    <div className="editor-tabs" role="tablist">
      <button role="tab" aria-selected={ritesTab} className={ritesTab ? 'active' : ''} onClick={() => setView({ tab: 'rites' })}>{t('editor.s11', "Обряды")}</button>
      <button role="tab" aria-selected={view.tab === 'categories'} className={view.tab === 'categories' ? 'active' : ''} onClick={() => setView({ tab: 'categories' })}>{t('editor.s12', "Категории обрядов")}</button>
      <button role="tab" aria-selected={view.tab === 'folklore'} className={view.tab === 'folklore' ? 'active' : ''} onClick={() => setView({ tab: 'folklore' })}>{t('editor.s13', "Фольклор")}</button>
      <button role="tab" aria-selected={view.tab === 'translations'} className={view.tab === 'translations' ? 'active' : ''} onClick={() => setView({ tab: 'translations' })}>{t('editor.s14', "Переводы")}</button>
    </div>
    {view.tab === 'rites' && <RiteList onEdit={id => setView({ tab: 'form', id })} onCreate={() => setView({ tab: 'form', id: null })} openRite={openRite} notify={setToast} />}
    {view.tab === 'form' && <RiteEditor id={view.id} onCancel={() => setView({ tab: 'rites' })} onSaved={saved} />}
    {view.tab === 'categories' && <Categories notify={setToast} />}
    {view.tab === 'translations' && <Translations notify={setToast} />}
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
  const [busy, setBusy] = useState<number | null>(null)

  const load = () => editorRites().then(setRows).catch(e => setError(errorText(e, t('editor.s15', "Не удалось загрузить обряды."))))
  useEffect(() => { load() }, [])

  const shown = useMemo(() => (rows ?? []).filter(r =>
    (status === 'ALL' || r.status === status) &&
    (r.title.toLowerCase().includes(search.trim().toLowerCase()) || r.category.toLowerCase().includes(search.trim().toLowerCase()))
  ), [rows, search, status])

  const remove = async (row: EditorRiteRow) => {
    try {
      await deleteRite(row.id)
      setConfirm(null)
      notify(tf('editor.s16', "«{0}» удалён", row.title))
      load()
    } catch (e) { setError(errorText(e, t('editor.s17', "Не удалось удалить обряд."))) }
  }

  const changeStatus = async (row: EditorRiteRow, next: RiteStatus) => {
    setBusy(row.id); setError('')
    try {
      const updated = await setRiteStatus(row.id, next)
      setRows(rs => (rs ?? []).map(r => r.id === row.id ? { ...r, ...updated } : r))
      notify(`«${row.title}»: ${STATUS[next].toLowerCase()}`)
    } catch (e) { setError(errorText(e, t('editor.s18', "Не удалось сменить статус."))) } finally { setBusy(null) }
  }

  const counts = (s: RiteStatus) => (rows ?? []).filter(r => r.status === s).length

  return <section>
    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder={t('editor.s19', "Найти по названию или категории")} aria-label={t('editor.s20', "Поиск")} />
      <select value={status} onChange={e => setStatus(e.target.value as RiteStatus | 'ALL')} aria-label={t('editor.s21', "Статус")}>
        <option value="ALL">{t('editor.s22', "Все статусы ·")}{' '}{rows?.length ?? 0}</option>
        {STATUSES.map(s => <option key={s} value={s}>{STATUS[s]} · {counts(s)}</option>)}
      </select>
      <button className="btn" onClick={onCreate}>{t('editor.s23', "+ Добавить обряд")}</button>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">{t('editor.s24', "Загрузка…")}</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">{rows.length === 0 ? t('editor.s25', "Обрядов пока нет. Добавьте первый.") : t('editor.s26', "Ничего не найдено.")}</p>}
    {shown.length > 0 && <div className="editor-table" role="table" aria-label={t('editor.s11', "Обряды")}>
      <div className="row head" role="row"><span role="columnheader">{t('editor.s27', "Обряд")}</span><span role="columnheader">{t('editor.s21', "Статус")}</span><span role="columnheader">{t('editor.s28', "Этапов")}</span><span role="columnheader"><span className="sr-only">{t('editor.s29', "Действия")}</span></span></div>
      {shown.map(r => <div className="row" role="row" key={r.id}>
        <span role="cell" className="title-cell">
          {r.imageUrl ? <img src={r.imageUrl} alt="" /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
          <span><b>{r.title}</b><small>{r.category}</small></span>
        </span>
        <span role="cell"><StatusSelect value={r.status} title={r.title} busy={busy === r.id} onChange={s => changeStatus(r, s)} /></span>
        <span role="cell" className="num">{r.stepCount}</span>
        <span role="cell" className="row-actions">
          {confirm === r.id ? <>
            <span className="confirm-text">{t('editor.s30', "Удалить навсегда?")}</span>
            <button className="danger" onClick={() => remove(r)}>{t('editor.s31', "Да, удалить")}</button>
            <button onClick={() => setConfirm(null)}>{t('editor.s32', "Отмена")}</button>
          </> : <>
            {r.status === 'PUBLISHED' && <button onClick={() => openRite(r.id)}>{t('editor.s33', "На сайте")}</button>}
            <button onClick={() => onEdit(r.id)}>{t('editor.s34', "Изменить")}</button>
            <button className="danger-link" onClick={() => setConfirm(r.id)} aria-label={tf('editor.s35', "Удалить {0}", r.title)}>{t('editor.s36', "Удалить")}</button>
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
    editorCategories().then(setCategories).catch(e => setFormError(errorText(e, t('editor.s37', "Не удалось загрузить категории."))))
    if (id !== null) editorRite(id).then(f => setForm(normalize(f))).catch(e => setFormError(errorText(e, t('editor.s38', "Не удалось загрузить обряд."))))
  }, [id])

  if (!form) return <section><button className="back" onClick={onCancel}>{t('editor.s39', "← К списку обрядов")}</button>
    {formError ? <p className="editor-error" role="alert">{formError}</p> : <p className="editor-empty" role="status">{t('editor.s24', "Загрузка…")}</p>}</section>

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
    if (!form.title.trim()) e.title = t('editor.s40', "Введите название")
    if (form.categoryId === null) e.categoryId = t('editor.s41', "Выберите категорию")
    if (form.summary.length > 500) e.summary = t('editor.s42', "Не длиннее 500 символов")
    form.steps.forEach((s, i) => { if (!s.text.trim()) e[`steps[${i}].text`] = t('editor.s43', "Опишите этап или удалите его") })
    Object.assign(e, imageErrors(form.images))
    if (form.status === 'PUBLISHED' && !form.summary.trim()) e.summary = t('editor.s44', "Для публикации нужна аннотация — она видна в каталоге")
    return e
  }

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    const e = validate()
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) { setFormError(t('editor.s45', "Проверьте выделенные поля.")); return }
    setSaving(true)
    try {
      const saved = id === null ? await createRite(form) : await updateRite(id, form)
      onSaved(id === null ? tf('editor.s46', "Обряд «{0}» добавлен", saved.title) : tf('editor.s47', "Изменения в «{0}» сохранены", saved.title))
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) {
        setErrors(err.fieldErrors)
        setFormError(t('editor.s45', "Проверьте выделенные поля."))
      } else setFormError(errorText(err, t('editor.s48', "Не удалось сохранить. Попробуйте ещё раз.")))
    } finally { setSaving(false) }
  }

  return <form className="rite-form" onSubmit={submit} noValidate>
    <div className="form-top">
      <button type="button" className="back" onClick={onCancel}>{t('editor.s39', "← К списку обрядов")}</button>
      <h2>{id === null ? t('editor.s49', "Новый обряд") : form.title || t('editor.s50', "Без названия")}</h2>
    </div>
    <div className="form-grid">
      <div className="form-main">
        <Field label={t('editor.s51', "Название *")} error={errors.title}><input value={form.title} onChange={e => set('title', e.target.value)} maxLength={200} placeholder={t('editor.s52', "Например, Бешик той")} /></Field>
        <Field label={tf('editor.s53', "Аннотация · {0}/500", form.summary.length)} error={errors.summary} hint={t('editor.s54', "Одно-два предложения для карточки в каталоге.")}>
          <textarea rows={2} value={form.summary} onChange={e => set('summary', e.target.value)} maxLength={500} /></Field>
        <Field label={t('editor.s55', "Описание")} error={errors.description} hint={t('editor.s56', "Абзацы разделяйте пустой строкой.")}><textarea rows={7} value={form.description} onChange={e => set('description', e.target.value)} /></Field>
        <Field label={t('editor.s57', "Символика и культурный смысл")} error={errors.symbolism}><textarea rows={3} value={form.symbolism} onChange={e => set('symbolism', e.target.value)} /></Field>
        <Field label={t('editor.s58', "Источники")} error={errors.sources} hint={t('editor.s59', "Книги, статьи, этнографические работы.")}><textarea rows={3} value={form.sources} onChange={e => set('sources', e.target.value)} /></Field>

        <fieldset className="steps">
          <legend>{t('editor.s60', "Этапы обряда")}{' '}<span>{form.steps.length}</span></legend>
          {form.steps.map((s, i) => <div className="step" key={i}>
            <b className="step-no">{String(i + 1).padStart(2, '0')}</b>
            <div>
              <input value={s.title} onChange={e => setStep(i, { title: e.target.value })} placeholder={t('editor.s61', "Название этапа (необязательно)")} aria-label={tf('editor.s62', "Название этапа {0}", i + 1)} maxLength={200} />
              <textarea rows={2} value={s.text} onChange={e => setStep(i, { text: e.target.value })} placeholder={t('editor.s63', "Что происходит на этом этапе")} aria-label={tf('editor.s64', "Описание этапа {0}", i + 1)} className={errors[`steps[${i}].text`] ? 'invalid' : ''} />
              {errors[`steps[${i}].text`] && <small className="err">{errors[`steps[${i}].text`]}</small>}
            </div>
            <div className="step-tools">
              <button type="button" disabled={i === 0} onClick={() => moveStep(i, -1)} aria-label={tf('editor.s65', "Поднять этап {0}", i + 1)}>↑</button>
              <button type="button" disabled={i === form.steps.length - 1} onClick={() => moveStep(i, 1)} aria-label={tf('editor.s66', "Опустить этап {0}", i + 1)}>↓</button>
              <button type="button" onClick={() => set('steps', form.steps.filter((_, j) => j !== i))} aria-label={tf('editor.s67', "Удалить этап {0}", i + 1)}>×</button>
            </div>
          </div>)}
          <button type="button" className="add" onClick={() => set('steps', [...form.steps, { title: '', text: '' }])}>{t('editor.s68', "+ Добавить этап")}</button>
        </fieldset>

        <RelationsField yurtElementKeys={form.yurtElementKeys} heroIds={form.heroIds} workIds={form.workIds}
          onChange={next => setForm({ ...form, ...next })} />
      </div>

      <aside className="form-side">
        <StatusOptions value={form.status} onChange={s => set('status', s)} />
        <div className="side-card">
          <Field label={t('editor.s69', "Категория *")} error={errors.categoryId}>
            <select value={form.categoryId ?? ''} onChange={e => set('categoryId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">{t('editor.s70', "— выберите —")}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
        </div>
        <ImagesField images={form.images} onChange={images => set('images', images)} errors={errors} />
      </aside>
    </div>
    <div className="form-actions">
      {formError && <p className="editor-error" role="alert">{formError}</p>}
      <button type="button" className="btn ghost" onClick={onCancel}>{t('editor.s32', "Отмена")}</button>
      <button type="submit" className="btn" disabled={saving}>{saving ? t('editor.s71', "Сохранение…") : id === null ? t('editor.s72', "Добавить обряд") : t('editor.s73', "Сохранить изменения")}</button>
    </div>
  </form>
}

// ---------- Категории ----------

function Categories({ notify }: { notify: (t: string) => void }) {
  const [rows, setRows] = useState<EditorCategory[] | null>(null)
  const [name, setName] = useState('')
  const [editing, setEditing] = useState<{ id: number; name: string } | null>(null)
  const [error, setError] = useState('')

  const load = () => editorCategories().then(setRows).catch(e => setError(errorText(e, t('editor.s37', "Не удалось загрузить категории."))))
  useEffect(() => { load() }, [])

  const fail = (e: unknown, fallback: string) => {
    setError(e instanceof ApiError && e.fieldErrors.name ? e.fieldErrors.name : errorText(e, fallback))
  }

  const add = async (ev: FormEvent) => {
    ev.preventDefault()
    if (!name.trim()) { setError(t('editor.s74', "Введите название категории")); return }
    try { await createCategory(name.trim()); notify(tf('editor.s75', "Категория «{0}» добавлена", name.trim())); setName(''); setError(''); load() }
    catch (e) { fail(e, t('editor.s76', "Не удалось добавить категорию.")) }
  }
  const rename = async () => {
    if (!editing || !editing.name.trim()) return
    try { await renameCategory(editing.id, editing.name.trim()); notify(t('editor.s77', "Категория переименована")); setEditing(null); setError(''); load() }
    catch (e) { fail(e, t('editor.s78', "Не удалось переименовать категорию.")) }
  }
  const remove = async (c: EditorCategory) => {
    try { await deleteCategory(c.id); notify(tf('editor.s79', "Категория «{0}» удалена", c.name)); setError(''); load() }
    catch (e) { fail(e, t('editor.s80', "Не удалось удалить категорию.")) }
  }

  return <section className="categories">
    <form className="editor-toolbar" onSubmit={add}>
      <input value={name} onChange={e => setName(e.target.value)} placeholder={t('editor.s81', "Новая категория, например «Обряды взросления»")} maxLength={100} aria-label={t('editor.s82', "Название новой категории")} />
      <button className="btn" type="submit">{t('editor.s83', "+ Добавить")}</button>
    </form>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">{t('editor.s24', "Загрузка…")}</p>}
    {rows && <ul className="cat-list">{rows.map(c => <li key={c.id}>
      {editing?.id === c.id
        ? <input autoFocus value={editing.name} onChange={e => setEditing({ id: c.id, name: e.target.value })} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); rename() } if (e.key === 'Escape') setEditing(null) }} aria-label={t('editor.s84', "Новое название")} maxLength={100} />
        : <b>{c.name}</b>}
      <span className="count">{c.riteCount} {plural(c.riteCount)}</span>
      <span className="row-actions">
        {editing?.id === c.id
          ? <><button onClick={rename}>{t('editor.s85', "Сохранить")}</button><button onClick={() => setEditing(null)}>{t('editor.s32', "Отмена")}</button></>
          : <><button onClick={() => setEditing({ id: c.id, name: c.name })}>{t('editor.s86', "Переименовать")}</button>
            <button className="danger-link" disabled={c.riteCount > 0} title={c.riteCount > 0 ? t('editor.s87', "Сначала перенесите или удалите обряды этой категории") : undefined} onClick={() => remove(c)}>{t('editor.s36', "Удалить")}</button></>}
      </span>
    </li>)}</ul>}
  </section>
}

function plural(n: number) {
  return tp('plural.rites', n, 'обряд', 'обряда', 'обрядов')
}
