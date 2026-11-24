import type { ReactNode } from 'react'
import type { AttemptResult, QuestionResult } from './api'
import { NewRewards } from './Rewards'

/** Результат прохождения (FR-5.2): процент, очки с расшифровкой, разбор вопросов с пояснениями. */

export const TOPIC: Record<string, string> = { YURT: 'Юрта', RITES: 'Обряды', FOLKLORE: 'Герои и фольклор' }

export function pointsWord(n: number) {
  const last = Math.abs(n) % 10, tail = Math.abs(n) % 100
  if (last === 1 && tail !== 11) return 'очко'
  if (last >= 2 && last <= 4 && (tail < 12 || tail > 14)) return 'очка'
  return 'очков'
}

function verdict(score: number) {
  if (score === 100) return 'Безупречно!'
  if (score >= 80) return 'Отличный результат'
  if (score >= 50) return 'Хороший результат'
  return 'Есть над чем поработать'
}

/** Кольцо с процентом. */
export function ScoreRing({ score, size = 132 }: { score: number; size?: number }) {
  const r = 52, c = 2 * Math.PI * r
  return <svg className="score-ring" width={size} height={size} viewBox="0 0 120 120" role="img" aria-label={`Результат ${score}%`}>
    <circle cx="60" cy="60" r={r} className="track" />
    <circle cx="60" cy="60" r={r} className="value" strokeDasharray={c} strokeDashoffset={c * (1 - score / 100)} transform="rotate(-90 60 60)" />
    <text x="60" y="58" className="pct">{score}%</text>
    <text x="60" y="78" className="cap">результат</text>
  </svg>
}

function QuestionReview({ q, n }: { q: QuestionResult; n: number }) {
  const partial = !q.correct && q.correctItems > 0
  const state = q.correct ? 'ok' : partial ? 'part' : 'bad'
  const label = q.correct ? 'Верно' : partial ? `Частично: ${q.correctItems} из ${q.totalItems}` : 'Неверно'
  return <li className={`qr ${state}`}>
    <div className="qr-head">
      <span className="qr-no">{String(n).padStart(2, '0')}</span>
      <p className="qr-text">{q.text}</p>
      <span className="qr-pts" title="Очки за вопрос">+{q.pointsEarned}<small> / {q.maxPoints}</small></span>
    </div>
    <p className="qr-state"><b>{label}</b>{q.hintUsed && <em>с подсказкой · очки × 0,5</em>}</p>
    <dl>
      <div><dt>Ваш ответ</dt><dd>{q.userAnswer || '—'}</dd></div>
      {!q.correct && q.correctAnswer && <div><dt>Правильный ответ</dt><dd>{q.correctAnswer}</dd></div>}
    </dl>
    {q.explanation && <p className="qr-expl">{q.explanation}</p>}
  </li>
}

export function QuizResultView({ result, actions, compact = false }: { result: AttemptResult; actions?: ReactNode; compact?: boolean }) {
  return <section className={compact ? 'quiz-result compact' : 'quiz-result'} aria-labelledby={`res-${result.attemptId}`}>
    <div className="res-head">
      <ScoreRing score={result.score} />
      <div className="res-summary">
        <p className="kicker">{TOPIC[result.topic] ?? result.topic}{result.repeatAttempt && ' · повторное прохождение'}</p>
        <h2 id={`res-${result.attemptId}`}>{compact ? result.quizTitle : verdict(result.score)}</h2>
        <p className="res-facts">
          <span><b>{result.correctCount}</b> из {result.questionCount} верно</span>
          {result.bestStreak >= 2 && <span>серия <b>{result.bestStreak}</b> подряд</span>}
          {result.hintsUsed > 0 && <span>подсказок: <b>{result.hintsUsed}</b></span>}
        </p>
        <p className="res-earned">+{result.pointsEarned} <small>{pointsWord(result.pointsEarned)}</small></p>
      </div>
    </div>

    <ul className="res-points" aria-label="Расшифровка очков">
      {result.points.map((l, i) => <li key={i} className={l.points === 0 ? 'zero' : ''}><span>{l.label}</span><b>{l.points > 0 ? `+${l.points}` : '—'}</b></li>)}
      <li className="sum"><span>Итого за прохождение</span><b>+{result.pointsEarned}</b></li>
      {!compact && <li className="total"><span>Всего у вас</span><b>{result.totalPoints} {pointsWord(result.totalPoints)}</b></li>}
    </ul>

    <NewRewards achievements={result.newAchievements ?? []} levelUp={result.levelUp} />

    {actions && <div className="quiz-buttons res-actions">{actions}</div>}

    <h3 className="res-review-title">Разбор ответов</h3>
    <ol className="res-review">{result.questions.map((q, i) => <QuestionReview key={i} q={q} n={i + 1} />)}</ol>
  </section>
}
