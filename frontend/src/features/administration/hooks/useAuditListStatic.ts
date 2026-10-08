import { useMemo, useState } from 'react'
import type { AuditEntry } from '../types'

interface Options {
  source: AuditEntry[]
  size?: number
}

/** Filtrage + pagination statiques du journal d'audit. */
export function useAuditListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [utilisateur, setUtilisateur] = useState('tous')
  const [action, setAction] = useState('toutes')
  const [module, setModule] = useState('tous')
  const [entite, setEntite] = useState('toutes')

  const filtered = useMemo(
    () =>
      source.filter((e) => {
        if (utilisateur !== 'tous' && e.nomUtilisateur !== utilisateur) return false
        if (action !== 'toutes' && e.action !== action) return false
        if (module !== 'tous' && e.module !== module) return false
        if (entite !== 'toutes' && e.entite !== entite) return false
        return true
      }),
    [source, utilisateur, action, module, entite],
  )

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  const reset = () => {
    setUtilisateur('tous')
    setAction('toutes')
    setModule('tous')
    setEntite('toutes')
    setPage(0)
  }

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    setPage,
    utilisateur,
    setUtilisateur: (v: string) => {
      setUtilisateur(v)
      setPage(0)
    },
    action,
    setAction: (v: string) => {
      setAction(v)
      setPage(0)
    },
    module,
    setModule: (v: string) => {
      setModule(v)
      setPage(0)
    },
    entite,
    setEntite: (v: string) => {
      setEntite(v)
      setPage(0)
    },
    reset,
  }
}
