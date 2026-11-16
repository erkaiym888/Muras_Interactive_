import { useEffect, useState } from 'react'
import { fetchAttempt, fetchHistory } from './api'
import type { AttemptResult, HistoryItem, Progress, QuizSummary } from './api'
import { QuizResultView, TOPIC, pointsWord } from './QuizResult'

/** История прохождений и прогресс (FR-5.4). */

type Status = 'loading' | 'ready' | 'error'
const note = { textAlign: 'center' as const, color: '#716861' }

export const formatDateTime = (iso: string) => new Date(iso).toLocaleString('ru-RU', {
  day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Bishkek',
})

export function attemptsWord(n: number) {
  const last = n % 10, tail = n % 100
  if (last === 1 && tail !== 11) return 'прохождение'
  if (last >= 2 && last <= 4 && (tail < 12 || tail > 14)) return 'прохождения'
  return 'прохождений'
}

/** Общие цифры и прогресс по темам. */
export function ProgressPanel({ progress }: { progress: Progress }) {
  const pct = progress.quizzesTotal ? Math.round(100 * progress.quizzesCompleted / progress.quizzesTotal) : 0
  return <section className="progress-panel" aria-label="Ваш прогресс">
    <div className="pp-stats">
      <div><b>{progress.quizzesCompleted}<small> / {progress.quizzesTotal}</small></b><span>викторин пройдено</span></div>
      <div><b>{progress.averageBestScore}%</b><span>средний лучший результат</span></div>
      <div><b>{progress.correctAnswers}<small> / {progress.answeredQuestions}</small></b><span>верных ответов</span></div>
      <div><b>{progress.totalPoints}</b><span>{pointsWord(progress.totalPoints)} всего</span></div>
    </div>
    <div className="pp-bar" role="progressbar" aria-label="Пройдено викторин" aria-valuemin={0} aria-valuemax={100} aria-valuenow={pct}><i style={{ width: `${pct}%` }} /></div>
    <ul className="pp-topics">{progress.topics.map(t => {
      const w = t.quizzesTotal ? Math.round(100 * t.quizzesCompleted / t.quizzesTotal) : 0
      return <li key={t.topic}>
        <span className="pp-name">{TOPIC[t.topic] ?? t.topic}</span>
        <span className="pp-track" aria-hidden="true"><i style={{ width: `${w}%` }} /></span>
        <span className="pp-num">{t.quizzesCompleted} из {t.quizzesTotal}{t.quizzesCompleted > 0 && <small> · лучший в среднем {t.averageBestScore}%</small>}</span>
      </li>
    })}</ul>
  </section>
}

/** Список прохождений; по клику открывается разбор. */
export function HistoryList({ items, onOpen }: { items: HistoryItem[]; onOpen: (attemptId: number) => void }) {
  return <ul className="history-list">{items.map(h => <li key={h.attemptId}>
    <button onClick={() => onOpen(h.attemptId)} aria-label={`Разбор: ${h.quizTitle}, ${formatDateTime(h.completedAt)}`}>
      <span className={`hl-score ${h.score >= 80 ? 'good' : h.score >= 50 ? 'mid' : 'low'}`}>{h.score}%</span>
      <span className="hl-main"><b>{h.quizTitle}</b><small>{TOPIC[h.topic] ?? h.topic} · {h.correctCount} из {h.questionCount} верно{h.repeatAttempt ? ' · повторно' : ''}</small></span>
      <span className="hl-pts">+{h.pointsEarned}</span>
      <span className="hl-date">{formatDateTime(h.completedAt)}</span>
      <span className="hl-go" aria-hidden="true">→</span>
    </button>
  </li>)}</ul>
}

/** Разбор одного прохождения из истории. */
export function AttemptDetail({ attemptId, onBack }: { attemptId: number; onBack: () => void }) {
  const [result, setResult] = useState<AttemptResult | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  useEffect(() => {
    let alive = true
    setStatus('loading')
    fetchAttempt(attemptId).then(r => { if (alive) { setResult(r); setStatus('ready') } }).catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [attemptId])
  return <div className="attempt-detail">
    <button className="back" onClick={onBack}>← К истории прохождений</button>
    {status === 'loading' && <p role="status" style={note}>Загрузка…</p>}
    {status === 'error' && <p role="alert" style={note}>Не удалось загрузить прохождение.</p>}
    {result && <>
      <p className="ad-date">{formatDateTime(result.completedAt)}</p>
      <QuizResultView result={result} compact />
    </>}
  </div>
}

/** Страница истории: фильтр по викторине, список, разбор выбранного прохождения. */
export function QuizHistoryPage({ quizzes, onBack }: { quizzes: QuizSummary[]; onBack: () => void }) {
  const [items, setItems] = useState<HistoryItem[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [quizId, setQuizId] = useState<number | ''>('')
  const [open, setOpen] = useState<number | null>(null)

  useEffect(() => {
    let alive = true
    setStatus('loading')
    fetchHistory(quizId === '' ? undefined : quizId)
      .then(h => { if (alive) { setItems(h); setStatus('ready') } })
      .catch(() => alive && setStatus('error'))
    return () => { alive = false }
  }, [quizId])

  if (open !== null) return <main className="catalog"><section className="quiz-result"><AttemptDetail attemptId={open} onBack={() => setOpen(null)} /></section></main>

  return <main className="catalog">
    <button className="back" onClick={onBack}>← К списку викторин</button>
    <div className="page-title"><p className="kicker">ВИКТОРИНЫ · ИСТОРИЯ</p><h1>История <i>прохождений</i></h1><p>Все ваши попытки: результат, очки и разбор ответов с пояснениями.</p></div>
    <div className="history-tools">
      <label>Викторина
        <select value={quizId} onChange={e => setQuizId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">Все викторины</option>
          {quizzes.map(q => <option key={q.id} value={q.id}>{q.title}</option>)}
        </select>
      </label>
      {status === 'ready' && <span role="status">Прохождений: {items.length}</span>}
    </div>
    {status === 'loading' && <p role="status" style={note}>Загрузка…</p>}
    {status === 'error' && <p role="alert" style={note}>Не удалось загрузить историю.</p>}
    {status === 'ready' && items.length === 0 && <p style={note}>Прохождений пока нет.</p>}
    {status === 'ready' && items.length > 0 && <HistoryList items={items} onOpen={setOpen} />}
  </main>
}
