import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { ApiError, fetchMe, login, register, requestPasswordReset, resetPassword } from './api'
import type { User } from './api'
import { photos } from './data'

type Mode = 'login' | 'register' | 'recover' | 'reset'
type AuthPage = 'home' | 'login' | 'register' | 'recover'

const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6" /></svg>
const Eye = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></svg>

function Logo({ onClick }: { onClick: () => void }) {
  return <button className="logo" onClick={onClick} aria-label="На главную"><span className="logo-mark">М</span><span><b>MURAS</b><small>INTERACTIVE</small></span></button>
}

function Button({ children, onClick, type = 'button', disabled }: { children: ReactNode; onClick?: () => void; type?: 'button' | 'submit'; disabled?: boolean }) {
  return <button type={type} onClick={onClick} disabled={disabled} className="btn">{children}<Arrow /></button>
}

const titles: Record<Mode, [string, string]> = {
  login: ['С возвращением', 'Войдите, чтобы продолжить путешествие по наследию.'],
  register: ['Создайте аккаунт', 'Сохраняйте материалы и продолжайте исследование.'],
  recover: ['Восстановление доступа', 'Мы отправим инструкции на вашу электронную почту.'],
  reset: ['Новый пароль', 'Придумайте новый пароль для входа.'],
}

const MIN_PASSWORD = 8

/** Вход, регистрация, восстановление и смена пароля. Форма та же, что в дизайне, но данные уходят на сервер. */
export function Auth({ mode, go, onUser, resetToken }: {
  mode: Mode
  go: (p: AuthPage) => void
  onUser: (user: User | null) => void
  resetToken: string
}) {
  const [show, setShow] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    const val = (name: string) => String(f.get(name) ?? '')
    const email = val('email').trim().toLowerCase()
    const password = val('password')
    const displayName = val('displayName').trim()

    const err: Record<string, string> = {}
    if (mode !== 'reset' && !email.includes('@')) err.email = 'Введите корректный адрес электронной почты'
    if (mode === 'register' && !displayName) err.displayName = 'Введите имя'
    if (mode === 'login' && !password) err.password = 'Введите пароль'
    if ((mode === 'register' || mode === 'reset') && password.length < MIN_PASSWORD) err.password = `Пароль должен содержать не менее ${MIN_PASSWORD} символов`
    if ((mode === 'register' || mode === 'reset') && password !== val('confirm')) err.confirm = 'Пароли не совпадают'
    setErrors(err)
    setFormError('')
    if (Object.keys(err).length) return

    setBusy(true)
    try {
      if (mode === 'login') {
        await login(email, password)
        onUser(await fetchMe())
        go('home')
      } else if (mode === 'register') {
        await register(displayName, email, password)
        await login(email, password)
        onUser(await fetchMe())
        go('home')
      } else if (mode === 'recover') {
        await requestPasswordReset(email)
        setDone(true)
      } else {
        await resetPassword(resetToken, password)
        window.history.replaceState(null, '', window.location.pathname)
        setDone(true)
      }
    } catch (error) {
      if (error instanceof ApiError) {
        if (mode === 'login' && error.status === 401) setFormError('Неверная почта или пароль')
        else if (Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors)
        else setFormError(error.message || 'Что-то пошло не так. Попробуйте ещё раз.')
      } else {
        setFormError('Не удалось связаться с сервером. Проверьте, что он запущен.')
      }
    } finally {
      setBusy(false)
    }
  }

  const success = mode === 'reset'
    ? { title: 'Пароль изменён', text: 'Теперь можно войти с новым паролем.', button: 'Перейти ко входу' }
    : { title: 'Письмо отправлено', text: 'Если аккаунт с таким адресом существует, инструкции уже в пути.', button: 'Вернуться ко входу' }

  const newPassword = mode === 'register' || mode === 'reset'

  return <main className="auth">
    <div className="auth-image" style={{ backgroundImage: `linear-gradient(0deg,rgba(50,20,22,.72),rgba(50,20,22,.05)),url(${photos.hero})` }}>
      <Logo onClick={() => go('home')} />
      <blockquote>«То, что мы сохраняем сегодня, становится памятью <i>завтра</i>»</blockquote>
    </div>
    <section className="auth-form">
      <button className="back" onClick={() => go('home')}>← На главную</button>
      <div>
        <p className="kicker">MURAS INTERACTIVE</p>
        <h1>{titles[mode][0]}</h1>
        <p>{titles[mode][1]}</p>
        {done ? <div className="success"><b>{success.title}</b><p>{success.text}</p><Button onClick={() => go('login')}>{success.button}</Button></div> : <form onSubmit={submit} noValidate>
          {mode === 'register' && <label>Отображаемое имя<input name="displayName" autoComplete="nickname" className={errors.displayName ? 'invalid' : ''} placeholder="Как к вам обращаться?" />{errors.displayName && <small>{errors.displayName}</small>}</label>}
          {mode !== 'reset' && <label>Электронная почта<input name="email" type="email" autoComplete="email" className={errors.email ? 'invalid' : ''} placeholder="name@example.com" />{errors.email && <small>{errors.email}</small>}</label>}
          {mode !== 'recover' && <label>{mode === 'reset' ? 'Новый пароль' : 'Пароль'}<div className="password"><input name="password" type={show ? 'text' : 'password'} autoComplete={newPassword ? 'new-password' : 'current-password'} className={errors.password ? 'invalid' : ''} placeholder={newPassword ? `Не менее ${MIN_PASSWORD} символов` : 'Ваш пароль'} /><button type="button" onClick={() => setShow(!show)} aria-label="Показать пароль"><Eye /></button></div>{errors.password && <small>{errors.password}</small>}</label>}
          {newPassword && <label>Подтвердите пароль<input name="confirm" type={show ? 'text' : 'password'} autoComplete="new-password" className={errors.confirm ? 'invalid' : ''} placeholder="Повторите пароль" />{errors.confirm && <small>{errors.confirm}</small>}</label>}
          {mode === 'login' && <button type="button" className="forgot" onClick={() => go('recover')}>Забыли пароль?</button>}
          {formError && <p role="alert" style={{ color: '#a42b2b', margin: '0 0 16px', fontSize: 13 }}>{formError}</p>}
          <Button type="submit" disabled={busy}>{mode === 'login' ? 'Войти' : mode === 'register' ? 'Зарегистрироваться' : mode === 'recover' ? 'Отправить инструкции' : 'Сохранить пароль'}</Button>
        </form>}
        {!done && mode !== 'reset' && <p className="switch">{mode === 'login' ? 'Ещё нет аккаунта?' : mode === 'register' ? 'Уже есть аккаунт?' : 'Вспомнили пароль?'} <button onClick={() => go(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Зарегистрироваться' : 'Войти'}</button></p>}
      </div>
    </section>
  </main>
}
