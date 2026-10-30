import { KeyboardEvent, useState } from 'react'
import { PartKey, partKeys, YURT_IMAGE, yurtParts, yurtSize } from './yurt'

// Холст схемы: изображение 1000×731 со смещением, вокруг — поля для размерных линий
const OX = 80, OY = 24, IW = 1000, IH = 731, VW = 1150, VH = 815
const sx = (x: number) => OX + x * IW
const sy = (y: number) => OY + y * IH

function Hotspot({ k, active, select }: { k: PartKey; active: boolean; select: (k: PartKey) => void }) {
  const part = yurtParts[k]
  const x = sx(part.x), y = sy(part.y)
  const left = part.x > 0.72
  const w = part.name.length * 8.6 + 26
  const onKey = (e: KeyboardEvent) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); select(k) } }
  return <g className={active ? 'hs active' : 'hs'} transform={`translate(${x} ${y})`} tabIndex={0} role="button"
    aria-label={`${part.no}. ${part.name}`} aria-pressed={active} onClick={() => select(k)} onKeyDown={onKey}>
    <g className="hs-in">
    <circle className="hs-hit" r="24"/>
    <circle className="hs-pulse" r="14"/>
    <circle className="hs-dot" r="9"/>
    <g className="hs-tip" transform={`translate(${left ? -w - 18 : 18} -15)`}>
      <rect width={w} height="30" rx="15"/>
      <text x={w / 2} y="20">{part.name}</text>
    </g>
    </g>
  </g>
}

function Dimensions() {
  const top = sy(36 / 731), base = sy(686 / 731)
  const kTop = sy(386 / 731), kBot = sy(578 / 731)
  const dLeft = sx(40 / 1000), dRight = sx(902 / 1000)
  const hx = VW - 40, kx = 34, dy = VH - 22
  return <g className="dims" aria-hidden="true">
    {/* высота юрты */}
    <path className="ext" d={`M${sx(.52)} ${top}H${hx + 8}M${sx(.6)} ${base}H${hx + 8}`}/>
    <path className="dim" d={`M${hx} ${top}V${base}`} markerStart="url(#tick)" markerEnd="url(#tick)"/>
    <g transform={`translate(${hx} ${(top + base) / 2}) rotate(-90)`}><rect x="-86" y="-14" width="172" height="28" rx="14"/><text y="5">Высота · {yurtSize.height}</text></g>
    {/* высота кереге */}
    <path className="ext" d={`M${sx(.045)} ${kTop}H${kx - 8}M${sx(.058)} ${kBot}H${kx - 8}`}/>
    <path className="dim" d={`M${kx} ${kTop}V${kBot}`} markerStart="url(#tick)" markerEnd="url(#tick)"/>
    <g transform={`translate(${kx} ${(kTop + kBot) / 2}) rotate(-90)`}><rect x="-70" y="-14" width="140" height="28" rx="14"/><text y="5">Кереге · {yurtSize.keregeHeight}</text></g>
    {/* диаметр */}
    <path className="ext" d={`M${dLeft} ${sy(.79)}V${dy + 8}M${dRight} ${sy(.8)}V${dy + 8}`}/>
    <path className="dim" d={`M${dLeft} ${dy}H${dRight}`} markerStart="url(#tick)" markerEnd="url(#tick)"/>
    <g transform={`translate(${(dLeft + dRight) / 2} ${dy})`}><rect x="-92" y="-14" width="184" height="28" rx="14"/><text y="5">Диаметр · {yurtSize.diameter}</text></g>
  </g>
}

export default function AuthenticYurtDiagram({ selected, select }: { selected: PartKey; select: (part: PartKey) => void }) {
  const [dims, setDims] = useState(true)
  return <div className="diagram-wrap">
    <div className="diagram-tools">
      <span>15 элементов · нажмите на точку</span>
      <button className={dims ? 'active' : ''} aria-pressed={dims} onClick={() => setDims(!dims)}>
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7v10M20 7v10M4 12h16M7 9l-3 3 3 3M17 9l3 3-3 3"/></svg>Размеры
      </button>
    </div>
    <svg className="yurt-svg yurt-main" viewBox={`0 0 ${VW} ${VH}`} role="group" aria-label="Интерактивная схема кыргызской юрты: 15 элементов конструкции">
      <defs>
        <marker id="tick" viewBox="-6 -6 12 12" markerWidth="12" markerHeight="12" orient="auto"><path d="M-4 4L4 -4" className="tick"/></marker>
      </defs>
      <image href={YURT_IMAGE} x={OX} y={OY} width={IW} height={IH} preserveAspectRatio="xMidYMid meet"/>
      {dims && <Dimensions/>}
      {partKeys.filter(k => k !== selected).map(k => <Hotspot key={k} k={k} active={false} select={select}/>)}
      <Hotspot k={selected} active select={select}/>
    </svg>
  </div>
}
