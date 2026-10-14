import { useEffect, useState } from 'react'
import { Alert, Spinner, TablePagination } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import type { Utilisateur } from '@/features/auth/types'
import { AuditDetailModal, AuditTable } from '../components'
import { ACTIONS_AUDIT, ENTITES_AUDIT, MODULES_AUDIT, getLibelleActionAudit } from '../constants/labels'
import { utilisateursApi } from '../services/administration.service'
import { useAuditList } from '../hooks/useAuditList'

const PAGE_SIZE = 8

/** Journal d'audit branché sur l'API : filtres serveur, pagination, détail, export CSV. */
export function AuditPage() {
  const [detailId, setDetailId] = useState<number | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, ...filtres } =
    useAuditList(PAGE_SIZE)

  const { data: utilisateurs } = useApi<ApiPage<Utilisateur>>(
    () => utilisateursApi.list({ page: 0, size: 100, recherche: null }),
    [],
  )
  const optionsUtilisateurs = utilisateurs?.content ?? []

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  const handleExport = () => {
    const header = 'Date;Utilisateur;Action;Module;Entite;IdEntite;Ancienne;Nouvelle;IP'
    const rows = items.map((e) =>
      [
        e.createdAt,
        e.nomUtilisateur,
        e.action,
        e.module,
        e.entite,
        e.entiteId,
        e.ancienneValeur,
        e.nouvelleValeur,
        e.adresseIp,
      ]
        .map((v) => `"${(v ?? '').toString().replace(/"/g, '""')}"`)
        .join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'journal-audit.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Journal d&apos;audit</h1>
          <p>Historique des actions effectuées dans l&apos;application.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleExport} disabled={items.length === 0}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
        </div>
      </header>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Utilisateur
            <select
              value={filtres.utilisateurId ?? ''}
              onChange={(e) =>
                filtres.setUtilisateurId(e.target.value === '' ? null : Number(e.target.value))
              }
            >
              <option value="">Tous</option>
              {optionsUtilisateurs.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.prenom} {u.nom}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Action
            <select value={filtres.action} onChange={(e) => filtres.setAction(e.target.value)}>
              <option value="toutes">Toutes</option>
              {ACTIONS_AUDIT.map((a) => (
                <option key={a} value={a}>
                  {getLibelleActionAudit(a)}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Module
            <select value={filtres.module} onChange={(e) => filtres.setModule(e.target.value)}>
              <option value="tous">Tous</option>
              {MODULES_AUDIT.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Entité
            <select value={filtres.entite} onChange={(e) => filtres.setEntite(e.target.value)}>
              <option value="toutes">Toutes</option>
              {ENTITES_AUDIT.map((e) => (
                <option key={e} value={e}>
                  {e}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Période (début)
            <input
              type="date"
              value={filtres.dateDebut}
              onChange={(e) => filtres.setDateDebut(e.target.value)}
            />
          </label>
          <label className="field filter-field">
            Période (fin)
            <input type="date" value={filtres.dateFin} onChange={(e) => filtres.setDateFin(e.target.value)} />
          </label>
          <button type="submit" className="btn-primary">
            Appliquer
          </button>
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement du journal…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <AuditTable items={items} onView={setDetailId} />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                  {total} entrées
                </span>
                <TablePagination page={page} totalPages={totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )}
      </section>

      <AuditDetailModal id={detailId} onClose={() => setDetailId(null)} />
    </div>
  )
}
