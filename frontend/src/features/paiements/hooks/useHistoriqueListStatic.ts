import { useMemo, useState } from 'react'
import { dansPeriode, type ModePaiement, type PaiementHistorique } from '../data/mockPaiements'

/** Historique des paiements + filtres + pagination (8 par page). */
export function useHistoriqueListStatic(source: PaiementHistorique[], size = 8) {
  const [page, setPage] = useState(0)
  const [recherche, setRecherche] = useState('')
  const [client, setClient] = useState('tous')
  const [periode, setPeriode] = useState('tous')
  const [mode, setMode] = useState<ModePaiement | 'tous'>('tous')

  const filtered = useMemo(() => {
    const terme = recherche.trim().toLowerCase()
    return source.filter((p) => {
      if (terme && !`${p.reference} ${p.demandeRef} ${p.client}`.toLowerCase().includes(terme)) return false
      if (client !== 'tous' && p.client !== client) return false
      if (!dansPeriode(p.date, periode)) return false
      if (mode !== 'tous' && p.mode !== mode) return false
      return true
    })
  }, [source, recherche, client, periode, mode])

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  const hasActiveFiltres =
    recherche.trim() !== '' || client !== 'tous' || periode !== 'tous' || mode !== 'tous'

  const resetFiltres = () => {
    setRecherche('')
    setClient('tous')
    setPeriode('tous')
    setMode('tous')
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
    client,
    setClient: (v: string) => {
      setClient(v)
      setPage(0)
    },
    periode,
    setPeriode: (v: string) => {
      setPeriode(v)
      setPage(0)
    },
    mode,
    setMode: (v: ModePaiement | 'tous') => {
      setMode(v)
      setPage(0)
    },
    resetFiltres,
    hasActiveFiltres,
  }
}
