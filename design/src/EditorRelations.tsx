import { t } from './i18n'
import { useEffect, useState } from 'react'
import { editorRelationOptions } from './api'
import type { RelationOption, RelationOptions } from './api'

/**
 * Блок «Связи» в форме обряда (FR-3.3): с какими элементами юрты, героями и произведениями связан обряд.
 * Обратные связи (у героя - его обряды, у элемента юрты - обряды) сайт строит сам.
 */
export function RelationsField({ yurtElementKeys, heroIds, workIds, onChange }: {
  yurtElementKeys: string[]; heroIds: number[]; workIds: number[]
  onChange: (next: { yurtElementKeys?: string[]; heroIds?: number[]; workIds?: number[] }) => void
}) {
  const [options, setOptions] = useState<RelationOptions | null>(null)
  const [failed, setFailed] = useState(false)
  useEffect(() => { editorRelationOptions().then(setOptions).catch(() => setFailed(true)) }, [])

  const toggle = <T,>(list: T[], value: T) => list.includes(value) ? list.filter(x => x !== value) : [...list, value]
  const total = yurtElementKeys.length + heroIds.length + workIds.length

  return <fieldset className="steps rel-pick">
    <legend>{t('editorRelations.s1', "Связи с другими разделами")}{' '}<span>{total}</span></legend>
    <p className="hint-text">{t('editorRelations.s2', "Отмеченные материалы появятся в блоке «Связи» на странице обряда, а обряд — на их страницах.")}</p>
    {failed && <p className="hint-text">{t('editorRelations.s3', "Не удалось загрузить варианты связей.")}</p>}
    {!options && !failed && <p className="hint-text" role="status">{t('editorRelations.s4', "Загрузка…")}</p>}
    {options && <>
      <Group title={t('editorRelations.s5', "Элементы юрты")} kind="yurt" items={options.yurtElements}
        isOn={o => yurtElementKeys.includes(o.key ?? '')} onToggle={o => onChange({ yurtElementKeys: toggle(yurtElementKeys, o.key ?? '') })} />
      <Group title={t('editorRelations.s6', "Герои")} kind="hero" items={options.heroes}
        isOn={o => heroIds.includes(o.id ?? -1)} onToggle={o => onChange({ heroIds: toggle(heroIds, o.id ?? -1) })} />
      <Group title={t('editorRelations.s7', "Произведения")} kind="work" items={options.works}
        isOn={o => workIds.includes(o.id ?? -1)} onToggle={o => onChange({ workIds: toggle(workIds, o.id ?? -1) })} />
    </>}
  </fieldset>
}

function Group({ title, kind, items, isOn, onToggle }: {
  title: string; kind: string; items: RelationOption[]; isOn: (o: RelationOption) => boolean; onToggle: (o: RelationOption) => void
}) {
  return <div className={`rel-pick-group k-${kind}`}>
    <span className="rel-pick-title"><i aria-hidden="true" />{title}</span>
    {items.length === 0 ? <p className="hint-text">{t('editorRelations.s8', "Пока пусто.")}</p>
      : <div className="rel-chips" role="group" aria-label={title}>{items.map(o => <label key={o.key ?? o.id} className={isOn(o) ? 'rel-chip on' : 'rel-chip'}>
        <input type="checkbox" checked={isOn(o)} onChange={() => onToggle(o)} />{o.title}{o.subtitle && <small>{o.subtitle}</small>}
      </label>)}</div>}
  </div>
}
