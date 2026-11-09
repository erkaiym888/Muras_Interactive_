import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  ApiError, createHero, createWork, deleteHero, deleteWork, editorFolklore, editorFolkloreTypes, editorHero,
  editorWork, updateHero, updateWork,
} from './api'
import type { EditorFolkloreRow, EditorImage, FolkloreKind, FolkloreType } from './api'
import { Field, ImagesField, errorText, imageErrors } from './EditorFields'
import type { FolkloreRef } from './Folklore'

/** Управление фольклором (FR-7.1): герои и произведения, связи между ними, иллюстрации. */

const KIND: Record<FolkloreKind, string> = { hero: 'Герой', work: 'Произведение' }

type View = { mode: 'list' } | { mode: 'form'; kind: FolkloreKind; id: number | null }

export function FolkloreEditor({ notify, openFolklore }: { notify: (t: string) => void; openFolklore: (ref: FolkloreRef) => void }) {
  const [view, setView] = useState<View>({ mode: 'list' })
  if (view.mode === 'form') {
    return <FolkloreForm kind={view.kind} id={view.id} onCancel={() => setView({ mode: 'list' })}
      onSaved={text => { notify(text); setView({ mode: 'list' }) }} />
  }
  return <FolkloreList notify={notify} openFolklore={openFolklore}
    onEdit={(kind, id) => setView({ mode: 'form', kind, id })} />
}

// ---------- Список ----------

function FolkloreList({ notify, openFolklore, onEdit }: {
  notify: (t: string) => void; openFolklore: (ref: FolkloreRef) => void; onEdit: (kind: FolkloreKind, id: number | null) => void
}) {
  const [rows, setRows] = useState<EditorFolkloreRow[] | null>(null)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [kind, setKind] = useState<FolkloreKind | 'ALL'>('ALL')
  const [confirm, setConfirm] = useState<string | null>(null)

  const load = () => editorFolklore().then(setRows).catch(e => setError(errorText(e, 'Не удалось загрузить фольклор.')))
  useEffect(() => { load() }, [])

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (rows ?? []).filter(r => (kind === 'ALL' || r.kind === kind) &&
      (r.title.toLowerCase().includes(q) || r.type.toLowerCase().includes(q)))
  }, [rows, search, kind])

  const remove = async (r: EditorFolkloreRow) => {
    try {
      await (r.kind === 'hero' ? deleteHero(r.id) : deleteWork(r.id))
      setConfirm(null)
      notify(`«${r.title}» ${r.kind === 'hero' ? 'удалён' : 'удалено'}`)
      load()
    } catch (e) { setError(errorText(e, 'Не удалось удалить.')) }
  }

  const count = (k: FolkloreKind) => (rows ?? []).filter(r => r.kind === k).length

  return <section>
    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Найти по названию или типу" aria-label="Поиск" />
      <select value={kind} onChange={e => setKind(e.target.value as FolkloreKind | 'ALL')} aria-label="Вид">
        <option value="ALL">Всё · {rows?.length ?? 0}</option>
        <option value="hero">Герои · {count('hero')}</option>
        <option value="work">Произведения · {count('work')}</option>
      </select>
      <button className="btn ghost" onClick={() => onEdit('hero', null)}>+ Герой</button>
      <button className="btn" onClick={() => onEdit('work', null)}>+ Произведение</button>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">{rows.length === 0 ? 'Пока пусто. Добавьте героя или произведение.' : 'Ничего не найдено.'}</p>}
    {shown.length > 0 && <div className="editor-table" role="table" aria-label="Фольклор">
      <div className="row head" role="row"><span role="columnheader">Материал</span><span role="columnheader">Вид</span><span role="columnheader">Связей</span><span role="columnheader"><span className="sr-only">Действия</span></span></div>
      {shown.map(r => { const key = `${r.kind}-${r.id}`; return <div className="row" role="row" key={key}>
        <span role="cell" className="title-cell">
          {r.imageUrl ? <img src={r.imageUrl} alt="" /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
          <span><b>{r.title}</b><small>{r.type}</small></span>
        </span>
        <span role="cell"><em className={`badge kind-${r.kind}`}>{KIND[r.kind]}</em></span>
        <span role="cell" className="num" title={r.kind === 'hero' ? 'Произведений с этим героем' : 'Героев в произведении'}>{r.linkCount}</span>
        <span role="cell" className="row-actions">
          {confirm === key ? <>
            <span className="confirm-text">Удалить навсегда?</span>
            <button className="danger" onClick={() => remove(r)}>Да, удалить</button>
            <button onClick={() => setConfirm(null)}>Отмена</button>
          </> : <>
            <button onClick={() => openFolklore({ kind: r.kind, id: r.id })}>На сайте</button>
            <button onClick={() => onEdit(r.kind, r.id)}>Изменить</button>
            <button className="danger-link" onClick={() => setConfirm(key)} aria-label={`Удалить ${r.title}`}>Удалить</button>
          </>}
        </span>
      </div> })}
    </div>}
  </section>
}

// ---------- Форма героя или произведения ----------

type Draft = { title: string; typeId: number | null; text: string; links: number[]; images: EditorImage[] }

