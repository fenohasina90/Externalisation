import type { ReactNode } from 'react'

type Tone = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function StatutBadge({ statut }: { statut: string | null | undefined }) {
  const tone: Tone = tonParStatut(statut)
  return <Badge tone={tone}>{statut ?? '—'}</Badge>
}

function tonParStatut(statut: string | null | undefined): Tone {
  switch ((statut ?? '').toUpperCase()) {
    case 'EN_ATTENTE':
    case 'EN ATTENTE':
      return 'warning'
    case 'VALIDATION':
      return 'info'
    case 'EN_COURS':
    case 'EN COURS':
      return 'default'
    case 'TERMINEE':
    case 'TERMINÉE':
      return 'success'
    case 'ANNULEE':
    case 'ANNULÉE':
      return 'danger'
    case 'EN_RETARD':
    case 'EN RETARD':
      return 'danger'
    case 'PROCHE':
      return 'warning'
    case 'A_JOUR':
    case 'À JOUR':
      return 'success'
    default:
      return 'neutral'
  }
}

export function BooleenBadge({ valeur, si = 'Oui', non = 'Non' }: {
  valeur: boolean | null | undefined
  si?: string
  non?: string
}) {
  return <Badge tone={valeur ? 'success' : 'neutral'}>{valeur ? si : non}</Badge>
}