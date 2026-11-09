import { ReactNode, useEffect, useState } from 'react'
import { photos } from './data'
import { Traditions, TraditionDetail } from './Traditions'
import { Auth } from './Auth'
import { Folklore, FolkloreDetail } from './Folklore'
import type { FolkloreRef } from './Folklore'
import { canEditContent, fetchMe, logout } from './api'
import { Editor } from './Editor'
import type { User } from './api'
import AuthenticYurtDiagram from './AuthenticYurtDiagram'
import { PartKey, partKeys, YURT_IMAGE, yurtParts, yurtSize } from './yurt'

type Page = 'home' | 'yurt' | 'yurt-detail' | 'traditions' | 'tradition-detail' | 'folklore' | 'folklore-detail' | 'editor' | 'login' | 'register' | 'recover' | 'reset'

const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6"/></svg>
const Eye = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/></svg>

function Logo({ onClick }: { onClick: () => void }) {
  return <button className="logo" onClick={onClick} aria-label="На главную"><span className="logo-mark">М</span><span><b>MURAS</b><small>INTERACTIVE</small></span></button>
}

function Header({ page, go, user, onLogout }: { page: Page; go: (p: Page) => void; user: User | null; onLogout: () => void }) {
  const [open, setOpen] = useState(false)
  const nav = [{label:'Главная',p:'home'}, {label:'Юрта',p:'yurt'}, {label:'Традиции',p:'traditions'}, {label:'Фольклор',p:'folklore'}] as const
  return <header className="header"><Logo onClick={() => go('home')} />
    <button className="menu-btn" onClick={() => setOpen(!open)} aria-label="Открыть меню"><i/><i/></button>
    <nav className={open ? 'nav open' : 'nav'}>
      {nav.map(n => <button key={n.p} className={page === n.p ? 'active' : ''} onClick={() => {go(n.p);setOpen(false)}}>{n.label}</button>)}
      <button className="soon">Викторины <em>Скоро</em></button>
      {canEditContent(user) && <button className={page === 'editor' ? 'active nav-editor' : 'nav-editor'} onClick={() => {go('editor');setOpen(false)}}>Редактор</button>}
      {user ? <><span style={{fontSize:13,color:'var(--wine)'}}>{user.displayName}</span><button className="login-link" onClick={onLogout}>Выйти <Arrow/></button></> : <button className="login-link" onClick={() => go('login')}>Войти <Arrow/></button>}
    </nav>
  </header>
}

function Button({ children, secondary, onClick, type='button' }: { children: ReactNode; secondary?: boolean; onClick?: () => void; type?: 'button'|'submit' }) {
  return <button type={type} onClick={onClick} className={secondary ? 'btn secondary' : 'btn'}>{children}<Arrow/></button>
}

function Footer({ go }: { go: (p: Page) => void }) {
  return <footer><div><Logo onClick={() => go('home')} /><p>Цифровой музей живого наследия Кыргызстана.</p></div><div className="footer-links"><button onClick={() => go('yurt')}>Исследовать юрту</button><button onClick={() => go('traditions')}>Традиции</button><span>© 2025 Muras Interactive</span></div></footer>
}

