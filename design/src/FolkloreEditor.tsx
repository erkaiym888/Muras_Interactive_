import { lrec, t, tf } from './i18n'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  ApiError, createHero, createWork, deleteHero, deleteWork, editorFolklore, editorFolkloreTypes, editorHero,
  editorWork, setFolkloreStatus, updateHero, updateWork,
} from './api'
import type { EditorFolkloreRow, EditorImage, FolkloreKind, FolkloreType, RiteStatus } from './api'
import { Field, ImagesField, STATUS, STATUSES, StatusOptions, StatusSelect, errorText, imageErrors } from './EditorFields'
import type { FolkloreRef } from './Folklore'

/** Управление фольклором (FR-7.1): герои и произведения, связи между ними, иллюстрации. */

const KIND: Record<FolkloreKind, string> = lrec('kind', { hero: 'Герой', work: 'Произведение' })

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
  const [status, setStatus] = useState<RiteStatus | 'ALL'>('ALL')
  const [confirm, setConfirm] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  const load = () => editorFolklore().then(setRows).catch(e => setError(errorText(e, t('folkloreEditor.s1', "Не удалось загрузить фольклор."))))
  useEffect(() => { load() }, [])

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (rows ?? []).filter(r => (kind === 'ALL' || r.kind === kind) && (status === 'ALL' || r.status === status) &&
      (r.title.toLowerCase().includes(q) || r.type.toLowerCase().includes(q)))
  }, [rows, search, kind, status])

  const changeStatus = async (r: EditorFolkloreRow, next: RiteStatus) => {
    const key = `${r.kind}-${r.id}`
    setBusy(key); setError('')
    try {
      const updated = await setFolkloreStatus(r.kind, r.id, next)
      setRows(rs => (rs ?? []).map(x => x.kind === r.kind && x.id === r.id ? { ...x, ...updated } : x))
      notify(`«${r.title}»: ${STATUS[next].toLowerCase()}`)
    } catch (e) { setError(errorText(e, t('folkloreEditor.s2', "Не удалось сменить статус."))) } finally { setBusy(null) }
  }

  const remove = async (r: EditorFolkloreRow) => {
    try {
      await (r.kind === 'hero' ? deleteHero(r.id) : deleteWork(r.id))
      setConfirm(null)
      notify(`«${r.title}» ${r.kind === 'hero' ? t('folkloreEditor.s3', "удалён") : t('folkloreEditor.s4', "удалено")}`)
      load()
    } catch (e) { setError(errorText(e, t('folkloreEditor.s5', "Не удалось удалить."))) }
  }

  const count = (k: FolkloreKind) => (rows ?? []).filter(r => r.kind === k).length
  const countStatus = (s: RiteStatus) => (rows ?? []).filter(r => r.status === s).length

  return <section>
    <div className="editor-toolbar">
      <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder={t('folkloreEditor.s6', "Найти по названию или типу")} aria-label={t('folkloreEditor.s7', "Поиск")} />
      <select value={kind} onChange={e => setKind(e.target.value as FolkloreKind | 'ALL')} aria-label={t('folkloreEditor.s8', "Вид")}>
        <option value="ALL">{t('folkloreEditor.s9', "Всё ·")}{' '}{rows?.length ?? 0}</option>
        <option value="hero">{t('folkloreEditor.s10', "Герои ·")}{' '}{count('hero')}</option>
        <option value="work">{t('folkloreEditor.s11', "Произведения ·")}{' '}{count('work')}</option>
      </select>
      <select value={status} onChange={e => setStatus(e.target.value as RiteStatus | 'ALL')} aria-label={t('folkloreEditor.s12', "Статус")}>
        <option value="ALL">{t('folkloreEditor.s13', "Все статусы")}</option>
        {STATUSES.map(s => <option key={s} value={s}>{STATUS[s]} · {countStatus(s)}</option>)}
      </select>
      <button className="btn ghost" onClick={() => onEdit('hero', null)}>{t('folkloreEditor.s14', "+ Герой")}</button>
      <button className="btn" onClick={() => onEdit('work', null)}>{t('folkloreEditor.s15', "+ Произведение")}</button>
    </div>
    {error && <p className="editor-error" role="alert">{error}</p>}
    {rows === null && !error && <p className="editor-empty" role="status">{t('folkloreEditor.s16', "Загрузка…")}</p>}
    {rows !== null && shown.length === 0 && <p className="editor-empty">{rows.length === 0 ? t('folkloreEditor.s17', "Пока пусто. Добавьте героя или произведение.") : t('folkloreEditor.s18', "Ничего не найдено.")}</p>}
    {shown.length > 0 && <div className="editor-table folk" role="table" aria-label={t('folkloreEditor.s19', "Фольклор")}>
      <div className="row head" role="row"><span role="columnheader">{t('folkloreEditor.s20', "Материал")}</span><span role="columnheader">{t('folkloreEditor.s8', "Вид")}</span><span role="columnheader">{t('folkloreEditor.s12', "Статус")}</span><span role="columnheader">{t('folkloreEditor.s21', "Связей")}</span><span role="columnheader"><span className="sr-only">{t('folkloreEditor.s22', "Действия")}</span></span></div>
      {shown.map(r => { const key = `${r.kind}-${r.id}`; return <div className="row" role="row" key={key}>
        <span role="cell" className="title-cell">
          {r.imageUrl ? <img src={r.imageUrl} alt="" /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
          <span><b>{r.title}</b><small>{r.type}</small></span>
        </span>
        <span role="cell"><em className={`badge kind-${r.kind}`}>{KIND[r.kind]}</em></span>
        <span role="cell"><StatusSelect value={r.status} title={r.title} busy={busy === key} onChange={s => changeStatus(r, s)} /></span>
        <span role="cell" className="num" title={r.kind === 'hero' ? t('folkloreEditor.s23', "Произведений с этим героем") : t('folkloreEditor.s24', "Героев в произведении")}>{r.linkCount}</span>
        <span role="cell" className="row-actions">
          {confirm === key ? <>
            <span className="confirm-text">{t('folkloreEditor.s25', "Удалить навсегда?")}</span>
            <button className="danger" onClick={() => remove(r)}>{t('folkloreEditor.s26', "Да, удалить")}</button>
            <button onClick={() => setConfirm(null)}>{t('folkloreEditor.s27', "Отмена")}</button>
          </> : <>
            {r.status === 'PUBLISHED' && <button onClick={() => openFolklore({ kind: r.kind, id: r.id })}>{t('folkloreEditor.s28', "На сайте")}</button>}
            <button onClick={() => onEdit(r.kind, r.id)}>{t('folkloreEditor.s29', "Изменить")}</button>
            <button className="danger-link" onClick={() => setConfirm(key)} aria-label={tf('folkloreEditor.s30', "Удалить {0}", r.title)}>{t('folkloreEditor.s31', "Удалить")}</button>
          </>}
        </span>
      </div> })}
    </div>}
  </section>
}

