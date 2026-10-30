import { FormEvent, ReactNode, useState } from 'react'
import { photos, traditions } from './data'
import AuthenticYurtDiagram from './AuthenticYurtDiagram'
import { PartKey, partKeys, YURT_IMAGE, yurtParts, yurtSize } from './yurt'

type Page = 'home' | 'yurt' | 'yurt-detail' | 'traditions' | 'tradition-detail' | 'login' | 'register' | 'recover'

const Arrow = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 6l6 6-6 6"/></svg>
const Eye = () => <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/></svg>

function Logo({ onClick }: { onClick: () => void }) {
  return <button className="logo" onClick={onClick} aria-label="На главную"><span className="logo-mark">М</span><span><b>MURAS</b><small>INTERACTIVE</small></span></button>
}

function Header({ page, go }: { page: Page; go: (p: Page) => void }) {
  const [open, setOpen] = useState(false)
  const nav = [{label:'Главная',p:'home'}, {label:'Юрта',p:'yurt'}, {label:'Традиции',p:'traditions'}] as const
  return <header className="header"><Logo onClick={() => go('home')} />
    <button className="menu-btn" onClick={() => setOpen(!open)} aria-label="Открыть меню"><i/><i/></button>
    <nav className={open ? 'nav open' : 'nav'}>
      {nav.map(n => <button key={n.p} className={page === n.p ? 'active' : ''} onClick={() => {go(n.p);setOpen(false)}}>{n.label}</button>)}
      <button className="soon">Фольклор <em>Скоро</em></button><button className="soon">Викторины <em>Скоро</em></button>
      <button className="login-link" onClick={() => go('login')}>Войти <Arrow/></button>
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

function Traditions({ go }: { go: (p: Page) => void }) {
  const cats=['Все традиции','Свадебные','Рождение ребёнка','Похоронные и поминальные','Праздничные','Бытовые']; const [cat,setCat]=useState('Все традиции')
  const shown=cat==='Все традиции'?traditions:traditions.filter(t=>t.category===cat)
  return <main className="catalog"><div className="page-title"><p className="kicker">КУЛЬТУРНЫЙ АРХИВ · 02</p><h1>Традиции и <i>обряды</i></h1><p>Живые ритуалы, сопровождающие человека от рождения и объединяющие кыргызские семьи.</p></div><div className="filters">{cats.map(c=><button className={cat===c?'active':''} onClick={()=>setCat(c)} key={c}>{c}</button>)}</div><div className="card-grid">{shown.map((t,i)=><article className="trad-card" key={t.id}><div className="image"><img src={t.image} alt={t.title}/><span>0{i+1}</span></div><p>{t.category}</p><h2>{t.title}</h2><div><span>{t.description}</span><button onClick={()=>go('tradition-detail')} aria-label={`Подробнее: ${t.title}`}><Arrow/></button></div></article>)}</div></main>
}

function TraditionDetail({ go }: { go: (p: Page) => void }) {
  return <main className="trad-detail"><button className="back" onClick={()=>go('traditions')}>← Назад в каталог</button><div className="trad-hero" style={{backgroundImage:`linear-gradient(0deg,rgba(30,18,15,.7),transparent),url(${photos.interior})`}}><div><p>РОЖДЕНИЕ РЕБЁНКА</p><h1>Бешик той</h1><span>Праздник колыбели и первое благословение ребёнка</span></div></div><div className="reading"><aside><p>Время чтения<br/><b>6 минут</b></p><p>Регион<br/><b>Весь Кыргызстан</b></p></aside><article><p className="big">Бешик той — семейное торжество, посвящённое первому укладыванию младенца в бешик, традиционную кыргызскую колыбель.</p><p>Обряд обычно проводит уважаемая старшая женщина семьи. Родные собираются, чтобы пожелать ребёнку здоровья, долгой жизни и достойного пути.</p><h2>Этапы обряда</h2><ol><li><b>Подготовка бешика</b><span>Колыбель очищают, украшают и застилают специально подготовленными принадлежностями.</span></li><li><b>Алас-алас</b><span>Проводится символическое очищение дымом арчи с добрыми пожеланиями.</span></li><li><b>Укладывание ребёнка</b><span>Опытная женщина показывает молодой матери, как безопасно уложить младенца.</span></li><li><b>Бата</b><span>Старшие произносят благословение, после чего семье преподносят подарки.</span></li></ol><h2>Культурный смысл</h2><p>Бешик воспринимается не только как предмет быта, но и как защищённое первое пространство ребёнка. Обряд подчёркивает участие рода в заботе о новом поколении.</p><div className="sources"><b>Источники</b><p>Материалы этнографических исследований Кыргызского национального музея и открытых публикаций о семейной обрядности кыргызов.</p></div></article></div></main>
}

function Auth({ mode, go }: { mode: 'login'|'register'|'recover'; go: (p: Page) => void }) {
  const [show,setShow]=useState(false), [errors,setErrors]=useState<Record<string,string>>({}), [sent,setSent]=useState(false)
  const submit=(e:FormEvent<HTMLFormElement>)=>{e.preventDefault(); const f=new FormData(e.currentTarget), err:Record<string,string>={}; const email=String(f.get('email')||''); if(!email.includes('@'))err.email='Введите корректный адрес электронной почты'; if(mode!=='recover' && String(f.get('password')||'').length<6)err.password='Пароль должен содержать не менее 6 символов'; if(mode==='register'&&f.get('password')!==f.get('confirm'))err.confirm='Пароли не совпадают'; setErrors(err); if(!Object.keys(err).length&&mode==='recover')setSent(true)}
  const titles={login:['С возвращением','Войдите, чтобы продолжить путешествие по наследию.'],register:['Создайте аккаунт','Сохраняйте материалы и продолжайте исследование.'],recover:['Восстановление доступа','Мы отправим инструкции на вашу электронную почту.']}
  return <main className="auth"><div className="auth-image" style={{backgroundImage:`linear-gradient(0deg,rgba(50,20,22,.72),rgba(50,20,22,.05)),url(${photos.hero})`}}><Logo onClick={()=>go('home')}/><blockquote>«То, что мы сохраняем сегодня, становится памятью <i>завтра</i>»</blockquote></div><section className="auth-form"><button className="back" onClick={()=>go('home')}>← На главную</button><div><p className="kicker">MURAS INTERACTIVE</p><h1>{titles[mode][0]}</h1><p>{titles[mode][1]}</p>
    {sent?<div className="success"><b>Письмо отправлено</b><p>Если аккаунт с таким адресом существует, инструкции уже в пути.</p><Button onClick={()=>go('login')}>Вернуться ко входу</Button></div>:<form onSubmit={submit} noValidate>
      {mode==='register'&&<label>Отображаемое имя<input name="name" required placeholder="Как к вам обращаться?"/></label>}
      <label>Электронная почта<input name="email" type="email" className={errors.email?'invalid':''} placeholder="name@example.com"/>{errors.email&&<small>{errors.email}</small>}</label>
      {mode!=='recover'&&<label>Пароль<div className="password"><input name="password" type={show?'text':'password'} className={errors.password?'invalid':''} placeholder="Не менее 6 символов"/><button type="button" onClick={()=>setShow(!show)} aria-label="Показать пароль"><Eye/></button></div>{errors.password&&<small>{errors.password}</small>}</label>}
      {mode==='register'&&<label>Подтвердите пароль<input name="confirm" type={show?'text':'password'} className={errors.confirm?'invalid':''} placeholder="Повторите пароль"/>{errors.confirm&&<small>{errors.confirm}</small>}</label>}
      {mode==='login'&&<button type="button" className="forgot" onClick={()=>go('recover')}>Забыли пароль?</button>}
      <Button type="submit">{mode==='login'?'Войти':mode==='register'?'Зарегистрироваться':'Отправить инструкции'}</Button>
    </form>}
    {!sent&&<p className="switch">{mode==='login'?'Ещё нет аккаунта?':mode==='register'?'Уже есть аккаунт?':'Вспомнили пароль?'} <button onClick={()=>go(mode==='login'?'register':'login')}>{mode==='login'?'Зарегистрироваться':'Войти'}</button></p>}
  </div></section></main>
}

export default function App() {
  const [page,setPage]=useState<Page>('home')
  const [part,setPart]=useState<PartKey>('tunduk')
  const go=(p:Page)=>{setPage(p);window.scrollTo({top:0,behavior:'smooth'})}
  const content=page==='home'?<Home go={go}/>:page==='yurt'?<Yurt go={go} selected={part} setSelected={setPart}/>:page==='yurt-detail'?<YurtDetail go={go} selected={part}/>:page==='traditions'?<Traditions go={go}/>:page==='tradition-detail'?<TraditionDetail go={go}/>:<Auth mode={page as 'login'|'register'|'recover'} go={go}/>
  const auth=['login','register','recover'].includes(page)
  return <div>{!auth&&<Header page={page} go={go}/>} {content} {!auth&&<Footer go={go}/>}</div>
}
