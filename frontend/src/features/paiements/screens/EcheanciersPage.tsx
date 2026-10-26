import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { formatMontantMGA } from '@/utils/format'
import type { ApiPage } from '@/types'
import type { Societe } from '@/features/clients/types'
import { clientsApi } from '@/features/clients/services/clients.service'
import { SituationPlanPill } from '../components'
import { LIBELLES_SITUATION } from '../constants/labels'
import { useEcheanciers } from '../hooks'

/** Liste des échéanciers par demande, branchée sur l'API. */
export function EcheanciersPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres,
    hasActiveFiltres, loading, error } = useEcheanciers(PAGE_SIZE)

  const { data: societes } = useApi<ApiPage<Societe>>(
    () =>
      clientsApi.list({ page: 0, size: 200, recherche: null }, {}, { champ: 'nomSociete', sens: 'asc' }),
    [],
  )

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Échéanciers</h1>
          <p>Suivi des échéances de chaque demande de service.</p>
        </div>
      </header>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
            <input
              type="text"
              value={filtres.recherche}
              onChange={(e) => setFiltre('recherche', e.target.value)}
              placeholder="Demande, client…"
            />
          </label>
          <label className="field filter-field">
            Client
            <select
              value={filtres.societeId ?? ''}
              onChange={(e) => setFiltre('societeId', e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Tous les clients</option>
              {(societes?.content ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nomSociete}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Situation
            <select
              value={filtres.situation}
              onChange={(e) =>
                setFiltre('situation', e.target.value as typeof filtres.situation)
              }
            >
              <option value="tous">Toutes</option>
              <option value="PAYE">Payé</option>
              <option value="PARTIEL">Partiel</option>
              <option value="EN_RETARD">En retard</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="NON_PLANIFIE">Non planifié</option>
            </select>
          </label>
          <div className="filter-actions filter-actions--bottom">
            <button
              type="button"
              className="btn-ghost"
              onClick={resetFiltres}
              disabled={!hasActiveFiltres}
            >
              Réinitialiser
            </button>
            <button type="submit" className="btn-primary">
              Appliquer
            </button>
          </div>
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des échéanciers…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <>
            <div className="table-scroll-container no-global-scroll">
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Demande</th>
                      <th>Client</th>
                      <th>Services</th>
                      <th>Échéances</th>
                      <th>Montant total</th>
                      <th>Reste à payer</th>
                      <th>Situation</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((p) => (
                      <tr key={p.demandeId}>
                        <td className="fw-700">{p.referenceDemande}</td>
                        <td>{p.nomSociete ?? '—'}</td>
                        <td className="cell-number">{p.nbServices}</td>
                        <td className="cell-number">
                          {p.situation === 'NON_PLANIFIE' ? (
                            <span className="text-muted">—</span>
                          ) : (
                            p.nbEcheances
                          )}
                        </td>
                        <td className="cell-number">{formatMontantMGA(p.montantTotal)}</td>
                        <td className="cell-number fw-700">{formatMontantMGA(p.reste)}</td>
                        <td>
                          <SituationPlanPill situation={p.situation} />
                        </td>
                        <td>
                          <Link
                            to={`/paiements/echeanciers/${p.referenceDemande}`}
                            className="btn-link success"
                          >
                            Voir ›
                          </Link>
                        </td>
                      </tr>
                    ))}
                    {items.length === 0 && (
                      <tr>
                        <td colSpan={8} className="text-muted">
                          Aucun échéancier trouvé.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à{' '}
                  {Math.min((page + 1) * PAGE_SIZE, total)} sur {total} échéanciers
                  {filtres.situation !== 'tous'
                    ? ` en situation « ${LIBELLES_SITUATION[filtres.situation]} »`
                    : ''}
                </span>
                <div className="pagination">
                  <button
                    type="button"
                    className="pagination-item"
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page <= 0}
                  >
                    ‹
                  </button>
                  {Array.from({ length: totalPages }).map((_, i) => (
                    <button
                      key={i}
                      type="button"
                      className={`pagination-item ${i === page ? 'active' : ''}`}
                      onClick={() => setPage(i)}
                    >
                      {i + 1}
                    </button>
                  ))}
                  <button
                    type="button"
                    className="pagination-item"
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                  >
                    ›
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}