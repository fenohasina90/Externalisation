import { useEffect, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { Link } from 'react-router-dom'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import type { Societe } from '@/features/clients/types'
import type { Commercial } from '@/features/commerciaux/types'
import type { Service } from '@/features/services/types'
import { clientsApi } from '@/features/clients/services/clients.service'
import { commerciauxApi } from '@/features/commerciaux/services/commerciaux.service'
import { servicesApi } from '@/features/services/services/services.service'
import { DemandesTable } from '../components'
import { useDemandesList } from '../hooks/useDemandesList'
import { demandesApi } from '../services/demandes.service'

/** Liste des demandes multi-services branchée sur l'API. */
export function DemandesPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [showFilters, setShowFilters] = useState(true)
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres,
    hasActiveFiltres, loading, error } = useDemandesList(PAGE_SIZE)

  const { data: pageEnCours } = useApi<ApiPage<unknown>>(
    () => demandesApi.list({ page: 0, size: 1, recherche: null }, { statut: 'EN_COURS' }),
    [],
  )
  const { data: pageEnAttente } = useApi<ApiPage<unknown>>(
    () => demandesApi.list({ page: 0, size: 1, recherche: null }, { statut: 'EN_ATTENTE' }),
    [],
  )
  const { data: societes } = useApi<ApiPage<Societe>>(
    () => clientsApi.list({ page: 0, size: 100, recherche: null }, {}, { champ: 'nomSociete', sens: 'asc' }),
    [],
  )
  const { data: services } = useApi<ApiPage<Service>>(
    () => servicesApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [],
  )
  const { data: commerciaux } = useApi<ApiPage<Commercial>>(
    () => commerciauxApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [],
  )

  const nbEnCours = pageEnCours?.totalElements ?? 0
  const nbEnAttente = pageEnAttente?.totalElements ?? 0

  const exporter = () => {
    const header = 'Reference;Client;Services;Total;Paye;Reste;Statut'
    const rows = items.map((d) =>
      [
        d.referenceDemande,
        d.nomSociete ?? '',
        (d.lignes ?? []).map((l) => l.nomService ?? '').join(' + '),
        d.montantTotal ?? 0,
        d.montantPaye,
        d.resteAPayer,
        d.statutActuel,
      ].join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'demandes.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

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
          <h1>Liste des demandes</h1>
          <p>Suivez toutes les demandes de service, leurs statuts et leur situation de paiement.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setShowFilters((v) => !v)}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
          <button type="button" className="btn-ghost" onClick={exporter}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
          <Can permission="DEMANDE_CREATE">
            <Link to="/demandes/nouveau" className="btn-primary">
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Nouvelle demande
            </Link>
          </Can>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Demandes totales</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nbEnCours}</div>
            <div className="kpi-label">En cours</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">À traiter</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nbEnAttente}</div>
            <div className="kpi-label">En attente</div>
          </div>
        </div>
      </section>

      {showFilters && (
        <section className="card card-pad">
          <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
            <label className="field filter-field">
              Recherche
              <input
                type="text"
                value={filtres.recherche}
                onChange={(e) => setFiltre('recherche', e.target.value)}
                placeholder="Référence…"
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
              Service
              <select
                value={filtres.serviceId ?? ''}
                onChange={(e) => setFiltre('serviceId', e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">Tous les services</option>
                {(services?.content ?? []).map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nomService}
                  </option>
                ))}
              </select>
            </label>
            <label className="field filter-field">
              Commercial
              <select
                value={filtres.commercialId ?? ''}
                onChange={(e) => setFiltre('commercialId', e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">Tous</option>
                {(commerciaux?.content ?? []).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nomCommercial}
                  </option>
                ))}
              </select>
            </label>
            <label className="field filter-field">
              Statut
              <select value={filtres.statut} onChange={(e) => setFiltre('statut', e.target.value)}>
                <option value="tous">Tous</option>
                <option value="EN_ATTENTE">En attente</option>
                <option value="VALIDATION">Validation</option>
                <option value="EN_COURS">En cours</option>
                <option value="TERMINEE">Terminée</option>
                <option value="ANNULEE">Annulée</option>
              </select>
            </label>
            <label className="field filter-field">
              Période (début)
              <input
                type="date"
                value={filtres.dateDebut}
                onChange={(e) => setFiltre('dateDebut', e.target.value)}
              />
            </label>
            <label className="field filter-field">
              Période (fin)
              <input
                type="date"
                value={filtres.dateFin}
                onChange={(e) => setFiltre('dateFin', e.target.value)}
              />
            </label>
            <button type="button" className="btn-ghost" onClick={resetFiltres} disabled={!hasActiveFiltres}>
              Réinitialiser
            </button>
          </form>
        </section>
      )}

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des demandes…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <DemandesTable items={items} />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                  {total} demandes
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
          </div>
        )}
      </section>
    </div>
  )
}
