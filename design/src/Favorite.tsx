import { t } from './i18n'
import { useEffect, useState } from 'react'
import './features.css'
import { addFavorite, fetchFavoriteStatus, removeFavorite } from './api'
import type { FavoriteType, User } from './api'

/** Кнопка «В избранное». Гостю не показывается: избранное доступно после входа. */
export function FavoriteButton({ user, itemType, itemId }: { user: User | null; itemType: FavoriteType; itemId: number }) {
  const [favorite, setFavorite] = useState<boolean | null>(null)
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(false)
  const userId = user?.id

  useEffect(() => {
    setFavorite(null)
    if (userId === undefined) return
    let alive = true
    fetchFavoriteStatus(itemType, itemId)
      .then(s => { if (alive) setFavorite(s.favorite) })
      .catch(() => { if (alive) setFavorite(null) })
    return () => { alive = false }
  }, [userId, itemType, itemId])

  if (!user || favorite === null) return null

  const toggle = async () => {
    setBusy(true)
    setFailed(false)
    try {
      if (favorite) await removeFavorite(itemType, itemId)
      else await addFavorite(itemType, itemId)
      setFavorite(!favorite)
    } catch {
      setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  return <div className="fav-row">
    <button className="fav-btn" aria-pressed={favorite} disabled={busy} onClick={toggle}>
      <span aria-hidden="true">{favorite ? '★' : '☆'}</span>{favorite ? t('favorite.s1', "В избранном") : t('favorite.s2', "В избранное")}
    </button>
    {failed && <span role="alert" style={{ marginLeft: 12, color: '#a42b2b', fontSize: 13 }}>{t('favorite.s3', "Не удалось сохранить, попробуйте ещё раз")}</span>}
  </div>
}
