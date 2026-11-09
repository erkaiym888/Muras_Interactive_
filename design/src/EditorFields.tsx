import type { ReactNode } from 'react'
import { ApiError } from './api'
import type { EditorImage } from './api'

/** Общие части форм редактора контента (обряды и фольклор). */

export function errorText(e: unknown, fallback: string) {
  if (e instanceof ApiError) {
    if (e.status === 401) return 'Сессия закончилась. Войдите снова.'
    if (e.status === 403) return 'Недостаточно прав для этого действия.'
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
    <span className="side-label">Изображения <em>{images.length}</em></span>
    {images.map((m, i) => <div className="image-row" key={i}>
      {m.url.trim() ? <img src={m.url} alt={m.caption || 'Предпросмотр'} /> : <i className="thumb-empty" aria-hidden="true">✦</i>}
      <div>
        <input value={m.url} onChange={e => setImage(i, { url: e.target.value })} placeholder="https://… или /images/…" aria-label={`Адрес изображения ${i + 1}`} className={errors[`images[${i}].url`] ? 'invalid' : ''} />
        <input value={m.caption} onChange={e => setImage(i, { caption: e.target.value })} placeholder="Подпись (alt-текст)" aria-label={`Подпись изображения ${i + 1}`} />
        {errors[`images[${i}].url`] && <small className="err">{errors[`images[${i}].url`]}</small>}
      </div>
      <button type="button" onClick={() => onChange(images.filter((_, j) => j !== i))} aria-label={`Удалить изображение ${i + 1}`}>×</button>
    </div>)}
    <button type="button" className="add" onClick={() => onChange([...images, { url: '', caption: '' }])}>+ Добавить изображение</button>
    <small className="hint">Первое изображение — обложка в каталоге.</small>
  </div>
}

/** Проверка изображений перед отправкой. */
export function imageErrors(images: EditorImage[]) {
  const e: Record<string, string> = {}
  images.forEach((m, i) => { if (!m.url.trim()) e[`images[${i}].url`] = 'Укажите адрес или удалите изображение' })
  return e
}
