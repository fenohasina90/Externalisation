import type { ReactNode } from 'react'
import { formatMontantMGA } from '@/utils/format'

interface Props {
  icone: ReactNode
  libelle: string
  /** Étiquette d'appoint (ex. « 62 % »). */
  tag?: string
  /** Mise en avant ou alerte colorée. */
  ton?: 'hl' | 'danger'
  /** Montant en devise — `null` si la valeur est masquée pour le rôle. */
  montant?: number | null
  /** Compteur — `null` si la valeur est masquée pour le rôle. */
  nombre?: number | null
  loading?: boolean
}

/**
 * Carte d'indicateur de la liste des paiements.
 * Distingue trois états qu'un simple `?? 0` confondait : chargement,
 * valeur masquée par le backend (`null` → « — ») et valeur réelle à zéro.
 */
export function KpiCard({ icone, libelle, tag, ton, montant, nombre, loading }: Props) {
  const brut = montant !== undefined ? montant : nombre
  const affiche = loading ? '…' : brut === null || brut === undefined ? '—' : brut

  const classe = ton === 'hl' ? 'card kpi-card hl' : 'card kpi-card'

  return (
    <div className={classe} aria-busy={loading || undefined}>
      <div
        className="kpi-icon"
        style={
          ton === 'danger'
            ? { background: 'var(--danger-bg)', color: 'var(--danger)' }
            : undefined
        }
      >
        {icone}
      </div>
      <div>
        {tag && <span className="kpi-tag">{tag}</span>}
        <div className="kpi-num">
          {montant !== undefined && brut !== null && brut !== undefined
            ? formatMontantMGA(brut)
            : affiche}
        </div>
        <div className="kpi-label">{libelle}</div>
      </div>
    </div>
  )
}