function Home({ go }: { go: (p: Page) => void }) {
  return <main>
    <section className="hero" style={{backgroundImage:`linear-gradient(90deg,rgba(25,17,14,.76),rgba(25,17,14,.1)),url(${photos.hero})`}}>
      <div className="hero-copy"><p className="kicker light">ЦИФРОВОЙ МУЗЕЙ КЫРГЫЗСТАНА</p><h1>Открой наследие<br/><i>Кыргызстана</i></h1><p className="lead">Исследуйте устройство юрты, древние обряды и живые традиции народа через современный интерактивный музей.</p><div className="actions"><Button onClick={() => go('yurt')}>Исследовать юрту</Button><Button secondary onClick={() => go('traditions')}>Узнать о традициях</Button></div></div>
      <div className="hero-note"><span>01</span><p>Живое наследие<br/>в цифровом пространстве</p></div>
    </section>
    <section className="intro"><p className="kicker">НАЧНИТЕ ЗНАКОМСТВО</p><div className="section-head"><h2>История, которую можно<br/><i>почувствовать</i></h2><p>Каждая деталь — от изгиба уука до узора шырдака — хранит знания поколений.</p></div>
      <div className="feature-grid">
        <article className="feature large" onClick={() => go('yurt')}><img src={photos.valley} alt="Каркас кыргызской юрты в горах"/><div><span>ИНТЕРАКТИВНАЯ ЭКСПОЗИЦИЯ</span><h3>Внутри кыргызской юрты</h3><p>Разберите традиционное жилище на элементы и узнайте смысл каждого.</p><Button>Открыть экспозицию</Button></div></article>
        <article className="feature burgundy" onClick={() => go('traditions')}><span>КУЛЬТУРНЫЙ АРХИВ</span><div className="ornament">✦</div><h3>Традиции,<br/>объединяющие<br/>поколения</h3><p>Обряды рождения, взросления, семьи и гостеприимства.</p><button className="round"><Arrow/></button></article>
      </div>
    </section>
    <section className="quote"><span>МУРАС</span><blockquote>«Наследие живёт,<br/>пока о нём <i>помнят</i>»</blockquote><p>Сохраняем знания прошлого, чтобы передать их будущему.</p></section>
  </main>
}

function Lens({ k, className = 'lens' }: { k: PartKey; className?: string }) {
  const z = 3.2, part = yurtParts[k]
  const pos = (v: number) => Math.min(1, Math.max(0, (v * z - .5) / (z - 1)))
  const px = pos(part.x), py = pos(part.y)
  const mx = (part.x * z - px * (z - 1)) * 100, my = (part.y * z - py * (z - 1)) * 100
  return <div className={className} role="img" aria-label={`Фрагмент юрты: ${part.name}`}
    style={{ backgroundImage: `url(${YURT_IMAGE})`, backgroundSize: `${z * 100}% auto`, backgroundPosition: `${px * 100}% ${py * 100}%` }}>
    <i style={{ left: `${mx}%`, top: `${my}%` }}/>
  </div>
}

function Yurt({ go, selected, setSelected }: { go: (p: Page) => void; selected: PartKey; setSelected: (k: PartKey) => void }) {
  const [panelOpen, setPanelOpen] = useState(true)
  const part = yurtParts[selected]
  const idx = partKeys.indexOf(selected)
  const pick = (k: PartKey) => { setSelected(k); setPanelOpen(true) }
  const step = (d: number) => pick(partKeys[(idx + d + partKeys.length) % partKeys.length])
  const byName = (name: string) => partKeys.find(k => yurtParts[k].name === name)
  return <main className="museum-page"><div className="page-title"><p className="kicker">ИНТЕРАКТИВНАЯ ЭКСПОЗИЦИЯ · 01</p><h1>Внутри кыргызской <i>юрты</i></h1><p>Нажмите на точку на схеме, чтобы узнать устройство, назначение и смысл каждого элемента боз үй.</p></div>
    <section className="exhibit"><div className="exhibit-stage"><div className="stage-top"><span>БОЗ ҮЙ · ОСНОВНЫЕ ЭЛЕМЕНТЫ КОНСТРУКЦИИ</span><span>{String(part.no).padStart(2, '0')} / {partKeys.length}</span></div>
      <AuthenticYurtDiagram selected={selected} select={pick}/>
      <div className="size-strip"><p>{yurtSize.title}</p><div><span>Высота в центре<b>{yurtSize.height}</b></span><span>Высота кереге<b>{yurtSize.keregeHeight}</b></span><span>Диаметр<b>{yurtSize.diameter}</b></span></div></div>
    </div>
      <aside className={panelOpen ? 'info-panel' : 'info-panel panel-closed'} aria-live="polite"><button className="close" onClick={() => setPanelOpen(false)} aria-label="Закрыть">×</button>{panelOpen ? <>
        <p className="kicker"><span className="no">{String(part.no).padStart(2, '0')}</span>{part.group} · {part.eyebrow}</p><h2>{part.name}</h2><p className="info-intro">{part.description}</p>
        <Lens k={selected}/>
        <dl><div><dt>Назначение</dt><dd>{part.purpose}</dd></div><div><dt>Культурное значение</dt><dd>{part.meaning}</dd></div><div><dt>Связанные элементы</dt><dd className="chips">{part.related.map(r => { const k = byName(r); return k ? <button key={r} onClick={() => pick(k)}>{r}</button> : <span key={r}>{r}</span> })}</dd></div></dl>
        <div className="panel-nav"><button onClick={() => step(-1)} aria-label="Предыдущий элемент">←</button><Button onClick={() => go('yurt-detail')}>Подробнее об элементе</Button><button onClick={() => step(1)} aria-label="Следующий элемент">→</button></div></>
        : <button className="reopen-panel" onClick={() => setPanelOpen(true)}>Открыть карточку<br/><b>{part.name}</b></button>}
      </aside></section>
  </main>
}

