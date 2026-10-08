import { useMemo, useState } from 'react'
import { MOCK_DEMANDES } from '../data/mockDemandes'
import type { SituationPaiement, StatutDemandeUI } from '../data/mockDemandes'

export interface DemandesFiltres {
  reference: string
  client: string
  service: string
  domaine: string
  commercial: string
  statut: StatutDemandeUI | 'tous'
  dateDebut: string
  dateFin: string
  paiement: SituationPaiement | 'tous'
  retard: 'tous' | 'oui' | 'non'
}

/** jj/mm/aaaa → aaaa-mm-jj pour comparaison avec les champs date. */
function versISO(dateFR: string): string {
  const [j, m, a] = dateFR.split('/')
  return `${a}-${m}-${j}`
}

/** Liste + filtres + pagination statiques (8 par page). */
export function useDemandesListStatic(size = 8) {
  const [page, setPage] = useState(0)
  const [filtres, setFiltres] = useState<DemandesFiltres>({
    reference: '',
    client: 'tous',
    service: 'tous',
    domaine: 'tous',
    commercial: 'tous',
    statut: 'tous',
    dateDebut: '',
    dateFin: '',
    paiement: 'tous',
    retard: 'tous',
  })

  const filtered = useMemo(
    () =>
      MOCK_DEMANDES.filter((d) => {
        if (filtres.reference && !d.reference.toLowerCase().includes(filtres.reference.trim().toLowerCase())) return false
        if (filtres.client !== 'tous' && d.client !== filtres.client) return false
        if (filtres.service !== 'tous' && d.service !== filtres.service) return false
        if (filtres.domaine !== 'tous' && d.domaine !== filtres.domaine) return false
        if (filtres.commercial !== 'tous' && d.commercial !== filtres.commercial) return false
        if (filtres.statut !== 'tous' && d.statut !== filtres.statut) return false
        if (filtres.paiement !== 'tous' && d.situationPaiement !== filtres.paiement) return false
        if (filtres.retard === 'oui' && d.situationPaiement !== 'En retard') return false
        if (filtres.retard === 'non' && d.situationPaiement === 'En retard') return false
        if (filtres.dateDebut && versISO(d.dateDebut) < filtres.dateDebut) return false
        if (filtres.dateFin && versISO(d.dateDebut) > filtres.dateFin) return false
        return true
      }),
    [filtres],
  )

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  const setFiltre = <K extends keyof DemandesFiltres>(cle: K, valeur: DemandesFiltres[K]) => {
    setFiltres((f) => ({ ...f, [cle]: valeur }))
    setPage(0)
  }

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    filtres,
    setFiltre,
  }
}
