import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, deleteAvatar, updateDisplayName, updateEmail, uploadAvatar } from './api'
import type { User } from './api'

/**
 * Редактирование профиля в личном кабинете (FR-6.2): аватар, никнейм, почта.
 * Каждая часть сохраняется отдельно. Почта меняется только с текущим паролем.
 * Фото уменьшается и обрезается до квадрата 256×256 прямо в браузере - на сервер уходит маленький JPEG.
 */

const AVATAR_SIZE = 256
const MAX_SOURCE_MB = 10

/** Аватар: картинка или первая буква имени. */
export function Avatar({ user, size = 'md' }: { user: Pick<User, 'displayName' | 'avatarUrl'>; size?: 'sm' | 'md' | 'lg' }) {
  const letter = user.displayName.trim().charAt(0).toUpperCase() || '?'
  return user.avatarUrl
    ? <img className={`user-avatar ${size}`} src={user.avatarUrl} alt="" />
    : <span className={`user-avatar ${size} letter`} aria-hidden="true">{letter}</span>
}

/** Квадратная середина картинки, уменьшенная до 256×256, в JPEG. */
function toAvatarDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      const side = Math.min(img.naturalWidth, img.naturalHeight)
      const canvas = document.createElement('canvas')
      canvas.width = canvas.height = AVATAR_SIZE
      const ctx = canvas.getContext('2d')
      if (!ctx) { URL.revokeObjectURL(url); reject(new Error('canvas')); return }
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, AVATAR_SIZE, AVATAR_SIZE)
      ctx.drawImage(img, (img.naturalWidth - side) / 2, (img.naturalHeight - side) / 2, side, side, 0, 0, AVATAR_SIZE, AVATAR_SIZE)
      URL.revokeObjectURL(url)
      resolve(canvas.toDataURL('image/jpeg', 0.86))
    }
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('image')) }
    img.src = url
  })
}

const fieldError = (e: unknown, field: string, fallback: string) =>
  e instanceof ApiError ? (e.fieldErrors?.[field] ?? e.message ?? fallback) : fallback

