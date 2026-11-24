import { ReactNode, useEffect, useRef, useState } from 'react'
import { photos } from './data'
import { Traditions, TraditionDetail } from './Traditions'
import { Auth } from './Auth'
import { Folklore, FolkloreDetail } from './Folklore'
import type { FolkloreRef } from './Folklore'
import { canEditContent, fetchMe, isAdmin, logout, postVisit } from './api'
import { Rating } from './Rating'
import { RewardToasts, emitReward, trackView } from './Rewards'
import { Admin } from './Admin'
import { initLanguages, t, tf, useI18n } from './i18n'
import { Editor } from './Editor'
import { Quizzes } from './Quizzes'
import { Cabinet } from './Cabinet'
import { Avatar } from './ProfileEdit'
import type { RelationNode, SearchResult, User } from './api'
import { HeaderSearch } from './Search'
import { Linker, SeeAlso, useRelationLinks } from './Relations'
import AuthenticYurtDiagram from './AuthenticYurtDiagram'
import { PartKey, partKeys, YURT_IMAGE, yurtParts, yurtSize } from './yurt'

type Page = 'home' | 'yurt' | 'yurt-detail' | 'traditions' | 'tradition-detail' | 'folklore' | 'folklore-detail' | 'editor' | 'quizzes' | 'rating' | 'cabinet' | 'admin' | 'login' | 'register' | 'recover' | 'reset'

const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6"/></svg>
const Eye = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/></svg>

function Logo({ onClick }: { onClick: () => void }) {
  return <button className="logo" onClick={onClick} aria-label={t('app.s1', "На главную")}><span className="logo-mark">{t('app.s2', "М")}</span><span><b>MURAS</b><small>INTERACTIVE</small></span></button>
}

/** Разделы меню; подписи - через t(), чтобы их можно было перевести словарём (FR-8.1). */
const NAV = () => [
  { label: t('nav.home', 'Главная'), p: 'home' as Page, note: t('nav.home.note', 'Цифровой музей наследия') },
  { label: t('nav.yurt', 'Юрта'), p: 'yurt' as Page, note: t('nav.yurt.note', 'Интерактивная экспозиция') },
  { label: t('nav.traditions', 'Традиции'), p: 'traditions' as Page, note: t('nav.traditions.note', 'Обряды и обычаи') },
  { label: t('nav.folklore', 'Фольклор'), p: 'folklore' as Page, note: t('nav.folklore.note', 'Герои и произведения') },
  { label: t('nav.quizzes', 'Викторины'), p: 'quizzes' as Page, note: t('nav.quizzes.note', 'Проверьте знания') },
  { label: t('nav.rating', 'Рейтинг'), p: 'rating' as Page, note: t('nav.rating.note', 'Общий и недельный') },
]

const ROLE_LABEL = (): Record<string, string> => ({
  ADMIN: t('role.admin', 'Администратор'), EDITOR: t('role.editor', 'Редактор контента'), USER: t('role.user', 'Посетитель'),
})

/**
 * Переключатель языка (FR-8.1/8.2); пока включён один язык - не показывается.
 * compact - короткий вариант для шапки (RU · KY · EN), виден на всех страницах.
 */
function LanguageSwitch({ compact = false }: { compact?: boolean }) {
  const { lang, languages, setLang } = useI18n()
  if (languages.length < 2) return null
  return <div className={compact ? 'lang-switch compact' : 'lang-switch'} role="group" aria-label={t('lang.label', 'Язык сайта')}>
    {languages.map(l => <button key={l.code} className={l.code === lang ? 'active' : ''} aria-pressed={l.code === lang}
      lang={l.code} title={l.nativeName} onClick={() => setLang(l.code)}>{compact ? l.code.toUpperCase() : l.nativeName}</button>)}
  </div>
}

