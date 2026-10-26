import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import type { Utilisateur } from '@/features/auth/types'
import type { ApiPage } from '@/types'
import { utilisateursApi } from '../services/administration.service'

export type RoleFilter = 'tous' | string
export type StatutUtilisateurFilter = 'tous' | 'actifs' | 'inactifs'

/** Liste paginée via l'API : recherche, rôle, statut. */
export function useUtilisateursList(size = 8) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [role, setRole] = useState<RoleFilter>('tous')
  const [statut, setStatut] = useState<StatutUtilisateurFilter>('tous')
  const [roleId, setRoleId] = useState<number | null>(null)
  const terme = useDebounce(search, 300)

  const actif = statut === 'tous' ? undefined : statut === 'actifs'

  const { data, loading, error, reload } = useApi<ApiPage<Utilisateur>>(
    () =>
      utilisateursApi.list(
        { page, size, recherche: terme.trim() || null },
        { roleId: roleId ?? undefined, actif },
      ),
    [page, size, terme, roleId, statut],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  function handleRole(nom: RoleFilter, roles: { id: number; nom: string }[]): void {
    setRole(nom)
    setRoleId(nom === 'tous' ? null : (roles.find((r) => r.nom === nom)?.id ?? null))
    setPage(0)
  }

  function handleStatut(value: StatutUtilisateurFilter): void {
    setStatut(value)
    setPage(0)
  }

  return {
    items: data?.content ?? [],
    total: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    size,
    loading,
    error,
    reload,
    setPage,
    search,
    setSearch: handleSearch,
    role,
    setRole: handleRole,
    statut,
    setStatut: handleStatut,
  }
}
