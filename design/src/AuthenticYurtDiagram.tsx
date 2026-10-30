import { useState } from 'react'

type Part = 'tunduk' | 'uuk' | 'kerege' | 'eshik' | 'shyrdak'

export default function AuthenticYurtDiagram({ selected, select }: { selected: Part; select: (part: Part) => void }) {
  const [view, setView] = useState<'exterior' | 'structure'>('exterior')
  const state = (part: Part) => selected === part ? 'hot svg-part active' : 'hot svg-part'

  return <div className="diagram-wrap">
    <div className="view-tabs" role="tablist" aria-label="Вид юрты">
      <button role="tab" aria-selected={view === 'exterior'} className={view === 'exterior' ? 'active' : ''} onClick={() => setView('exterior')}>Внешний вид</button>
      <button role="tab" aria-selected={view === 'structure'} className={view === 'structure' ? 'active' : ''} onClick={() => setView('structure')}>Конструкция</button>
    </div>
    {view === 'exterior' ? <svg className="yurt-svg authentic-yurt" viewBox="0 0 900 590" role="img" aria-label="Внешний вид традиционной кыргызской боз үй">
      <image href="/yurt-exterior.svg" x="15" y="5" width="870" height="580" preserveAspectRatio="xMidYMid meet"/>
      <g onClick={() => select('tunduk')} className={state('tunduk')} aria-label="Түндүк">
        <ellipse cx="450" cy="128" rx="95" ry="58" fill="transparent"/>
      </g>
      <g onClick={() => select('uuk')} className={state('uuk')} aria-label="Уук">
        <path d="M145 350Q205 185 450 125Q695 185 755 350" fill="none" stroke="transparent" strokeWidth="105"/>
      </g>
      <g onClick={() => select('eshik')} className={state('eshik')} aria-label="Эшик">
        <rect x="374" y="330" width="152" height="218" fill="transparent"/>
      </g>
    </svg> : <svg className="yurt-svg authentic-yurt" viewBox="0 0 900 590" role="img" aria-label="Деревянная конструкция традиционной кыргызской боз үй">
      <image href="/yurt-structure.svg" x="15" y="5" width="870" height="580" preserveAspectRatio="xMidYMid meet"/>
      <g onClick={() => select('tunduk')} className={state('tunduk')} aria-label="Түндүк">
        <ellipse cx="450" cy="120" rx="100" ry="62" fill="transparent"/>
      </g>
      <g onClick={() => select('uuk')} className={state('uuk')} aria-label="Уук">
        <path d="M140 350Q205 178 450 116Q695 178 760 350" fill="none" stroke="transparent" strokeWidth="110"/>
      </g>
      <g onClick={() => select('kerege')} className={state('kerege')} aria-label="Кереге">
        <path d="M105 342H367V545H105ZM533 342H795V545H533Z" fill="transparent"/>
      </g>
      <g onClick={() => select('eshik')} className={state('eshik')} aria-label="Эшик">
        <rect x="367" y="325" width="166" height="225" fill="transparent"/>
      </g>
    </svg>}
    <div className="interior-access"><button onClick={() => select('shyrdak')} className={selected === 'shyrdak' ? 'active' : ''}><span className="shyrdak-swatch"/>Интерьер: Шырдак</button><span>Ковёр расположен внутри юрты</span></div>
    <div className="drag-hint">Нажмите на элемент конструкции или подпись</div>
  </div>
}

function Label({ x1, y1, x2, textX, text }: { x1:number; y1:number; x2:number; textX:number; text:string }) {
  return <g className="museum-label"><path d={`M${x1} ${y1}H${x2}`}/><circle cx={x1} cy={y1} r="7"/><text x={textX} y={y1 + 6}>{text}</text></g>
}