/** Шапка: только логотип и кнопка меню. Все разделы и аккаунт - в выезжающей панели. */
function Header({ page, go, user, onLogout, onFound }: { page: Page; go: (p: Page) => void; user: User | null; onLogout: () => void; onFound: (r: SearchResult) => void }) {
  const [open, setOpen] = useState(false)
  useI18n() // перерисовка подписей при смене языка
  const burger = useRef<HTMLButtonElement>(null)
  const panel = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    panel.current?.querySelector<HTMLElement>('.drawer-close')?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
      if (e.key === 'Tab' && panel.current) {
        const items = panel.current.querySelectorAll<HTMLElement>('button:not(:disabled)')
        const first = items[0], last = items[items.length - 1]
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus() }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus() }
      }
    }
    document.addEventListener('keydown', onKey)
    return () => { document.body.style.overflow = prev; document.removeEventListener('keydown', onKey); burger.current?.focus() }
  }, [open])

  const to = (p: Page) => { setOpen(false); go(p) }
  const items: { label: string; p: Page; note: string }[] = NAV()
  if (canEditContent(user)) items.push({ label: t('nav.editor', 'Редактор'), p: 'editor', note: t('nav.editor.note', 'Управление содержимым') })
  if (isAdmin(user)) items.push({ label: t('nav.admin', 'Пользователи'), p: 'admin', note: t('nav.admin.note', 'Роли, доступ и языки') })

  return <header className="header">
    <Logo onClick={() => go('home')} />
    <HeaderSearch onOpen={onFound} />
    <div className="header-right"><LanguageSwitch compact />
    <button ref={burger} className={open ? 'burger open' : 'burger'} onClick={() => setOpen(!open)} aria-expanded={open} aria-controls="site-menu" aria-label={open ? t('menu.close', 'Закрыть меню') : t('menu.open', 'Открыть меню')}>
      <span className="burger-label">{t('menu', 'Меню')}</span><span className="burger-lines" aria-hidden="true"><i/><i/><i/></span>
    </button></div>
    <div className={open ? 'drawer-backdrop show' : 'drawer-backdrop'} onClick={() => setOpen(false)} aria-hidden="true"/>
    <div ref={panel} id="site-menu" className={open ? 'drawer open' : 'drawer'} role="dialog" aria-modal="true" aria-label={t('menu.site', 'Меню сайта')} inert={!open}>
      <div className="drawer-top"><span className="kicker">MURAS INTERACTIVE</span><button className="drawer-close" onClick={() => setOpen(false)} aria-label={t('menu.close', 'Закрыть меню')}>×</button></div>
      <LanguageSwitch />
      <nav className="drawer-nav" aria-label={t('menu.sections', 'Разделы')}>
        {items.map((n, i) => <button key={n.p} className={page === n.p || page === `${n.p}-detail` ? 'active' : ''} aria-current={page === n.p ? 'page' : undefined} onClick={() => to(n.p)}>
          <em>{String(i + 1).padStart(2, '0')}</em><span><b>{n.label}</b><small>{n.note}</small></span>
        </button>)}
      </nav>
      <div className="drawer-account">
        {user ? <>
          <p className="who"><Avatar user={user} size="sm" /><span><b>{user.displayName}</b><small>{ROLE_LABEL()[user.role] ?? user.role}</small></span></p>
          <button className="drawer-btn" onClick={() => to('cabinet')}>{t('account.cabinet', 'Личный кабинет')} <Arrow/></button>
          <button className="drawer-btn ghost" onClick={() => { setOpen(false); onLogout() }}>{t('account.logout', 'Выйти из аккаунта')}</button>
        </> : <>
          <p className="drawer-hint">{t('account.hint', 'Войдите, чтобы сохранять материалы и проходить викторины.')}</p>
          <div className="drawer-auth"><button className="drawer-btn" onClick={() => to('login')}>{t('account.login', 'Войти')} <Arrow/></button><button className="drawer-btn ghost" onClick={() => to('register')}>{t('account.register', 'Регистрация')}</button></div>
        </>}
      </div>
    </div>
  </header>
}

function Button({ children, secondary, onClick, type='button' }: { children: ReactNode; secondary?: boolean; onClick?: () => void; type?: 'button'|'submit' }) {
  return <button type={type} onClick={onClick} className={secondary ? 'btn secondary' : 'btn'}>{children}<Arrow/></button>
}

function Footer({ go }: { go: (p: Page) => void }) {
  useI18n()
  return <footer><div><Logo onClick={() => go('home')} /><p>{t('footer.tagline', 'Цифровой музей живого наследия Кыргызстана.')}</p></div><div className="footer-links"><button onClick={() => go('yurt')}>{t('footer.yurt', 'Исследовать юрту')}</button><button onClick={() => go('traditions')}>{t('nav.traditions', 'Традиции')}</button><span>© 2026 Muras Interactive</span></div></footer>
}

