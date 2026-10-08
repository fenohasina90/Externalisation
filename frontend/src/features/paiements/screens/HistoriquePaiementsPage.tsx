import { useEffect } from 'react'
import { HistoriqueTable } from '../components'
import { MOCK_HISTORIQUE, PAIEMENT_OPTIONS, type ModePaiement } from '../data/mockPaiements'
import { useHistoriqueListStatic } from '../hooks'

const PAGE_SIZE = 8

/** Historique des paiements — étape 1 : interface seule, données statiques. */
export function HistoriquePaiementsPage() {
  const { items, total, totalPages, page, setPage, ...f } = useHistoriqueListStatic(
    MOCK_HISTORIQUE,
    PAGE_SIZE,
  )

  const exporter = () => {
    const header = 'Reference;Demande;Client;Date;Montant;Mode;Utilisateur;RefVirement'
    const rows = items.map((p) =>
      [p.reference, p.demandeRef, p.client, p.date, p.montant, p.mode, p.utilisateur, p.refVirement ?? ''].join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'historique-paiements.csv'
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
          <h1>Historique des paiements</h1>
          <p>Tous les règlements enregistrés, du plus récent au plus ancien.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={exporter}>
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
            Recherche
            <input
              type="text"
              value={f.recherche}
              onChange={(e) => f.setRecherche(e.target.value)}
              placeholder="Référence, demande, client…"
            />
          </label>
          <label className="field filter-field">
            Client
            <select value={f.client} onChange={(e) => f.setClient(e.target.value)}>
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
            <select value={f.periode} onChange={(e) => f.setPeriode(e.target.value)}>
              <option value="tous">Toutes les périodes</option>
              {PAIEMENT_OPTIONS.periodes.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Mode de paiement
            <select value={f.mode} onChange={(e) => f.setMode(e.target.value as ModePaiement | 'tous')}>
              <option value="tous">Tous</option>
              <option value="Espèces">Espèces</option>
              <option value="Virement">Virement</option>
            </select>
          </label>
          <div className="filter-actions filter-actions--bottom">
            <button
              type="button"
              className="btn-ghost"
              onClick={f.resetFiltres}
              disabled={!f.hasActiveFiltres}
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
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <HistoriqueTable items={items} />
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
