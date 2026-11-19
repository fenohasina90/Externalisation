/**
 * Désignation multi-lignes d'une facture / d'un paiement :
 * chaque ligne est préfixée par son type, coloré selon le type.
 */

export interface ObjetLigne {
  /** PRESTATION | DEBOURS | ABONNEMENT_MENSUEL */
  type: string
  /** Désignation lisible du service / débours (mois en fin pour l'abonnement). */
  libelle: string
}

const PREFIX: Record<string, string> = {
  PRESTATION: 'Prestation',
  DEBOURS: 'Débours',
  ABONNEMENT_MENSUEL: 'Abonnement',
}

const PREFIX_CLASS: Record<string, string> = {
  PRESTATION: 'objet-prefix objet-prefix--prestation',
  DEBOURS: 'objet-prefix objet-prefix--debours',
  ABONNEMENT_MENSUEL: 'objet-prefix objet-prefix--abonnement_mensuel',
}

export function DesignationObjets({ objets }: { objets: ObjetLigne[] }) {
  return (
    <span className="facture-designation">
      {objets.map((o, i) => (
        <span key={i} className="facture-designation__ligne">
          <span className={PREFIX_CLASS[o.type] ?? 'objet-prefix'}>
            {`${PREFIX[o.type] ?? o.type} : `}
          </span>
          <span>{o.libelle}</span>
        </span>
      ))}
    </span>
  )
}
