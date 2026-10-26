import type { ReactNode } from 'react'

export interface LigneRecapitulatif {
  label: string
  valeur: ReactNode
}

export interface SectionRecapitulatif {
  titre: string
  lignes: LigneRecapitulatif[]
}

interface Props {
  titre?: string
  sections: SectionRecapitulatif[]
  note?: ReactNode
}

function estVide(valeur: ReactNode): boolean {
  return (
    valeur === null ||
    valeur === undefined ||
    (typeof valeur === 'string' && valeur.trim() === '')
  )
}

/** Récapitulatif latéral des informations déjà saisies dans un formulaire progressif. */
export function RecapitulatifForm({ titre = 'Récapitulatif', sections, note }: Props) {
  return (
    <aside className="card card-pad recap-card recap-form" aria-label={titre}>
      <h3>{titre}</h3>
      {sections.map((section) => (
        <section key={section.titre} className="recap-form__section">
          <h4>{section.titre}</h4>
          <dl className="recap-form__liste">
            {section.lignes.map((ligne, index) => (
              <div className="rc" key={`${section.titre}-${ligne.label}-${index}`}>
                <dt>{ligne.label}</dt>
                <dd className={estVide(ligne.valeur) ? 'recap-form__vide' : undefined}>
                  {estVide(ligne.valeur) ? '—' : ligne.valeur}
                </dd>
              </div>
            ))}
          </dl>
        </section>
      ))}
      {note && <div className="nt recap-form__note">{note}</div>}
    </aside>
  )
}
