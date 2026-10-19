import { useEffect } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { Alert, Spinner } from '@/components/ui'
import { HistoriqueTable } from '../components'
import { usePaiementsList, useModesPaiement } from '../hooks'

/** Historique des paiements — branché sur GET /api/paiements. */
export function HistoriquePaiementsPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const { items, total, totalPages, page, setPage, filtres, setFiltre, resetFiltres,
    hasActiveFiltres, loading, error } = usePaiementsList(PAGE_SIZE)

  const { data: modes } = useModesPaiement()

  const exporter = () => {
    const header = 'Reference;Demande;Client;Date;Montant;Mode;Utilisateur;RefVirement'
    const rows = items.map((p) =>
      [
        p.referencePaiement,
        p.referenceDemande,
        p.nomSociete ?? '',
        p.datePaiement,
        p.montantPaye,
        p.libelleMode,
        p.createdByName ?? '',
        p.referenceVirement ?? '',
      ].join(';'),
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
              value={filtres.recherche}
              onChange={(e) => setFiltre('recherche', e.target.value)}
              placeholder="Référence, demande…"
            />
          </label>
          <label className="field filter-field">
            Mode de paiement
            <select
              value={filtres.modeId ?? ''}
              onChange={(e) => setFiltre('modeId', e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Tous</option>
              {(modes ?? []).map((m) => (
                <option key={m.id} value={m.id}>
                  {m.libelle}
                </option>
              ))}
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
          <Spinner label="Chargement de l'historique…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
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
                          <span
                            key={i}
                            className="pagination-item"
                            style={{ border: 'none', background: 'transparent' }}
                          >
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
        )}
      </section>
    </div>
  )
}