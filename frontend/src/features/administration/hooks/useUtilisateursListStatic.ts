import { useMemo, useState } from 'react'
import type { Utilisateur } from '@/features/auth/types'
import type { RoleFilter, StatutUtilisateurFilter } from '../data/mockUtilisateurs'

interface Options {
  source: Utilisateur[]
  size?: number
}

/** Filtrage + pagination statiques (même signature que les hooks liste). */
export function useUtilisateursListStatic({ source, size = 8 }: Options) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [role, setRole] = useState<RoleFilter>('tous')
  const [statut, setStatut] = useState<StatutUtilisateurFilter>('tous')

  const filtered = useMemo(() => {
    const terme = search.trim().toLowerCase()
    return source.filter((u) => {
      if (role !== 'tous' && u.role.nomRole !== role) return false
      if (statut === 'actifs' && !u.actif) return false
      if (statut === 'inactifs' && u.actif) return false
      if (!terme) return true
      return `${u.nom} ${u.prenom} ${u.email} ${u.username}`.toLowerCase().includes(terme)
    })
  }, [source, search, role, statut])

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / size))
  const safePage = Math.min(page, totalPages - 1)

  return {
    items: filtered.slice(safePage * size, safePage * size + size),
    total,
    totalPages,
    page: safePage,
    size,
    setPage,
    search,
    setSearch: (v: string) => {
      setSearch(v)
      setPage(0)
    },
    role,
    setRole: (v: RoleFilter) => {
      setRole(v)
      setPage(0)
    },
    statut,
    setStatut: (v: StatutUtilisateurFilter) => {
      setStatut(v)
      setPage(0)
    },
  }
}
