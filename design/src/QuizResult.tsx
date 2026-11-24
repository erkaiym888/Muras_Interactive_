import { lrec, t, tf, tp, ts } from './i18n'
import type { ReactNode } from 'react'
import type { AttemptResult, QuestionResult } from './api'
import { NewRewards } from './Rewards'

/** Результат прохождения (FR-5.2): процент, очки с расшифровкой, разбор вопросов с пояснениями. */

export const TOPIC: Record<string, string> = lrec('quiz.topic', { YURT: 'Юрта', RITES: 'Обряды', FOLKLORE: 'Герои и фольклор' })

export function pointsWord(n: number) {
  return tp('plural.points', n, 'очко', 'очка', 'очков')
}

function verdict(score: number) {
  if (score === 100) return t('quizResult.s4', "Безупречно!")
  if (score >= 80) return t('quizResult.s5', "Отличный результат")
  if (score >= 50) return t('quizResult.s6', "Хороший результат")
  return t('quizResult.s7', "Есть над чем поработать")
}

/** Кольцо с процентом. */
export function ScoreRing({ score, size = 132 }: { score: number; size?: number }) {
  const r = 52, c = 2 * Math.PI * r
  return <svg className="score-ring" width={size} height={size} viewBox="0 0 120 120" role="img" aria-label={tf('quizResult.s8', "Результат {0}%", score)}>
    <circle cx="60" cy="60" r={r} className="track" />
    <circle cx="60" cy="60" r={r} className="value" strokeDasharray={c} strokeDashoffset={c * (1 - score / 100)} transform="rotate(-90 60 60)" />
    <text x="60" y="58" className="pct">{score}%</text>
    <text x="60" y="78" className="cap">{t('quizResult.s9', "результат")}</text>
  </svg>
}

function QuestionReview({ q, n }: { q: QuestionResult; n: number }) {
  const partial = !q.correct && q.correctItems > 0
  const state = q.correct ? 'ok' : partial ? 'part' : 'bad'
  const label = q.correct ? t('quizResult.s10', "Верно") : partial ? tf('quizResult.s11', "Частично: {0} из {1}", q.correctItems, q.totalItems) : t('quizResult.s12', "Неверно")
  return <li className={`qr ${state}`}>
    <div className="qr-head">
      <span className="qr-no">{String(n).padStart(2, '0')}</span>
      <p className="qr-text">{q.text}</p>
      <span className="qr-pts" title={t('quizResult.s13', "Очки за вопрос")}>+{q.pointsEarned}<small> / {q.maxPoints}</small></span>
    </div>
    <p className="qr-state"><b>{label}</b>{q.hintUsed && <em>{t('quizResult.s14', "с подсказкой · очки × 0,5")}</em>}</p>
    <dl>
      <div><dt>{t('quizResult.s15', "Ваш ответ")}</dt><dd>{q.userAnswer || '—'}</dd></div>
      {!q.correct && q.correctAnswer && <div><dt>{t('quizResult.s16', "Правильный ответ")}</dt><dd>{q.correctAnswer}</dd></div>}
    </dl>
    {q.explanation && <p className="qr-expl">{q.explanation}</p>}
  </li>
}

export function QuizResultView({ result, actions, compact = false }: { result: AttemptResult; actions?: ReactNode; compact?: boolean }) {
  return <section className={compact ? 'quiz-result compact' : 'quiz-result'} aria-labelledby={`res-${result.attemptId}`}>
    <div className="res-head">
      <ScoreRing score={result.score} />
      <div className="res-summary">
        <p className="kicker">{TOPIC[result.topic] ?? result.topic}{result.repeatAttempt && t('quizResult.s17', " · повторное прохождение")}</p>
        <h2 id={`res-${result.attemptId}`}>{compact ? result.quizTitle : verdict(result.score)}</h2>
        <p className="res-facts">
          <span><b>{result.correctCount}</b>{' '}{t('quizResult.s18', "из")}{' '}{result.questionCount}{' '}{t('quizResult.s19', "верно")}</span>
          {result.bestStreak >= 2 && <span>{t('quizResult.s20', "серия")}{' '}<b>{result.bestStreak}</b>{' '}{t('quizResult.s21', "подряд")}</span>}
          {result.hintsUsed > 0 && <span>{t('quizResult.s22', "подсказок:")}{' '}<b>{result.hintsUsed}</b></span>}
        </p>
        <p className="res-earned">+{result.pointsEarned} <small>{pointsWord(result.pointsEarned)}</small></p>
      </div>
    </div>

    <ul className="res-points" aria-label={t('quizResult.s23', "Расшифровка очков")}>
      {result.points.map((l, i) => <li key={i} className={l.points === 0 ? 'zero' : ''}><span>{ts(l.label)}</span><b>{l.points > 0 ? `+${l.points}` : '—'}</b></li>)}
      <li className="sum"><span>{t('quizResult.s24', "Итого за прохождение")}</span><b>+{result.pointsEarned}</b></li>
      {!compact && <li className="total"><span>{t('quizResult.s25', "Всего у вас")}</span><b>{result.totalPoints} {pointsWord(result.totalPoints)}</b></li>}
    </ul>

    <NewRewards achievements={result.newAchievements ?? []} levelUp={result.levelUp} />

    {actions && <div className="quiz-buttons res-actions">{actions}</div>}

    <h3 className="res-review-title">{t('quizResult.s26', "Разбор ответов")}</h3>
    <ol className="res-review">{result.questions.map((q, i) => <QuestionReview key={i} q={q} n={i + 1} />)}</ol>
  </section>
}