export function ProfileEdit({ user, onUser, onClose }: { user: User; onUser: (u: User) => void; onClose: () => void }) {
  const [name, setName] = useState(user.displayName)
  const [email, setEmail] = useState(user.email)
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState<'' | 'avatar' | 'name' | 'email'>('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [done, setDone] = useState('')
  const dialog = useRef<HTMLDivElement>(null)
  const file = useRef<HTMLInputElement>(null)

  // Esc закрывает окно, фокус - внутрь окна
  useEffect(() => {
    dialog.current?.querySelector<HTMLElement>('button, input')?.focus()
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  const ok = (u: User, message: string) => { onUser(u); setDone(message); setErrors({}) }

  const pickAvatar = async (f: File | undefined) => {
    if (!f) return
    setDone(''); setErrors({})
    if (!f.type.startsWith('image/')) { setErrors({ avatar: 'Выберите картинку: JPG, PNG или WebP' }); return }
    if (f.size > MAX_SOURCE_MB * 1024 * 1024) { setErrors({ avatar: `Файл больше ${MAX_SOURCE_MB} МБ` }); return }
    setBusy('avatar')
    try {
      const dataUrl = await toAvatarDataUrl(f)
      ok(await uploadAvatar(dataUrl), 'Аватар обновлён')
    } catch (e) {
      setErrors({ avatar: fieldError(e, 'avatar', 'Не удалось загрузить картинку') })
    } finally {
      setBusy('')
      if (file.current) file.current.value = ''
    }
  }

  const removeAvatar = async () => {
    setBusy('avatar'); setDone('')
    try { ok(await deleteAvatar(), 'Аватар убран') }
    catch (e) { setErrors({ avatar: fieldError(e, 'avatar', 'Не удалось убрать аватар') }) }
    finally { setBusy('') }
  }

  const saveName = async (e: FormEvent) => {
    e.preventDefault(); setDone('')
    const value = name.trim()
    if (value.length < 2 || value.length > 40) { setErrors({ displayName: 'Никнейм - от 2 до 40 символов' }); return }
    setBusy('name')
    try { ok(await updateDisplayName(value), 'Никнейм сохранён') }
    catch (err) { setErrors({ displayName: fieldError(err, 'displayName', 'Не удалось сохранить никнейм') }) }
    finally { setBusy('') }
  }

  const saveEmail = async (e: FormEvent) => {
    e.preventDefault(); setDone('')
    const next: Record<string, string> = {}
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) next.email = 'Проверьте адрес почты'
    if (!password) next.currentPassword = 'Введите текущий пароль'
    if (Object.keys(next).length) { setErrors(next); return }
    setBusy('email')
    try {
      const u = await updateEmail(email.trim(), password)
      setPassword('')
      ok(u, 'Почта изменена. Входите теперь с новым адресом')
    } catch (err) {
      const fe = err instanceof ApiError ? err.fieldErrors ?? {} : {}
      setErrors(Object.keys(fe).length ? fe : { email: fieldError(err, 'email', 'Не удалось изменить почту') })
    } finally { setBusy('') }
  }

  const emailChanged = email.trim().toLowerCase() !== user.email

  return <div className="pe-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
    <div className="pe-dialog" role="dialog" aria-modal="true" aria-labelledby="pe-title" ref={dialog}>
      <div className="pe-head">
        <h2 id="pe-title">Редактировать профиль</h2>
        <button className="pe-close" onClick={onClose} aria-label="Закрыть">×</button>
      </div>
      {done && <p className="pe-done" role="status">{done}</p>}

      <section className="pe-section pe-avatar">
        <Avatar user={user} size="lg" />
        <div>
          <h3>Аватар</h3>
          <p className="pe-hint">Квадратная картинка, обрежем по центру. JPG, PNG или WebP.</p>
          <div className="pe-row">
            <button type="button" className="btn" disabled={busy === 'avatar'} onClick={() => file.current?.click()}>
              {busy === 'avatar' ? 'Загрузка…' : user.avatarUrl ? 'Сменить фото' : 'Выбрать фото'}
            </button>
            {user.avatarUrl && <button type="button" className="btn ghost" disabled={busy === 'avatar'} onClick={removeAvatar}>Убрать</button>}
            <input ref={file} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={e => pickAvatar(e.target.files?.[0])} />
          </div>
          {errors.avatar && <p className="pe-error" role="alert">{errors.avatar}</p>}
        </div>
      </section>

      <form className="pe-section" onSubmit={saveName} noValidate>
        <h3>Никнейм</h3>
        <p className="pe-hint">Так вас видят другие: в рейтинге и меню.</p>
        <div className="pe-row">
          <input value={name} onChange={e => setName(e.target.value)} maxLength={40} aria-label="Никнейм"
            aria-invalid={!!errors.displayName} aria-describedby={errors.displayName ? 'pe-name-err' : undefined} />
          <button className="btn" disabled={busy === 'name' || name.trim() === user.displayName}>{busy === 'name' ? 'Сохранение…' : 'Сохранить'}</button>
        </div>
        {errors.displayName && <p id="pe-name-err" className="pe-error" role="alert">{errors.displayName}</p>}
      </form>

      <form className="pe-section" onSubmit={saveEmail} noValidate>
        <h3>Почта</h3>
        <p className="pe-hint">Новый адрес станет логином. Для подтверждения введите текущий пароль.</p>
        <label className="pe-field"><span>Новая почта</span>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="email"
            aria-invalid={!!errors.email} aria-describedby={errors.email ? 'pe-email-err' : undefined} />
        </label>
        {errors.email && <p id="pe-email-err" className="pe-error" role="alert">{errors.email}</p>}
        {emailChanged && <>
          <label className="pe-field"><span>Текущий пароль</span>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password"
              aria-invalid={!!errors.currentPassword} aria-describedby={errors.currentPassword ? 'pe-pass-err' : undefined} />
          </label>
          {errors.currentPassword && <p id="pe-pass-err" className="pe-error" role="alert">{errors.currentPassword}</p>}
        </>}
        <div className="pe-row end">
          <button className="btn" disabled={busy === 'email' || !emailChanged}>{busy === 'email' ? 'Сохранение…' : 'Изменить почту'}</button>
        </div>
      </form>
    </div>
  </div>
}
