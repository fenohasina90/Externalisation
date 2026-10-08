import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DemandesTable } from '../components'
import { DEMANDE_OPTIONS, MOCK_DEMANDES } from '../data/mockDemandes'
import { useDemandesListStatic } from '../hooks'

const PAGE_SIZE = 8

/** Liste des demandes — étape 1 : interface seule, données statiques. */
export function DemandesPage() {
  const [showFilters, setShowFilters] = useState(false)
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres, hasActiveFiltres } =
    useDemandesListStatic(PAGE_SIZE)

  const kpis = useMemo(() => {
    const enCours = MOCK_DEMANDES.filter((d) => d.statut === 'EN_COURS').length
    const enAttente = MOCK_DEMANDES.filter((d) => d.statut === 'EN_ATTENTE').length
    const retards = MOCK_DEMANDES.filter((d) => d.situationPaiement === 'En retard').length
    return {
      total: MOCK_DEMANDES.length,
      enCours,
      pctEnCours: Math.round((enCours / MOCK_DEMANDES.length) * 100),
      enAttente,
      retards,
    }
  }, [])

  const exporter = () => {
    const header = 'Reference;Client;Service;Debut;Fin;Statut;Paiement'
    const rows = items.map((d) =>
      [d.reference, d.client, d.service, d.dateDebut, d.dateFin, d.statut, d.situationPaiement].join(';'),
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
          <Link to="/demandes/nouveau" className="btn-primary">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Nouvelle demande
          </Link>
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
            <div className="kpi-num">{kpis.total}</div>
            <div className="kpi-label">Demandes totales</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctEnCours} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.enCours}</div>
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
            <div className="kpi-num">{kpis.enAttente}</div>
            <div className="kpi-label">En attente</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">À suivre</span>
          <div
            className="kpi-icon"
            style={{ background: 'var(--danger-bg)', color: 'var(--danger)' }}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l10 18H2L12 3zM12 10v5M12 18h.01" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.retards}</div>
            <div className="kpi-label">Retards de paiement</div>
          </div>
        </div>
      </section>

      {showFilters && (
      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()}>
          <div className="filters-group filters-group--wide">
            <p className="filters-group__title">Demande</p>
            <label className="field">
              Référence
              <input
                type="text"
                value={filtres.reference}
                onChange={(e) => setFiltre('reference', e.target.value)}
                placeholder="REF-2026-…"
              />
            </label>
            <label className="field">
              Client
              <select value={filtres.client} onChange={(e) => setFiltre('client', e.target.value)}>
                <option value="tous">Tous les clients</option>
                {DEMANDE_OPTIONS.clients.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Service
              <select value={filtres.service} onChange={(e) => setFiltre('service', e.target.value)}>
                <option value="tous">Tous les services</option>
                {DEMANDE_OPTIONS.services.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Domaine
              <select value={filtres.domaine} onChange={(e) => setFiltre('domaine', e.target.value)}>
                <option value="tous">Tous les domaines</option>
                {DEMANDE_OPTIONS.domaines.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Commercial
              <select value={filtres.commercial} onChange={(e) => setFiltre('commercial', e.target.value)}>
                <option value="tous">Tous</option>
                {DEMANDE_OPTIONS.commerciaux.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="filters-group filters-group--wide">
            <p className="filters-group__title">Suivi</p>
            <label className="field">
              Statut
              <select value={filtres.statut} onChange={(e) => setFiltre('statut', e.target.value as typeof filtres.statut)}>
                <option value="tous">Tous</option>
                <option value="EN_ATTENTE">En attente</option>
                <option value="VALIDATION">Validation</option>
                <option value="EN_COURS">En cours</option>
                <option value="TERMINEE">Terminée</option>
                <option value="ANNULEE">Annulée</option>
              </select>
            </label>
            <label className="field">
              Paiement
              <select value={filtres.paiement} onChange={(e) => setFiltre('paiement', e.target.value as typeof filtres.paiement)}>
                <option value="tous">Tous</option>
                <option value="Payé">Payé</option>
                <option value="Partiel">Partiel</option>
                <option value="En attente">En attente</option>
                <option value="En retard">En retard</option>
              </select>
            </label>
            <label className="field">
              Retard
              <select value={filtres.retard} onChange={(e) => setFiltre('retard', e.target.value as typeof filtres.retard)}>
                <option value="tous">Tous</option>
                <option value="oui">En retard</option>
                <option value="non">À jour</option>
              </select>
            </label>
            <label className="field">
              Période (début)
              <input type="date" value={filtres.dateDebut} onChange={(e) => setFiltre('dateDebut', e.target.value)} />
            </label>
            <label className="field">
              Période (fin)
              <input type="date" value={filtres.dateFin} onChange={(e) => setFiltre('dateFin', e.target.value)} />
            </label>
          </div>

          <div className="filter-actions" style={{ marginTop: 12 }}>
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
      )}

      <section className="card">
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
                {Array.from({ length: totalPages }).map((_, i) => {
                  const show = i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1
                  if (!show) {
                    if (i === 1 || i === totalPages - 2) {
                      return (
                        <span key={i} className="pagination-item" style={{ border: 'none', background: 'transparent' }}>
                          …
                        </span>
                      )
                    }
                    return null
                  }
                  return (
                    <button
                      key={i}
                      type="button"
                      className={`pagination-item ${i === page ? 'active' : ''}`}
                      onClick={() => setPage(i)}
                    >
                      {i + 1}
                    </button>
                  )
                })}
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
      </section>
    </div>
  )
}
