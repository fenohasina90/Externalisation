import { useState } from 'react'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { auditApi } from '../services/administration.service'
import type { AuditEntry } from '../types'

/** Filtres + pagination serveur du journal d'audit. */
export function useAuditList(size = 8) {
  const [page, setPage] = useState(0)
  const [utilisateurId, setUtilisateurId] = useState<number | null>(null)
  const [action, setAction] = useState('toutes')
  const [module, setModule] = useState('tous')
  const [entite, setEntite] = useState('toutes')
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')

  const { data, loading, error, reload } = useApi<ApiPage<AuditEntry>>(
    () =>
      auditApi.list(
        { page, size, recherche: null },
        {
          utilisateurId,
          action: action === 'toutes' ? null : action,
          module: module === 'tous' ? null : module,
          entite: entite === 'toutes' ? null : entite,
          dateDebut: dateDebut || null,
          dateFin: dateFin || null,
        },
      ),
    [page, size, utilisateurId, action, module, entite, dateDebut, dateFin],
  )

  function resetPage(): void {
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
    utilisateurId,
    setUtilisateurId: (v: number | null) => {
      setUtilisateurId(v)
      resetPage()
    },
    action,
    setAction: (v: string) => {
      setAction(v)
      resetPage()
    },
    module,
    setModule: (v: string) => {
      setModule(v)
      resetPage()
    },
    entite,
    setEntite: (v: string) => {
      setEntite(v)
      resetPage()
    },
    dateDebut,
    setDateDebut: (v: string) => {
      setDateDebut(v)
      resetPage()
    },
    dateFin,
    setDateFin: (v: string) => {
      setDateFin(v)
      resetPage()
    },
  }
}