function Home({ go }: { go: (p: Page) => void }) {
  useI18n() // перерисовка текстов при смене языка (FR-8.2)
  return <main>
    <section className="hero" style={{backgroundImage:`linear-gradient(90deg,rgba(25,17,14,.76),rgba(25,17,14,.1)),url(${photos.hero})`}}>
      <div className="hero-copy"><p className="kicker light">{t('home.kicker', 'ЦИФРОВОЙ МУЗЕЙ КЫРГЫЗСТАНА')}</p><h1>{t('home.title', 'Открой наследие')}<br/><i>{t('home.titleAccent', 'Кыргызстана')}</i></h1><p className="lead">{t('home.lead', 'Исследуйте устройство юрты, древние обряды и живые традиции народа через современный интерактивный музей.')}</p><div className="actions"><Button onClick={() => go('yurt')}>{t('home.toYurt', 'Исследовать юрту')}</Button><Button secondary onClick={() => go('traditions')}>{t('home.toTraditions', 'Узнать о традициях')}</Button></div></div>
      <div className="hero-note"><span>01</span><p>{t('app.s3', "Живое наследие")}<br/>{t('app.s4', "в цифровом пространстве")}</p></div>
    </section>
    <section className="intro"><p className="kicker">{t('app.s5', "НАЧНИТЕ ЗНАКОМСТВО")}</p><div className="section-head"><h2>{t('app.s6', "История, которую можно")}<br/><i>{t('app.s7', "почувствовать")}</i></h2><p>{t('app.s8', "Каждая деталь — от изгиба уука до узора шырдака — хранит знания поколений.")}</p></div>
      <div className="feature-grid">
        <article className="feature large" onClick={() => go('yurt')}><img src={photos.valley} alt={t('app.s9', "Каркас кыргызской юрты в горах")}/><div><span>{t('app.s10', "ИНТЕРАКТИВНАЯ ЭКСПОЗИЦИЯ")}</span><h3>{t('app.s11', "Внутри кыргызской юрты")}</h3><p>{t('app.s12', "Разберите традиционное жилище на элементы и узнайте смысл каждого.")}</p><Button>{t('app.s13', "Открыть экспозицию")}</Button></div></article>
        <article className="feature burgundy" onClick={() => go('traditions')}><span>{t('app.s14', "КУЛЬТУРНЫЙ АРХИВ")}</span><div className="ornament">✦</div><h3>{t('app.s15', "Традиции,")}<br/>{t('app.s16', "объединяющие")}<br/>{t('app.s17', "поколения")}</h3><p>{t('app.s18', "Обряды рождения, взросления, семьи и гостеприимства.")}</p><button className="round"><Arrow/></button></article>
      </div>
    </section>
    <section className="quote"><span>{t('app.s19', "МУРАС")}</span><blockquote>{t('app.s20', "«Наследие живёт,")}<br/>{t('app.s21', "пока о нём")}{' '}<i>{t('app.s22', "помнят")}</i>»</blockquote><p>{t('app.s23', "Сохраняем знания прошлого, чтобы передать их будущему.")}</p></section>
  </main>
}

function Lens({ k, className = 'lens' }: { k: PartKey; className?: string }) {
  const z = 3.2, part = yurtParts[k]
  const pos = (v: number) => Math.min(1, Math.max(0, (v * z - .5) / (z - 1)))
  const px = pos(part.x), py = pos(part.y)
  const mx = (part.x * z - px * (z - 1)) * 100, my = (part.y * z - py * (z - 1)) * 100
  return <div className={className} role="img" aria-label={tf('app.s24', "Фрагмент юрты: {0}", part.name)}
    style={{ backgroundImage: `url(${YURT_IMAGE})`, backgroundSize: `${z * 100}% auto`, backgroundPosition: `${px * 100}% ${py * 100}%` }}>
    <i style={{ left: `${mx}%`, top: `${my}%` }}/>
  </div>
}

