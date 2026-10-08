import { useMemo, useState } from 'react'
import type { ClientResume, StatutClient, TriClients } from '../data/mockClients'

interface Options {
  source: ClientResume[]
  size?: number
}

/** Recherche, filtres, tri et pagination statiques (même signature que les hooks liste). */
export function useClientsListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [recherche, setRecherche] = useState('')
  const [activite, setActivite] = useState('toutes')
  const [responsable, setResponsable] = useState('tous')
  const [statut, setStatut] = useState<StatutClient | 'tous'>('tous')
  const [tri, setTri] = useState<TriClients>('nom-az')

  const filtered = useMemo(() => {
    const terme = recherche.trim().toLowerCase()
    const liste = source.filter((c) => {
      if (terme && !`${c.nom} ${c.email} ${c.telephone} ${c.responsable}`.toLowerCase().includes(terme)) return false
      if (activite !== 'toutes' && c.activite !== activite) return false
      if (responsable !== 'tous' && c.responsable !== responsable) return false
      if (statut !== 'tous' && c.statut !== statut) return false
      return true
    })
    const tries = [...liste]
    switch (tri) {
      case 'nom-az':
        tries.sort((a, b) => a.nom.localeCompare(b.nom, 'fr'))
        break
      case 'nom-za':
        tries.sort((a, b) => b.nom.localeCompare(a.nom, 'fr'))
        break
      case 'demandes-asc':
        tries.sort((a, b) => a.nbDemandes - b.nbDemandes)
        break
      case 'demandes-desc':
        tries.sort((a, b) => b.nbDemandes - a.nbDemandes)
        break
    }
    return tries
  }, [source, recherche, activite, responsable, statut, tri])

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  const hasActiveFiltres =
    recherche.trim() !== '' ||
    activite !== 'toutes' ||
    responsable !== 'tous' ||
    statut !== 'tous' ||
    tri !== 'nom-az'

  const resetFiltres = () => {
    setRecherche('')
    setActivite('toutes')
    setResponsable('tous')
    setStatut('tous')
    setTri('nom-az')
    setPage(0)
  }

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    recherche,
    setRecherche: (v: string) => {
      setRecherche(v)
      setPage(0)
    },
    activite,
    setActivite: (v: string) => {
      setActivite(v)
      setPage(0)
    },
    responsable,
    setResponsable: (v: string) => {
      setResponsable(v)
      setPage(0)
    },
    statut,
    setStatut: (v: StatutClient | 'tous') => {
      setStatut(v)
      setPage(0)
    },
    tri,
    setTri: (v: TriClients) => {
      setTri(v)
      setPage(0)
    },
    resetFiltres,
    hasActiveFiltres,
  }
}