function FolkloreForm({ kind, id, onCancel, onSaved }: {
  kind: FolkloreKind; id: number | null; onCancel: () => void; onSaved: (text: string) => void
}) {
  const isHero = kind === 'hero'
  const [draft, setDraft] = useState<Draft | null>(id === null ? { title: '', typeId: null, text: '', links: [], images: [] } : null)
  const [types, setTypes] = useState<FolkloreType[]>([])
  const [options, setOptions] = useState<EditorFolkloreRow[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    editorFolkloreTypes().then(setTypes).catch(e => setFormError(errorText(e, 'Не удалось загрузить типы.')))
    // Для героя выбираем произведения, для произведения - героев
    editorFolklore().then(rows => setOptions(rows.filter(r => r.kind !== kind))).catch(() => {})
    if (id === null) return
    const load = isHero
      ? editorHero(id).then(h => ({ title: h.name, typeId: h.typeId, text: h.description ?? '', links: h.workIds ?? [], images: h.images ?? [] }))
      : editorWork(id).then(w => ({ title: w.title, typeId: w.typeId, text: w.summary ?? '', links: w.heroIds ?? [], images: w.images ?? [] }))
    load.then(d => setDraft({ ...d, images: d.images.map(m => ({ url: m.url ?? '', caption: m.caption ?? '' })) }))
      .catch(e => setFormError(errorText(e, 'Не удалось загрузить материал.')))
  }, [kind, id, isHero])

  if (!draft) return <section><button className="back" onClick={onCancel}>← К списку фольклора</button>
    {formError ? <p className="editor-error" role="alert">{formError}</p> : <p className="editor-empty" role="status">Загрузка…</p>}</section>

  // Ошибка поля исчезает, как только его исправляют
  const errorKey: Partial<Record<keyof Draft, string>> = { title: isHero ? 'name' : 'title', typeId: 'typeId', text: isHero ? 'description' : 'summary' }
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => {
    setDraft({ ...draft, [key]: value })
    const ek = errorKey[key]
    if (ek && errors[ek]) setErrors(({ [ek]: _, ...rest }) => rest)
  }
  const toggle = (linkId: number) => set('links', draft.links.includes(linkId) ? draft.links.filter(x => x !== linkId) : [...draft.links, linkId])

  // Ошибки сервера приходят по именам полей формы героя/произведения
  const titleKey = isHero ? 'name' : 'title'
  const textKey = isHero ? 'description' : 'summary'

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    const e: Record<string, string> = { ...imageErrors(draft.images) }
    if (!draft.title.trim()) e[titleKey] = isHero ? 'Введите имя героя' : 'Введите название'
    if (draft.typeId === null) e.typeId = 'Выберите тип'
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) { setFormError('Проверьте выделенные поля.'); return }
    setSaving(true)
    try {
      let name: string
      if (isHero) {
        const body = { name: draft.title, typeId: draft.typeId, description: draft.text, workIds: draft.links, images: draft.images }
        name = (id === null ? await createHero(body) : await updateHero(id, body)).name
      } else {
        const body = { title: draft.title, typeId: draft.typeId, summary: draft.text, heroIds: draft.links, images: draft.images }
        name = (id === null ? await createWork(body) : await updateWork(id, body)).title
      }
      onSaved(id === null ? `«${name}» ${isHero ? 'добавлен' : 'добавлено'}` : `Изменения в «${name}» сохранены`)
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) {
        setErrors(err.fieldErrors)
        setFormError('Проверьте выделенные поля.')
      } else setFormError(errorText(err, 'Не удалось сохранить. Попробуйте ещё раз.'))
    } finally { setSaving(false) }
  }

  return <form className="rite-form" onSubmit={submit} noValidate>
    <div className="form-top">
      <button type="button" className="back" onClick={onCancel}>← К списку фольклора</button>
      <h2>{id === null ? (isHero ? 'Новый герой' : 'Новое произведение') : draft.title || 'Без названия'}</h2>
    </div>
    <div className="form-grid">
      <div className="form-main">
        <Field label={isHero ? 'Имя героя *' : 'Название *'} error={errors[titleKey]}>
          <input value={draft.title} onChange={e => set('title', e.target.value)} maxLength={200} placeholder={isHero ? 'Например, Манас' : 'Например, Манас (эпос)'} />
        </Field>
        <Field label={isHero ? 'Описание' : 'Краткое содержание'} error={errors[textKey]} hint="Показывается в каталоге и на странице материала.">
          <textarea rows={8} value={draft.text} onChange={e => set('text', e.target.value)} />
        </Field>
        <fieldset className="steps links">
          <legend>{isHero ? 'Произведения с этим героем' : 'Герои произведения'} <span>{draft.links.length}</span></legend>
          {options.length === 0
            ? <p className="hint-text">{isHero ? 'Произведений пока нет — сначала добавьте произведение.' : 'Героев пока нет — сначала добавьте героя.'}</p>
            : <div className="link-chips">{options.map(o => <label key={o.id} className={draft.links.includes(o.id) ? 'chip on' : 'chip'}>
              <input type="checkbox" checked={draft.links.includes(o.id)} onChange={() => toggle(o.id)} />{o.title}<small>{o.type}</small>
            </label>)}</div>}
          {(errors.workIds || errors.heroIds) && <small className="err">{errors.workIds || errors.heroIds}</small>}
        </fieldset>
      </div>
      <aside className="form-side">
        <div className="side-card">
          <span className="side-label">Вид</span>
          <p className="kind-line"><em className={`badge kind-${kind}`}>{KIND[kind]}</em></p>
          <Field label="Тип *" error={errors.typeId}>
            <select value={draft.typeId ?? ''} onChange={e => set('typeId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">— выберите —</option>
              {types.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </Field>
        </div>
        <ImagesField images={draft.images} onChange={images => set('images', images)} errors={errors} />
      </aside>
    </div>
    <div className="form-actions">
      {formError && <p className="editor-error" role="alert">{formError}</p>}
      <button type="button" className="btn ghost" onClick={onCancel}>Отмена</button>
      <button type="submit" className="btn" disabled={saving}>{saving ? 'Сохранение…' : id === null ? 'Добавить' : 'Сохранить изменения'}</button>
    </div>
  </form>
}
