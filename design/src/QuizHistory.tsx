import { locale, t, tf, tp } from './i18n'
import { useEffect, useState } from 'react'
import { fetchAttempt, fetchHistory } from './api'
import type { AttemptResult, HistoryItem, Progress, QuizSummary } from './api'
import { QuizResultView, TOPIC, pointsWord } from './QuizResult'

/** История прохождений и прогресс (FR-5.4). */

type Status = 'loading' | 'ready' | 'error'
const note = { textAlign: 'center' as const, color: '#716861' }

export const formatDateTime = (iso: string) => new Date(iso).toLocaleString(locale(), {
  day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Bishkek',
})

export function attemptsWord(n: number) {
  return tp('plural.attempts', n, 'прохождение', 'прохождения', 'прохождений')
}

/** Общие цифры и прогресс по темам. */
export function ProgressPanel({ progress }: { progress: Progress }) {
  const pct = progress.quizzesTotal ? Math.round(100 * progress.quizzesCompleted / progress.quizzesTotal) : 0
  return <section className="progress-panel" aria-label={t('quizHistory.s4', "Ваш прогресс")}>
    <div className="pp-stats">
      <div><b>{progress.quizzesCompleted}<small> / {progress.quizzesTotal}</small></b><span>{t('quizHistory.s5', "викторин пройдено")}</span></div>
      <div><b>{progress.averageBestScore}%</b><span>{t('quizHistory.s6', "средний лучший результат")}</span></div>
      <div><b>{progress.correctAnswers}<small> / {progress.answeredQuestions}</small></b><span>{t('quizHistory.s7', "верных ответов")}</span></div>
      <div><b>{progress.totalPoints}</b><span>{pointsWord(progress.totalPoints)}{' '}{t('quizHistory.s8', "всего")}</span></div>
    </div>
    <div className="pp-bar" role="progressbar" aria-label={t('quizHistory.s9', "Пройдено викторин")} aria-valuemin={0} aria-valuemax={100} aria-valuenow={pct}><i style={{ width: `${pct}%` }} /></div>
    <ul className="pp-topics">{progress.topics.map(topic => {
      const w = topic.quizzesTotal ? Math.round(100 * topic.quizzesCompleted / topic.quizzesTotal) : 0
      return <li key={topic.topic}>
        <span className="pp-name">{TOPIC[topic.topic] ?? topic.topic}</span>
        <span className="pp-track" aria-hidden="true"><i style={{ width: `${w}%` }} /></span>
        <span className="pp-num">{topic.quizzesCompleted}{' '}{t('quizHistory.s10', "из")}{' '}{topic.quizzesTotal}{topic.quizzesCompleted > 0 && <small>{' '}{t('quizHistory.s11', "· лучший в среднем")}{' '}{topic.averageBestScore}%</small>}</span>
      </li>
    })}</ul>
  </section>
}

/** Список прохождений; по клику открывается разбор. */
export function HistoryList({ items, onOpen }: { items: HistoryItem[]; onOpen: (attemptId: number) => void }) {
  return <ul className="history-list">{items.map(h => <li key={h.attemptId}>
    <button onClick={() => onOpen(h.attemptId)} aria-label={tf('quizHistory.s12', "Разбор: {0}, {1}", h.quizTitle, formatDateTime(h.completedAt))}>
      <span className={`hl-score ${h.score >= 80 ? 'good' : h.score >= 50 ? 'mid' : 'low'}`}>{h.score}%</span>
      <span className="hl-main"><b>{h.quizTitle}</b><small>{TOPIC[h.topic] ?? h.topic} · {h.correctCount}{' '}{t('quizHistory.s10', "из")}{' '}{h.questionCount}{' '}{t('quizHistory.s13', "верно")}{h.repeatAttempt ? t('quizHistory.s14', " · повторно") : ''}</small></span>
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
    <button className="back" onClick={onBack}>{t('quizHistory.s15', "← К истории прохождений")}</button>
    {status === 'loading' && <p role="status" style={note}>{t('quizHistory.s16', "Загрузка…")}</p>}
    {status === 'error' && <p role="alert" style={note}>{t('quizHistory.s17', "Не удалось загрузить прохождение.")}</p>}
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
    <button className="back" onClick={onBack}>{t('quizHistory.s18', "← К списку викторин")}</button>
    <div className="page-title"><p className="kicker">{t('quizHistory.s19', "ВИКТОРИНЫ · ИСТОРИЯ")}</p><h1>{t('quizHistory.s20', "История")}{' '}<i>{t('quizHistory.s3', "прохождений")}</i></h1><p>{t('quizHistory.s21', "Все ваши попытки: результат, очки и разбор ответов с пояснениями.")}</p></div>
    <div className="history-tools">
      <label>{t('quizHistory.s22', "Викторина")}
        <select value={quizId} onChange={e => setQuizId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">{t('quizHistory.s23', "Все викторины")}</option>
          {quizzes.map(q => <option key={q.id} value={q.id}>{q.title}</option>)}
        </select>
      </label>
      {status === 'ready' && <span role="status">{t('quizHistory.s24', "Прохождений:")}{' '}{items.length}</span>}
    </div>
    {status === 'loading' && <p role="status" style={note}>{t('quizHistory.s16', "Загрузка…")}</p>}
    {status === 'error' && <p role="alert" style={note}>{t('quizHistory.s25', "Не удалось загрузить историю.")}</p>}
    {status === 'ready' && items.length === 0 && <p style={note}>{t('quizHistory.s26', "Прохождений пока нет.")}</p>}
    {status === 'ready' && items.length > 0 && <HistoryList items={items} onOpen={setOpen} />}
  </main>
}