// ---------- Форма героя или произведения ----------

type Draft = { title: string; typeId: number | null; text: string; status: RiteStatus; links: number[]; images: EditorImage[] }

function FolkloreForm({ kind, id, onCancel, onSaved }: {
  kind: FolkloreKind; id: number | null; onCancel: () => void; onSaved: (text: string) => void
}) {
  const isHero = kind === 'hero'
  const [draft, setDraft] = useState<Draft | null>(id === null ? { title: '', typeId: null, text: '', status: 'DRAFT', links: [], images: [] } : null)
  const [types, setTypes] = useState<FolkloreType[]>([])
  const [options, setOptions] = useState<EditorFolkloreRow[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    editorFolkloreTypes().then(setTypes).catch(e => setFormError(errorText(e, t('folkloreEditor.s32', "Не удалось загрузить типы."))))
    // Для героя выбираем произведения, для произведения - героев
    editorFolklore().then(rows => setOptions(rows.filter(r => r.kind !== kind))).catch(() => {})
    if (id === null) return
    const load = isHero
      ? editorHero(id).then(h => ({ title: h.name, typeId: h.typeId, text: h.description ?? '', status: h.status ?? 'PUBLISHED', links: h.workIds ?? [], images: h.images ?? [] }))
      : editorWork(id).then(w => ({ title: w.title, typeId: w.typeId, text: w.summary ?? '', status: w.status ?? 'PUBLISHED', links: w.heroIds ?? [], images: w.images ?? [] }))
    load.then(d => setDraft({ ...d, images: d.images.map(m => ({ url: m.url ?? '', caption: m.caption ?? '' })) }))
      .catch(e => setFormError(errorText(e, t('folkloreEditor.s33', "Не удалось загрузить материал."))))
  }, [kind, id, isHero])

  if (!draft) return <section><button className="back" onClick={onCancel}>{t('folkloreEditor.s34', "← К списку фольклора")}</button>
    {formError ? <p className="editor-error" role="alert">{formError}</p> : <p className="editor-empty" role="status">{t('folkloreEditor.s16', "Загрузка…")}</p>}</section>

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
    if (!draft.title.trim()) e[titleKey] = isHero ? t('folkloreEditor.s35', "Введите имя героя") : t('folkloreEditor.s36', "Введите название")
    if (draft.typeId === null) e.typeId = t('folkloreEditor.s37', "Выберите тип")
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) { setFormError(t('folkloreEditor.s38', "Проверьте выделенные поля.")); return }
    setSaving(true)
    try {
      let name: string
      if (isHero) {
        const body = { name: draft.title, typeId: draft.typeId, description: draft.text, status: draft.status, workIds: draft.links, images: draft.images }
        name = (id === null ? await createHero(body) : await updateHero(id, body)).name
      } else {
        const body = { title: draft.title, typeId: draft.typeId, summary: draft.text, status: draft.status, heroIds: draft.links, images: draft.images }
        name = (id === null ? await createWork(body) : await updateWork(id, body)).title
      }
      onSaved(id === null ? `«${name}» ${isHero ? t('folkloreEditor.s39', "добавлен") : t('folkloreEditor.s40', "добавлено")}` : tf('folkloreEditor.s41', "Изменения в «{0}» сохранены", name))
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) {
        setErrors(err.fieldErrors)
        setFormError(t('folkloreEditor.s38', "Проверьте выделенные поля."))
      } else setFormError(errorText(err, t('folkloreEditor.s42', "Не удалось сохранить. Попробуйте ещё раз.")))
    } finally { setSaving(false) }
  }

  return <form className="rite-form" onSubmit={submit} noValidate>
    <div className="form-top">
      <button type="button" className="back" onClick={onCancel}>{t('folkloreEditor.s34', "← К списку фольклора")}</button>
      <h2>{id === null ? (isHero ? t('folkloreEditor.s43', "Новый герой") : t('folkloreEditor.s44', "Новое произведение")) : draft.title || t('folkloreEditor.s45', "Без названия")}</h2>
    </div>
    <div className="form-grid">
      <div className="form-main">
        <Field label={isHero ? t('folkloreEditor.s46', "Имя героя *") : t('folkloreEditor.s47', "Название *")} error={errors[titleKey]}>
          <input value={draft.title} onChange={e => set('title', e.target.value)} maxLength={200} placeholder={isHero ? t('folkloreEditor.s48', "Например, Манас") : t('folkloreEditor.s49', "Например, Манас (эпос)")} />
        </Field>
        <Field label={isHero ? t('folkloreEditor.s50', "Описание") : t('folkloreEditor.s51', "Краткое содержание")} error={errors[textKey]} hint={t('folkloreEditor.s52', "Показывается в каталоге и на странице материала.")}>
          <textarea rows={8} value={draft.text} onChange={e => set('text', e.target.value)} />
        </Field>
        <fieldset className="steps links">
          <legend>{isHero ? t('folkloreEditor.s53', "Произведения с этим героем") : t('folkloreEditor.s54', "Герои произведения")} <span>{draft.links.length}</span></legend>
          {options.length === 0
            ? <p className="hint-text">{isHero ? t('folkloreEditor.s55', "Произведений пока нет — сначала добавьте произведение.") : t('folkloreEditor.s56', "Героев пока нет — сначала добавьте героя.")}</p>
            : <div className="link-chips">{options.map(o => <label key={o.id} className={draft.links.includes(o.id) ? 'chip on' : 'chip'}>
              <input type="checkbox" checked={draft.links.includes(o.id)} onChange={() => toggle(o.id)} />{o.title}<small>{o.type}</small>
            </label>)}</div>}
          {(errors.workIds || errors.heroIds) && <small className="err">{errors.workIds || errors.heroIds}</small>}
        </fieldset>
      </div>
      <aside className="form-side">
        <StatusOptions value={draft.status} onChange={s => set('status', s)} />
        <div className="side-card">
          <span className="side-label">{t('folkloreEditor.s8', "Вид")}</span>
          <p className="kind-line"><em className={`badge kind-${kind}`}>{KIND[kind]}</em></p>
          <Field label={t('folkloreEditor.s57', "Тип *")} error={errors.typeId}>
            <select value={draft.typeId ?? ''} onChange={e => set('typeId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">{t('folkloreEditor.s58', "— выберите —")}</option>
              {types.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </Field>
        </div>
        <ImagesField images={draft.images} onChange={images => set('images', images)} errors={errors} />
      </aside>
    </div>
    <div className="form-actions">
      {formError && <p className="editor-error" role="alert">{formError}</p>}
      <button type="button" className="btn ghost" onClick={onCancel}>{t('folkloreEditor.s27', "Отмена")}</button>
      <button type="submit" className="btn" disabled={saving}>{saving ? t('folkloreEditor.s59', "Сохранение…") : id === null ? t('folkloreEditor.s60', "Добавить") : t('folkloreEditor.s61', "Сохранить изменения")}</button>
    </div>
  </form>
}
