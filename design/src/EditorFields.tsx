import { lrec, t, tf } from './i18n'
import type { ReactNode } from 'react'
import { ApiError } from './api'
import type { EditorImage, RiteStatus } from './api'

/** Общие части форм редактора контента (обряды и фольклор). */

/** Статусы материала (FR-7.2) - одинаковые для обрядов и фольклора. */
export const STATUS: Record<RiteStatus, string> = lrec('status', { PUBLISHED: 'Опубликован', DRAFT: 'Черновик', HIDDEN: 'Скрыт' })
export const STATUS_HINT: Record<RiteStatus, string> = lrec('status.hint', {
  PUBLISHED: 'Виден всем посетителям в каталоге.',
  DRAFT: 'Виден только в редакторе, пока вы работаете над текстом.',
  HIDDEN: 'Убран из каталога, но не удалён.',
})
export const STATUSES = Object.keys(STATUS) as RiteStatus[]

/** Выбор статуса в форме: три варианта с пояснением. */
export function StatusOptions({ value, onChange }: { value: RiteStatus; onChange: (s: RiteStatus) => void }) {
  return <div className="side-card">
    <span className="side-label">{t('editorFields.s1', "Статус")}</span>
    {STATUSES.map(s => <label className={value === s ? 'status-opt active' : 'status-opt'} key={s}>
      <input type="radio" name="status" checked={value === s} onChange={() => onChange(s)} />
      <span><b><em className={`badge ${s.toLowerCase()}`}>{STATUS[s]}</em></b><small>{STATUS_HINT[s]}</small></span>
    </label>)}
  </div>
}

/** Быстрая смена статуса прямо в строке списка. */
export function StatusSelect({ value, title, busy, onChange }: {
  value: RiteStatus; title: string; busy?: boolean; onChange: (s: RiteStatus) => void
}) {
  return <select className={`status-select st-${value.toLowerCase()}`} value={value} disabled={busy}
    aria-label={tf('editorFields.s2', "Статус: {0}", title)} title={STATUS_HINT[value]} onChange={e => onChange(e.target.value as RiteStatus)}>
    {STATUSES.map(s => <option key={s} value={s}>{STATUS[s]}</option>)}
  </select>
}

export function errorText(e: unknown, fallback: string) {
  if (e instanceof ApiError) {
    if (e.status === 401) return t('editorFields.s3', "Сессия закончилась. Войдите снова.")
    if (e.status === 403) return t('editorFields.s4', "Недостаточно прав для этого действия.")
    if (e.message) return e.message
  }
  return fallback
}

export function Field({ label, error, children, hint }: { label: string; error?: string; children: ReactNode; hint?: string }) {
  return <label className={error ? 'ed-field invalid' : 'ed-field'}><span>{label}</span>{children}{hint && !error && <small className="hint">{hint}</small>}{error && <small>{error}</small>}</label>
}

/** Список изображений: адрес, подпись, предпросмотр. Ошибки по ключам images[i].url. */
export function ImagesField({ images, onChange, errors }: {
  images: EditorImage[]; onChange: (images: EditorImage[]) => void; errors: Record<string, string>
}) {
  const setImage = (i: number, patch: Partial<EditorImage>) => onChange(images.map((m, j) => j === i ? { ...m, ...patch } : m))
  return <div className="side-card">
    <span className="side-label">{t('editorFields.s5', "Изображения")}{' '}<em>{images.length}</em></span>
    {images.map((m, i) => <div className="image-row" key={i}>
      {m.url.trim() ? <img src={m.url} alt={m.caption || t('editorFields.s6', "Предпросмотр")} /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
      <div>
        <input value={m.url} onChange={e => setImage(i, { url: e.target.value })} placeholder={t('editorFields.s7', "https://… или /images/…")} aria-label={tf('editorFields.s8', "Адрес изображения {0}", i + 1)} className={errors[`images[${i}].url`] ? 'invalid' : ''} />
        <input value={m.caption} onChange={e => setImage(i, { caption: e.target.value })} placeholder={t('editorFields.s9', "Подпись (alt-текст)")} aria-label={tf('editorFields.s10', "Подпись изображения {0}", i + 1)} />
        {errors[`images[${i}].url`] && <small className="err">{errors[`images[${i}].url`]}</small>}
      </div>
      <button type="button" onClick={() => onChange(images.filter((_, j) => j !== i))} aria-label={tf('editorFields.s11', "Удалить изображение {0}", i + 1)}>×</button>
    </div>)}
    <button type="button" className="add" onClick={() => onChange([...images, { url: '', caption: '' }])}>{t('editorFields.s12', "+ Добавить изображение")}</button>
    <small className="hint">{t('editorFields.s13', "Первое изображение — обложка в каталоге.")}</small>
  </div>
}

/** Проверка изображений перед отправкой. */
export function imageErrors(images: EditorImage[]) {
  const e: Record<string, string> = {}
  images.forEach((m, i) => { if (!m.url.trim()) e[`images[${i}].url`] = t('editorFields.s14', "Укажите адрес или удалите изображение") })
  return e
}
