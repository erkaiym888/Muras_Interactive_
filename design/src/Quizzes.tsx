import { useEffect, useState } from 'react'
import './features.css'
import { fetchQuiz, fetchQuizzes } from './api'
import type { Quiz, QuizQuestion, QuizSummary, User } from './api'

type Status = 'loading' | 'ready' | 'error'

/** Ответ на один вопрос: выбранные варианты или пары «левая часть → правая часть». */
export type QuizAnswer = { optionIds: number[]; pairs: Record<number, string> }
export type QuizAnswers = Record<number, QuizAnswer>

const TOPIC: Record<string, string> = { YURT: 'Юрта', RITES: 'Обряды', FOLKLORE: 'Герои и фольклор' }
const LEVEL: Record<string, string> = { EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная' }
const HINT: Record<string, string> = {
  SINGLE_CHOICE: 'Выберите один ответ',
  MULTIPLE_CHOICE: 'Выберите все верные ответы',
  TRUE_FALSE: 'Верно или неверно?',
  MATCHING: 'Сопоставьте пары',
}
const note = { textAlign: 'center' as const, color: '#716861' }

function questionsWord(n: number) {
  const last = n % 10, tail = n % 100
  if (last === 1 && tail !== 11) return 'вопрос'
  if (last >= 2 && last <= 4 && (tail < 12 || tail > 14)) return 'вопроса'
  return 'вопросов'
}

const empty: QuizAnswer = { optionIds: [], pairs: {} }

function isAnswered(question: QuizQuestion, answer: QuizAnswer) {
  if (question.type === 'MATCHING') return question.options.every(o => answer.pairs[o.id])
  return answer.optionIds.length > 0
}

function Question({ question, answer, onChange }: { question: QuizQuestion; answer: QuizAnswer; onChange: (a: QuizAnswer) => void }) {
  if (question.type === 'MATCHING') {
    return <fieldset className="quiz-opts">
      <legend>{HINT[question.type]}</legend>
      {question.options.map(o => {
        const taken = Object.entries(answer.pairs).filter(([id]) => Number(id) !== o.id).map(([, v]) => v)
        return <div className="quiz-pair" key={o.id}>
          <span id={`left-${o.id}`}>{o.text}</span>
          <select aria-labelledby={`left-${o.id}`} value={answer.pairs[o.id] ?? ''} onChange={e => onChange({ ...answer, pairs: { ...answer.pairs, [o.id]: e.target.value } })}>
            <option value="" disabled>Выберите…</option>
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
  return <fieldset className="quiz-opts">
    <legend>{HINT[question.type]}</legend>
    {question.options.map(o => <label key={o.id}>
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
  const [done, setDone] = useState(false)

  useEffect(() => {
    let alive = true
    setStatus('loading')
    window.scrollTo({ top: 0 })
    fetchQuiz(quizId)
      .then(q => { if (alive) { setQuiz(q); setStatus('ready') } })
      .catch(() => { if (alive) setStatus('error') })
    return () => { alive = false }
  }, [quizId])

  const exit = <button className="back" onClick={onExit}>← К списку викторин</button>

  if (status !== 'ready' || !quiz) {
    return <main className="catalog">{exit}
      {status === 'error' ? <p role="alert" style={note}>Не удалось загрузить викторину.</p> : <p role="status" style={note}>Загрузка…</p>}
    </main>
  }

  if (done) {
    // Здесь собранные answers отправляются на проверку (FR-5.2): сервер сравнит их с правильными
    // вариантами, покажет пояснения и начислит очки.
    return <main className="catalog">{exit}
      <section className="quiz-run quiz-done">
        <p className="kicker">{TOPIC[quiz.topic]}</p>
        <h2>Викторина пройдена</h2>
        <p>Вы ответили на все вопросы: {quiz.questions.length}.</p>
        <div className="quiz-buttons"><button className="btn" onClick={onExit}>К списку викторин</button></div>
      </section>
    </main>
  }

  const question = quiz.questions[index]
  const answer = answers[question.id] ?? empty
  const last = index === quiz.questions.length - 1

  return <main className="catalog">{exit}
    <section className="quiz-run" aria-labelledby="quiz-q">
      <div className="quiz-top"><span>{quiz.title}</span><span>Вопрос {index + 1} из {quiz.questions.length}</span></div>
      <div className="quiz-progress" role="progressbar" aria-valuemin={0} aria-valuemax={quiz.questions.length} aria-valuenow={index + 1}><i style={{ width: `${((index + 1) / quiz.questions.length) * 100}%` }} /></div>
      <h2 id="quiz-q">{question.text}</h2>
      <Question key={question.id} question={question} answer={answer} onChange={a => setAnswers({ ...answers, [question.id]: a })} />
      <div className="quiz-actions">
        <button className="btn outline" disabled={index === 0} onClick={() => setIndex(index - 1)}>Назад</button>
        <button className="btn" disabled={!isAnswered(question, answer)} onClick={() => (last ? setDone(true) : setIndex(index + 1))}>{last ? 'Завершить' : 'Далее'}</button>
      </div>
    </section>
  </main>
}

/** Список викторин и прохождение. Гость видит приглашение войти (FR-5.1). */
export function Quizzes({ user, go }: { user: User | null; go: (p: 'login' | 'register') => void }) {
  const [list, setList] = useState<QuizSummary[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [activeId, setActiveId] = useState<number | null>(null)
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

  if (activeId !== null && user) return <QuizPlayer quizId={activeId} onExit={() => setActiveId(null)} />

  return <main className="catalog">
    <div className="page-title"><p className="kicker">ПРОВЕРЬТЕ ЗНАНИЯ · 04</p><h1>Викторины о <i>наследии</i></h1><p>Вопросы по юрте, обрядам и фольклору: выбор ответа, несколько ответов, сопоставление и «верно или неверно».</p></div>
    {!user ? <section className="quiz-gate">
      <h2>Викторины для зарегистрированных пользователей</h2>
      <p>Войдите или создайте аккаунт, чтобы проходить викторины.</p>
      <div className="quiz-buttons"><button className="btn" onClick={() => go('login')}>Войти</button><button className="btn outline" onClick={() => go('register')}>Зарегистрироваться</button></div>
    </section> : <>
      {status === 'loading' && <p role="status" style={note}>Загрузка…</p>}
      {status === 'error' && <p role="alert" style={note}>Не удалось загрузить викторины. Проверьте, что сервер запущен.</p>}
      <div className="quiz-grid">{list.map(q => <article className="quiz-card" key={q.id}>
        <div className="quiz-meta"><span>{TOPIC[q.topic]}</span><span>{LEVEL[q.difficulty]}</span><span>{q.questionCount} {questionsWord(q.questionCount)}</span></div>
        <h2>{q.title}</h2>
        {q.description && <p>{q.description}</p>}
        <button className="btn" onClick={() => setActiveId(q.id)}>Начать</button>
      </article>)}</div>
    </>}
  </main>
}