function Yurt({ go, selected, setSelected, user, openNode }: { go: (p: Page) => void; selected: PartKey; setSelected: (k: PartKey) => void; user: User | null; openNode: (n: RelationNode) => void }) {
  const [panelOpen, setPanelOpen] = useState(true)
  const links = useRelationLinks('yurt', selected)
  const part = yurtParts[selected]
  // Связи (FR-3.3): обряды, герои и произведения, упомянутые в карточке, - ссылками
  const linker = new Linker(links ?? [], openNode)
  const idx = partKeys.indexOf(selected)
  // FR-5.3: +2 за первое открытие элемента, значок «Хозяин юрты»
  const pick = (k: PartKey) => { setSelected(k); setPanelOpen(true); trackView(user, 'YURT_PART', k) }
  // элемент, открытый при входе на страницу, тоже считается открытым
  useEffect(() => { trackView(user, 'YURT_PART', selected) }, [user]) // eslint-disable-line react-hooks/exhaustive-deps
  const step = (d: number) => pick(partKeys[(idx + d + partKeys.length) % partKeys.length])
  const byName = (name: string) => partKeys.find(k => yurtParts[k].name === name)
  return <main className="museum-page"><div className="page-title"><p className="kicker">{t('app.s25', "ИНТЕРАКТИВНАЯ ЭКСПОЗИЦИЯ · 01")}</p><h1>{t('app.s26', "Внутри кыргызской")}{' '}<i>{t('app.s27', "юрты")}</i></h1><p>{t('app.s28', "Нажмите на точку на схеме, чтобы узнать устройство, назначение и смысл каждого элемента боз үй.")}</p></div>
    <section className="exhibit"><div className="exhibit-stage"><div className="stage-top"><span>{t('app.s29', "БОЗ ҮЙ · ОСНОВНЫЕ ЭЛЕМЕНТЫ КОНСТРУКЦИИ")}</span><span>{String(part.no).padStart(2, '0')} / {partKeys.length}</span></div>
      <AuthenticYurtDiagram selected={selected} select={pick}/>
      <div className="size-strip"><p>{yurtSize.title}</p><div><span>{t('app.s30', "Высота в центре")}<b>{yurtSize.height}</b></span><span>{t('app.s31', "Высота кереге")}<b>{yurtSize.keregeHeight}</b></span><span>{t('app.s32', "Диаметр")}<b>{yurtSize.diameter}</b></span></div></div>
    </div>
      <aside className={panelOpen ? 'info-panel' : 'info-panel panel-closed'} aria-live="polite"><button className="close" onClick={() => setPanelOpen(false)} aria-label={t('app.s33', "Закрыть")}>×</button>{panelOpen ? <>
        <p className="kicker"><span className="no">{String(part.no).padStart(2, '0')}</span>{part.group} · {part.eyebrow}</p><h2>{part.name}</h2><p className="info-intro">{linker.text(part.description)}</p>
        <Lens k={selected}/>
        <dl><div><dt>{t('app.s34', "Назначение")}</dt><dd>{linker.text(part.purpose)}</dd></div><div><dt>{t('app.s35', "Культурное значение")}</dt><dd>{linker.text(part.meaning)}</dd></div><div><dt>{t('app.s36', "Связанные элементы")}</dt><dd className="chips">{part.related.map(r => { const k = byName(r); return k ? <button key={r} onClick={() => pick(k)}>{r}</button> : <span key={r}>{r}</span> })}</dd></div></dl>
        <SeeAlso linker={linker} compact />
        <div className="panel-nav"><button onClick={() => step(-1)} aria-label={t('app.s37', "Предыдущий элемент")}>←</button><Button onClick={() => go('yurt-detail')}>{t('app.s38', "Подробнее об элементе")}</Button><button onClick={() => step(1)} aria-label={t('app.s39', "Следующий элемент")}>→</button></div></>
        : <button className="reopen-panel" onClick={() => setPanelOpen(true)}>{t('app.s40', "Открыть карточку")}<br/><b>{part.name}</b></button>}
      </aside></section>
  </main>
}

function YurtDetail({ go, selected, openNode }: { go: (p: Page) => void; selected: PartKey; openNode: (n: RelationNode) => void }) {
  const part = yurtParts[selected]
  const linker = new Linker(useRelationLinks('yurt', selected) ?? [], openNode)
  return <main className="article-page"><button className="back" onClick={() => go('yurt')}>{t('app.s41', "← Вернуться к экспозиции")}</button>
    <div className="article-hero detail-hero"><Lens k={selected} className="lens hero-lens"/><div><p className="kicker">{t('app.s42', "ЭЛЕМЕНТ ЮРТЫ ·")}{' '}{part.group.toUpperCase()}</p><h1>{part.name}</h1><p>{part.eyebrow}</p></div></div>
    <div className="article-body"><aside><b>{t('app.s43', "КЫРГЫЗСКОЕ НАЗВАНИЕ")}</b><strong>{part.name}</strong><span>{t('app.s44', "Элемент")}{' '}{part.no}{' '}{t('app.s45', "из")}{' '}{partKeys.length}</span></aside>
      <article><p className="big">{linker.text(part.description)}</p><h2>{t('app.s46', "Назначение и устройство")}</h2><p>{linker.text(part.purpose)}</p><h2>{t('app.s35', "Культурное значение")}</h2><p>{linker.text(part.meaning)}</p>
        <div className="materials"><b>{t('app.s36', "Связанные элементы")}</b>{part.related.map(r => <span key={r}>{r}</span>)}</div>
        <SeeAlso linker={linker} /></article></div></main>
}