function YurtDetail({ go, selected }: { go: (p: Page) => void; selected: PartKey }) {
  const part = yurtParts[selected]
  return <main className="article-page"><button className="back" onClick={() => go('yurt')}>← Вернуться к экспозиции</button>
    <div className="article-hero detail-hero"><Lens k={selected} className="lens hero-lens"/><div><p className="kicker">ЭЛЕМЕНТ ЮРТЫ · {part.group.toUpperCase()}</p><h1>{part.name}</h1><p>{part.eyebrow}</p></div></div>
    <div className="article-body"><aside><b>КЫРГЫЗСКОЕ НАЗВАНИЕ</b><strong>{part.name}</strong><span>Элемент {part.no} из {partKeys.length}</span></aside>
      <article><p className="big">{part.description}</p><h2>Назначение и устройство</h2><p>{part.purpose}</p><h2>Культурное значение</h2><p>{part.meaning}</p>
        <div className="materials"><b>Связанные элементы</b>{part.related.map(r => <span key={r}>{r}</span>)}</div></article></div></main>
}

export default function App() {
  const [page,setPage]=useState<Page>(()=>new URLSearchParams(window.location.search).has('reset')?'reset':'home')
  const [resetToken]=useState(()=>new URLSearchParams(window.location.search).get('reset')??'')
  const [user,setUser]=useState<User|null>(null)
  useEffect(()=>{fetchMe().then(setUser).catch(()=>setUser(null))},[])
  const doLogout=async()=>{try{await logout()}catch{/* всё равно считаем вышедшей */}finally{setUser(null);go('home')}}
  const [part,setPart]=useState<PartKey>('tunduk')
  const [riteId,setRiteId]=useState<number|null>(null)
  const [folkloreRef,setFolkloreRef]=useState<FolkloreRef|null>(null)
  const go=(p:Page)=>{setPage(p);window.scrollTo({top:0,behavior:'smooth'})}
  const content=page==='home'?<Home go={go}/>:page==='yurt'?<Yurt go={go} selected={part} setSelected={setPart}/>:page==='yurt-detail'?<YurtDetail go={go} selected={part}/>:page==='traditions'?<Traditions openRite={id=>{setRiteId(id);go('tradition-detail')}}/>:page==='tradition-detail'?<TraditionDetail go={go} riteId={riteId}/>:page==='folklore'?<Folklore openItem={r=>{setFolkloreRef(r);go('folklore-detail')}}/>:page==='folklore-detail'?<FolkloreDetail go={go} item={folkloreRef} openItem={setFolkloreRef}/>:page==='editor'?<Editor user={user} goLogin={()=>go('login')} openRite={id=>{setRiteId(id);go('tradition-detail')}}/>:<Auth mode={page as 'login'|'register'|'recover'|'reset'} go={go} onUser={setUser} resetToken={resetToken}/>
  const auth=['login','register','recover','reset'].includes(page)
  return <div>{!auth&&<Header page={page} go={go} user={user} onLogout={doLogout}/>} {content} {!auth&&<Footer go={go}/>}</div>
}
