import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { saveTranslation, translationForm, translationItems, translationMeta } from './api'
import type { TranslationForm, TranslationItem, TranslationMeta } from './api'
import { errorText } from './EditorFields'

/**
 * Переводы материалов (FR-8.1): русский оригинал слева, перевод справа.
 * Виды материалов и поля приходят с сервера, поэтому для нового языка ничего в коде менять не нужно.
 */
export function Translations({ notify }: { notify: (t: string) => void }) {
  const [meta, setMeta] = useState<TranslationMeta | null>(null)
  const [lang, setLang] = useState('')
  const [type, setType] = useState('')
  const [items, setItems] = useState<TranslationItem[] | null>(null)
  const [openId, setOpenId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    translationMeta().then(m => {
      setMeta(m)
      setLang(m.languages[0]?.code ?? '')
      setType(m.kinds[0]?.type ?? '')
    }).catch(e => setError(errorText(e, 'Не удалось загрузить переводы.')))
  }, [])

  const loadItems = () => {
    if (!lang || !type) return
    setItems(null)
    translationItems(type, lang).then(setItems).catch(e => setError(errorText(e, 'Не удалось загрузить материалы.')))
  }
  useEffect(loadItems, [lang, type])

  if (!meta) return <section>{error ? <p className="editor-error" role="alert">{error}</p> : <p className="editor-empty" role="status">Загрузка…</p>}</section>
  if (meta.languages.length === 0) return <section><p className="editor-empty">Других языков, кроме основного, пока нет.</p></section>

  const language = meta.languages.find(l => l.code === lang)
  if (openId !== null) {
    return <TranslationEditor type={type} id={openId} lang={lang} langName={language?.name ?? lang}
      onClose={() => { setOpenId(null); loadItems() }} notify={notify} />
  }

  return <section>
    <div className="editor-toolbar">
      <select value={lang} onChange={e => setLang(e.target.value)} aria-label="Язык перевода">
        {meta.languages.map(l => <option key={l.code} value={l.code}>{l.name} ({l.nativeName}){l.enabled ? '' : ' — выключен'}</option>)}
      </select>
      <select value={type} onChange={e => setType(e.target.value)} aria-label="Вид материала">
        {meta.kinds.map(k => <option key={k.type} value={k.type}>{k.title}</option>)}
      </select>
    </div>
    {language && !language.enabled && <p className="tr-note">Язык ещё выключен: переводы можно готовить заранее, посетители увидят их после включения в разделе «Пользователи → Языки».</p>}
    {error && <p className="editor-error" role="alert">{error}</p>}
    {items === null && !error && <p className="editor-empty" role="status">Загрузка…</p>}
    {items && items.length === 0 && <p className="editor-empty">Материалов этого вида пока нет.</p>}
    {items && items.length > 0 && <div className="editor-table tr-table" role="table" aria-label="Материалы для перевода">
      <div className="row head" role="row"><span role="columnheader">Материал</span><span role="columnheader">Готовность</span><span role="columnheader"><span className="sr-only">Действия</span></span></div>
      {items.map(i => {
        const pct = i.total ? Math.round(i.filled / i.total * 100) : 0
        return <div className="row" role="row" key={i.id}>
          <span role="cell" className="title-cell"><span><b>{i.title}</b><small>{i.filled} из {i.total} полей</small></span></span>
          <span role="cell"><span className={pct === 100 ? 'tr-bar done' : 'tr-bar'} role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} aria-label={`Переведено ${pct}%`}><i style={{ width: `${pct}%` }} /></span><small className="tr-pct">{pct}%</small></span>
          <span role="cell" className="row-actions"><button onClick={() => setOpenId(i.id)}>{i.filled ? 'Изменить перевод' : 'Перевести'}</button></span>
        </div>
      })}
    </div>}
  </section>
}

function TranslationEditor({ type, id, lang, langName, onClose, notify }: {
  type: string; id: number; lang: string; langName: string; onClose: () => void; notify: (t: string) => void
}) {
  const [form, setForm] = useState<TranslationForm | null>(null)
  const [values, setValues] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    translationForm(type, id, lang).then(f => {
      setForm(f)
      setValues(Object.fromEntries(f.fields.map(x => [x.field, x.translation ?? ''])))
    }).catch(e => setError(errorText(e, 'Не удалось загрузить материал.')))
  }, [type, id, lang])

  if (!form) return <section><button className="back" onClick={onClose}>← К списку переводов</button>
    {error ? <p className="editor-error" role="alert">{error}</p> : <p className="editor-empty" role="status">Загрузка…</p>}</section>

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    setSaving(true); setError('')
    try {
      await saveTranslation(type, id, lang, values)
      notify(`Перевод «${form.title}» сохранён`)
      onClose()
    } catch (e) { setError(errorText(e, 'Не удалось сохранить перевод.')) } finally { setSaving(false) }
  }

  return <form className="rite-form tr-form" onSubmit={submit} noValidate>
    <div className="form-top">
      <button type="button" className="back" onClick={onClose}>← К списку переводов</button>
      <h2>{form.title} <small>· {langName}</small></h2>
    </div>
    <div className="tr-grid">
      <div className="tr-head"><span>Оригинал · русский</span><span>Перевод · {langName.toLowerCase()}</span></div>
      {form.fields.map(f => <div className="tr-pair" key={f.field}>
        <div className="tr-orig"><span className="tr-label">{f.label}</span><p>{f.original}</p></div>
        <label className="ed-field"><span className="sr-only">{f.label}: перевод</span>
          {f.multiline
            ? <textarea rows={Math.min(10, Math.max(3, Math.ceil(f.original.length / 70)))} value={values[f.field] ?? ''} lang={lang}
              onChange={e => setValues({ ...values, [f.field]: e.target.value })} placeholder="Перевода нет — показывается оригинал" />
            : <input value={values[f.field] ?? ''} lang={lang} onChange={e => setValues({ ...values, [f.field]: e.target.value })} placeholder="Перевода нет — показывается оригинал" />}
        </label>
      </div>)}
    </div>
    <div className="form-actions">
      {error && <p className="editor-error" role="alert">{error}</p>}
      <button type="button" className="btn ghost" onClick={onClose}>Отмена</button>
      <button type="submit" className="btn" disabled={saving}>{saving ? 'Сохранение…' : 'Сохранить перевод'}</button>
    </div>
  </form>
}