export default function App() {
  const [page,setPage]=useState<Page>(()=>new URLSearchParams(window.location.search).has('reset')?'reset':'home')
  const [resetToken]=useState(()=>new URLSearchParams(window.location.search).get('reset')??'')
  const [user,setUser]=useState<User|null>(null)
  useEffect(()=>{fetchMe().then(setUser).catch(()=>setUser(null))},[])
  // FR-5.3: ежедневный вход (+5 раз в сутки), «Первый шаг», серии входов
  const userId=user?.id
  useEffect(()=>{if(userId!==undefined)postVisit().then(emitReward).catch(()=>{})},[userId])
  useEffect(()=>{initLanguages()},[])
  // FR-8.2: при смене языка ничего не пересоздаётся - страницы сами перезапрашивают данные на новом языке,
  // поэтому остаются открытый раздел, выбранный обряд или герой, фильтры, поиск и место прокрутки
  useI18n()
  const doLogout=async()=>{try{await logout()}catch{/* всё равно считаем вышедшей */}finally{setUser(null);go('login')}}
  const [part,setPart]=useState<PartKey>('tunduk')
  const [riteId,setRiteId]=useState<number|null>(null)
  const [folkloreRef,setFolkloreRef]=useState<FolkloreRef|null>(null)
  const go=(p:Page)=>{setPage(p);window.scrollTo({top:0,behavior:'smooth'})}
  // Переход по связи (FR-3.3) в нужный раздел
  const openNode=(n:RelationNode)=>{
    if(n.kind==='rite'&&n.id!==null){setRiteId(n.id);go('tradition-detail')}
    else if((n.kind==='hero'||n.kind==='work')&&n.id!==null){setFolkloreRef({kind:n.kind,id:n.id});go('folklore-detail')}
    else if(n.kind==='yurt'&&n.key&&(partKeys as string[]).includes(n.key)){setPart(n.key as PartKey);go('yurt')}
  }
  // Выбран вариант в поиске (FR-4.1) - туда же
  const openFound=(r:SearchResult)=>openNode({kind:r.kind,id:r.id,key:r.key,title:r.title,subtitle:r.subtitle,via:null})
  const content=page==='home'?<Home go={go}/>:page==='yurt'?<Yurt go={go} selected={part} setSelected={setPart} user={user} openNode={openNode}/>:page==='yurt-detail'?<YurtDetail go={go} selected={part} openNode={openNode}/>:page==='traditions'?<Traditions openRite={id=>{setRiteId(id);go('tradition-detail')}}/>:page==='tradition-detail'?<TraditionDetail go={go} riteId={riteId} openNode={openNode} user={user}/>:page==='folklore'?<Folklore openItem={r=>{setFolkloreRef(r);go('folklore-detail')}}/>:page==='folklore-detail'?<FolkloreDetail go={go} item={folkloreRef} openNode={openNode} user={user}/>:page==='quizzes'?<Quizzes user={user} go={go}/>:page==='rating'?<Rating user={user} goLogin={()=>go('login')}/>:page==='cabinet'?<Cabinet user={user} onUser={setUser} go={go} openRite={id=>{setRiteId(id);go('tradition-detail')}} openItem={r=>{setFolkloreRef(r);go('folklore-detail')}}/>:page==='editor'?<Editor user={user} goLogin={()=>go('login')} openRite={id=>{setRiteId(id);go('tradition-detail')}} openFolklore={r=>{setFolkloreRef(r);go('folklore-detail')}}/>:page==='admin'?<Admin user={user} goLogin={()=>go('login')}/>:<Auth mode={page as 'login'|'register'|'recover'|'reset'} go={go} onUser={setUser} resetToken={resetToken}/>
  const auth=['login','register','recover','reset'].includes(page)
  return <div>{!auth&&<Header page={page} go={go} user={user} onLogout={doLogout} onFound={openFound}/>} {content} {!auth&&<Footer go={go}/>}<RewardToasts/></div>
}
