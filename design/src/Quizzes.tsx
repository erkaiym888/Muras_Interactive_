import { lrec, t, tp } from './i18n'
import { useEffect, useState } from 'react'
import './features.css'
import { ApiError, fetchAwards, fetchHint, fetchProgress, fetchQuiz, fetchQuizzes, submitAttempt } from './api'
import type { AttemptResult, Progress, Quiz, QuizHint, QuizQuestion, QuizSummary, User } from './api'
import { ProgressPanel, QuizHistoryPage, attemptsWord } from './QuizHistory'
import { QuizResultView, TOPIC, pointsWord } from './QuizResult'

type Status = 'loading' | 'ready' | 'error'

/** Ответ на один вопрос: выбранные варианты или пары «левая часть → правая часть». */
export type QuizAnswer = { optionIds: number[]; pairs: Record<number, string> }
export type QuizAnswers = Record<number, QuizAnswer>

const LEVEL: Record<string, string> = lrec('quiz.level', { EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная' })
const HINT: Record<string, string> = lrec('quiz.hint', {
  SINGLE_CHOICE: 'Выберите один ответ',
  MULTIPLE_CHOICE: 'Выберите все верные ответы',
  TRUE_FALSE: 'Верно или неверно?',
  MATCHING: 'Сопоставьте пары',
})
const note = { textAlign: 'center' as const, color: '#716861' }

function questionsWord(n: number) {
  return tp('plural.questions', n, 'вопрос', 'вопроса', 'вопросов')
}

const empty: QuizAnswer = { optionIds: [], pairs: {} }

function isAnswered(question: QuizQuestion, answer: QuizAnswer) {
  if (question.type === 'MATCHING') return question.options.every(o => answer.pairs[o.id])
  return answer.optionIds.length > 0
}

function Question({ question, answer, onChange, hint }: { question: QuizQuestion; answer: QuizAnswer; onChange: (a: QuizAnswer) => void; hint?: QuizHint }) {
  if (question.type === 'MATCHING') {
    const locked = new Set((hint?.revealedPairs ?? []).map(p => p.optionId))
    return <fieldset className="quiz-opts">
      <legend>{HINT[question.type]}</legend>
      {question.options.map(o => {
        const taken = Object.entries(answer.pairs).filter(([id]) => Number(id) !== o.id).map(([, v]) => v)
        return <div className={locked.has(o.id) ? 'quiz-pair revealed' : 'quiz-pair'} key={o.id}>
          <span id={`left-${o.id}`}>{o.text}</span>
          <select aria-labelledby={`left-${o.id}`} disabled={locked.has(o.id)} value={answer.pairs[o.id] ?? ''} onChange={e => onChange({ ...answer, pairs: { ...answer.pairs, [o.id]: e.target.value } })}>
            <option value="" disabled>{t('quizzes.s4', "Выберите…")}</option>
            {question.matchChoices.map(c => <option key={c} value={c} disabled={taken.includes(c)}>{c}</option>)}
          </select>
        </div>
      })}
    </fieldset>
  }

  const multiple = question.type === 'MULTIPLE_CHOICE'
  const toggle = (id: number) => {
    if (!multiple) return onChange({ ...answer, optionIds: [id] })
    const ids = answer.optionIds.includes(id) ? answer.optionIds.filter(x => x !== id) : [...answer.optionIds, id]
    onChange({ ...answer, optionIds: ids })
  }
  const hidden = new Set(hint?.hiddenOptionIds ?? [])
  return <fieldset className="quiz-opts">
    <legend>{HINT[question.type]}</legend>
    {question.options.filter(o => !hidden.has(o.id)).map(o => <label key={o.id}>
      <input type={multiple ? 'checkbox' : 'radio'} name={`q${question.id}`} checked={answer.optionIds.includes(o.id)} onChange={() => toggle(o.id)} />
      <span>{o.text}</span>
    </label>)}
  </fieldset>
}

function QuizPlayer({ quizId, onExit }: { quizId: number; onExit: () => void }) {
  const [quiz, setQuiz] = useState<Quiz | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState<QuizAnswers>({})
  const [hints, setHints] = useState<Record<number, QuizHint>>({})
  const [hintBusy, setHintBusy] = useState(false)
  const [result, setResult] = useState<AttemptResult | null>(null)
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState('')
  const [round, setRound] = useState(0)

  useEffect(() => {
    let alive = true
    setStatus('loading')
    window.scrollTo({ top: 0 })
    fetchQuiz(quizId)
      .then(q => { if (alive) { setQuiz(q); setStatus('ready') } })
      .catch(() => { if (alive) setStatus('error') })
    return () => { alive = false }
  }, [quizId, round])

  const restart = () => { setAnswers({}); setHints({}); setResult(null); setIndex(0); setSendError(''); setRound(r => r + 1) }

  // Подсказка (FR-5.2): часть неверных вариантов убирается или показывается верная пара, очки за вопрос × 0,5
  const takeHint = async (q: QuizQuestion) => {
    setHintBusy(true)
    try {
      const h = await fetchHint(quizId, q.id)
      setHints(prev => ({ ...prev, [q.id]: h }))
      if (h.available) {
        const cur = answers[q.id] ?? empty
        const pairs = { ...cur.pairs }
        h.revealedPairs.forEach(p => { pairs[p.optionId] = p.match })
        const optionIds = cur.optionIds.filter(id => !h.hiddenOptionIds.includes(id))
        setAnswers(prev => ({ ...prev, [q.id]: { optionIds, pairs } }))
      }
    } catch { setHints(prev => ({ ...prev, [q.id]: { available: false, text: t('quizzes.s5', "Не удалось получить подсказку"), hiddenOptionIds: [], revealedPairs: [] } })) }
    finally { setHintBusy(false) }
  }

  // Ответы проверяет сервер: правильные варианты на клиент не приходят (FR-5.2)
  const finish = async () => {
    if (!quiz) return
    setSending(true)
    setSendError('')
    try {
      const payload = quiz.questions.map(q => ({
        questionId: q.id,
        optionIds: (answers[q.id] ?? empty).optionIds,
        pairs: (answers[q.id] ?? empty).pairs,
        hintUsed: Boolean(hints[q.id]?.available),
      }))
      setResult(await submitAttempt(quiz.id, payload))
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (e) {
      setSendError(e instanceof ApiError && e.status === 401 ? t('quizzes.s6', "Сессия закончилась. Войдите снова — ответы не сохранены.") : t('quizzes.s7', "Не удалось отправить ответы. Попробуйте ещё раз."))
    } finally { setSending(false) }
  }

  const exit = <button className="back" onClick={onExit}>{t('quizzes.s8', "← К списку викторин")}</button>

  if (status !== 'ready' || !quiz) {
    return <main className="catalog">{exit}
      {status === 'error' ? <p role="alert" style={note}>{t('quizzes.s9', "Не удалось загрузить викторину.")}</p> : <p role="status" style={note}>{t('quizzes.s10', "Загрузка…")}</p>}
    </main>
  }

  if (result) {
    return <main className="catalog">{exit}
      <QuizResultView result={result} actions={<>
        <button className="btn" onClick={restart}>{t('quizzes.s11', "Пройти ещё раз")}</button>
        <button className="btn outline" onClick={onExit}>{t('quizzes.s12', "К списку викторин")}</button>
      </>} />
    </main>
  }

  const question = quiz.questions[index]
  const answer = answers[question.id] ?? empty
  const last = index === quiz.questions.length - 1

  return <main className="catalog">{exit}
    <section className="quiz-run" aria-labelledby="quiz-q">
      <div className="quiz-top"><span>{quiz.title}</span><span>{t('quizzes.s13', "Вопрос")}{' '}{index + 1}{' '}{t('quizzes.s14', "из")}{' '}{quiz.questions.length} · {question.points} {pointsWord(question.points)}</span></div>
      <div className="quiz-progress" role="progressbar" aria-valuemin={0} aria-valuemax={quiz.questions.length} aria-valuenow={index + 1}><i style={{ width: `${((index + 1) / quiz.questions.length) * 100}%` }} /></div>
      <h2 id="quiz-q">{question.text}</h2>
      <Question key={question.id} question={question} answer={answer} hint={hints[question.id]} onChange={a => setAnswers({ ...answers, [question.id]: a })} />
      {question.type !== 'TRUE_FALSE' && <div className="quiz-hint">
        {hints[question.id]
          ? <p className={hints[question.id].available ? 'hint-on' : 'hint-off'} role="status">💡 {hints[question.id].text}</p>
          : <button type="button" className="hint-btn" disabled={hintBusy} onClick={() => takeHint(question)}>{t('quizzes.s15', "💡 Подсказка")}{' '}<small>{t('quizzes.s16', "очки за вопрос × 0,5")}</small></button>}
      </div>}
      {sendError && <p className="quiz-error" role="alert">{sendError}</p>}
      <div className="quiz-actions">
        <button className="btn outline" disabled={index === 0 || sending} onClick={() => setIndex(index - 1)}>{t('quizzes.s17', "Назад")}</button>
        <button className="btn" disabled={!isAnswered(question, answer) || sending} onClick={() => (last ? finish() : setIndex(index + 1))}>{last ? (sending ? t('quizzes.s18', "Проверяем…") : t('quizzes.s19', "Завершить и проверить")) : t('quizzes.s20', "Далее")}</button>
      </div>
    </section>
  </main>
}

/** Список викторин и прохождение. Гость видит приглашение войти (FR-5.1). */
export function Quizzes({ user, go }: { user: User | null; go: (p: 'login' | 'register') => void }) {
  const [list, setList] = useState<QuizSummary[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [activeId, setActiveId] = useState<number | null>(null)
  const [showHistory, setShowHistory] = useState(false)
  const [progress, setProgress] = useState<Progress | null>(null)
  const [level, setLevel] = useState(1)
  const userId = user?.id

  useEffect(() => {
    if (userId === undefined) return
    let alive = true
    setStatus('loading')
    fetchQuizzes()
      .then(l => { if (alive) { setList(l); setStatus('ready') } })
      .catch(() => { if (alive) setStatus('error') })
    return () => { alive = false }
  }, [userId])

  // Прогресс (FR-5.4) обновляем при возврате к списку - после нового прохождения
  useEffect(() => {
    if (userId === undefined || activeId !== null || showHistory) return
    let alive = true
    fetchProgress().then(p => { if (alive) setProgress(p) }).catch(() => {})
    fetchAwards().then(a => { if (alive) setLevel(a.level.number) }).catch(() => {})
    return () => { alive = false }
  }, [userId, activeId, showHistory])

  if (activeId !== null && user) return <QuizPlayer quizId={activeId} onExit={() => setActiveId(null)} />
  if (showHistory && user) return <QuizHistoryPage quizzes={list} onBack={() => setShowHistory(false)} />

  const mine = (id: number) => progress?.quizzes.find(q => q.quizId === id)

  return <main className="catalog">
    <div className="page-title"><p className="kicker">{t('quizzes.s21', "ПРОВЕРЬТЕ ЗНАНИЯ · 04")}</p><h1>{t('quizzes.s22', "Викторины о")}{' '}<i>{t('quizzes.s23', "наследии")}</i></h1><p>{t('quizzes.s24', "Вопросы по юрте, обрядам и фольклору: выбор ответа, несколько ответов, сопоставление и «верно или неверно».")}</p></div>
    {!user ? <section className="quiz-gate">
      <h2>{t('quizzes.s25', "Викторины для зарегистрированных пользователей")}</h2>
      <p>{t('quizzes.s26', "Войдите или создайте аккаунт, чтобы проходить викторины.")}</p>
      <div className="quiz-buttons"><button className="btn" onClick={() => go('login')}>{t('quizzes.s27', "Войти")}</button><button className="btn outline" onClick={() => go('register')}>{t('quizzes.s28', "Зарегистрироваться")}</button></div>
    </section> : <>
      {progress && <>
        <ProgressPanel progress={progress} />
        <div className="quiz-buttons history-link"><button className="btn outline" onClick={() => setShowHistory(true)} disabled={progress.attemptsCount === 0}>{t('quizzes.s29', "История прохождений ·")}{' '}{progress.attemptsCount}</button></div>
      </>}
      {status === 'loading' && <p role="status" style={note}>{t('quizzes.s10', "Загрузка…")}</p>}
      {status === 'error' && <p role="alert" style={note}>{t('quizzes.s30', "Не удалось загрузить викторины. Проверьте, что сервер запущен.")}</p>}
      <div className="quiz-grid">{list.map(q => <article className="quiz-card" key={q.id}>
        <div className="quiz-meta"><span>{TOPIC[q.topic]}</span><span>{LEVEL[q.difficulty]}</span><span>{q.questionCount} {questionsWord(q.questionCount)}</span></div>
        <h2>{q.title}</h2>
        {q.description && <p>{q.description}</p>}
        {(() => { const m = mine(q.id); return m && m.bestScore !== null
          ? <p className="quiz-mine"><b>{m.bestScore}%</b>{' '}{t('quizzes.s31', "лучший результат ·")}{' '}{m.attempts} {attemptsWord(m.attempts)}</p>
          : <p className="quiz-mine new">{t('quizzes.s32', "Ещё не пройдена · +25 за первое прохождение")}</p> })()}
        {q.difficulty === 'HARD' && level < 5
          ? <p className="quiz-locked" role="note">{t('quizzes.s33', "🔒 Откроется на уровне «Акын» (800 очков)")}</p>
          : <button className="btn" onClick={() => setActiveId(q.id)}>{mine(q.id)?.bestScore != null ? t('quizzes.s34', "Пройти снова") : t('quizzes.s35', "Начать")}</button>}
      </article>)}</div>
    </>}
  </main>
}
