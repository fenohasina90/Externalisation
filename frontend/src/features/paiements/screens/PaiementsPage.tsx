import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { PlansTable } from '../components'
import { MOCK_PLANS, PAIEMENT_OPTIONS, formatMGA } from '../data/mockPaiements'
import { usePlansListStatic } from '../hooks'

const PAGE_SIZE = 8

/** Liste des paiements — étape 1 : interface seule, données statiques. */
export function PaiementsPage() {
  const [showFilters, setShowFilters] = useState(false)
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres, hasActiveFiltres } =
    usePlansListStatic(MOCK_PLANS, PAGE_SIZE)

  const kpis = useMemo(() => {
    const totalMontant = MOCK_PLANS.reduce((s, p) => s + p.montantTotal, 0)
    const paye = MOCK_PLANS.reduce((s, p) => s + p.montantPaye, 0)
    const retards = MOCK_PLANS.filter((p) => p.statut === 'En retard').length
    return {
      total: totalMontant,
      paye,
      pctPaye: Math.round((paye / totalMontant) * 100),
      reste: totalMontant - paye,
      pctReste: Math.round(((totalMontant - paye) / totalMontant) * 100),
      retards,
    }
  }, [])

  const exporter = () => {
    const header = 'Reference;Client;Demande;Total;Paye;Reste;Statut'
    const rows = items.map((p) =>
      [p.reference, p.client, p.demandeRef, p.montantTotal, p.montantPaye, p.montantTotal - p.montantPaye, p.statut].join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'paiements.csv'
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
          <h1>Liste des paiements</h1>
          <p>Suivez les montants payés et restants de chaque demande de service.</p>
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
          <Link to="/paiements/nouveau" className="btn-primary">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Enregistrer un paiement
          </Link>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <path d="M3 10h18" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(kpis.total)}</div>
            <div className="kpi-label">Montant total à payer</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctPaye} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(kpis.paye)}</div>
            <div className="kpi-label">Montant total payé</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">{kpis.pctReste} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(kpis.reste)}</div>
            <div className="kpi-label">Reste à payer</div>
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
            <div className="kpi-label">Paiements en retard</div>
          </div>
        </div>
      </section>

      {showFilters && (
      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Client
            <select value={filtres.client} onChange={(e) => setFiltre('client', e.target.value)}>
              <option value="tous">Tous les clients</option>
              {PAIEMENT_OPTIONS.clients.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Période
            <select value={filtres.periode} onChange={(e) => setFiltre('periode', e.target.value)}>
              <option value="tous">Toutes les périodes</option>
              {PAIEMENT_OPTIONS.periodes.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Statut
            <select value={filtres.statut} onChange={(e) => setFiltre('statut', e.target.value as typeof filtres.statut)}>
              <option value="tous">Tous</option>
              <option value="Payé">Payé</option>
              <option value="Partiel">Partiel</option>
              <option value="En attente">En attente</option>
              <option value="En retard">En retard</option>
            </select>
          </label>
          <label className="field filter-field">
            Mode de paiement
            <select value={filtres.mode} onChange={(e) => setFiltre('mode', e.target.value as typeof filtres.mode)}>
              <option value="tous">Tous</option>
              <option value="Espèces">Espèces</option>
              <option value="Virement">Virement</option>
            </select>
          </label>
          <label className="field filter-field">
            Échéance
            <select value={filtres.nbEcheances} onChange={(e) => setFiltre('nbEcheances', e.target.value)}>
              <option value="toutes">Toutes</option>
              <option value="2">2 échéances</option>
              <option value="3">3 échéances</option>
              <option value="4">4 échéances</option>
            </select>
          </label>
          <label className="field filter-field">
            Retard
            <select value={filtres.retard} onChange={(e) => setFiltre('retard', e.target.value as typeof filtres.retard)}>
              <option value="tous">Tous</option>
              <option value="oui">En retard</option>
              <option value="non">À jour</option>
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
      )}

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <PlansTable items={items} />
          </div>
          {total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} paiements
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
