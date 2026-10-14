import { useEffect, useMemo, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { Link } from 'react-router-dom'
import { StatutPlanPill } from '../components'
import { MOCK_PLANS, formatMGA } from '../data/mockPaiements'


/** Liste des échéanciers par demande (index vers le détail). */
export function EcheanciersPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const filtered = useMemo(() => {
    const terme = search.trim().toLowerCase()
    if (!terme) return MOCK_PLANS
    return MOCK_PLANS.filter((p) =>
      `${p.demandeRef} ${p.client}`.toLowerCase().includes(terme),
    )
  }, [search])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const items = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE)

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
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(0)
              }}
              placeholder="Demande, client…"
            />
          </label>
        </form>
      </section>

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Demande</th>
                  <th>Client</th>
                  <th>Échéances</th>
                  <th>Montant total</th>
                  <th>Reste à payer</th>
                  <th>Statut</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.reference}>
                    <td className="fw-700">{p.demandeRef}</td>
                    <td>{p.client}</td>
                    <td className="cell-number">{p.echeances.length}</td>
                    <td className="cell-number">{formatMGA(p.montantTotal)}</td>
                    <td className="cell-number fw-700">{formatMGA(p.montantTotal - p.montantPaye)}</td>
                    <td>
                      <StatutPlanPill statut={p.statut} />
                    </td>
                    <td>
                      <Link to={`/paiements/echeanciers/${p.demandeRef}`} className="btn-link success">
                        Voir ›
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="table-footer">
            <span>
              Affichage de {safePage * PAGE_SIZE + 1} à{' '}
              {Math.min((safePage + 1) * PAGE_SIZE, filtered.length)} sur {filtered.length} échéanciers
            </span>
            <div className="pagination">
              <button
                type="button"
                className="pagination-item"
                onClick={() => setPage(Math.max(0, safePage - 1))}
                disabled={safePage <= 0}
              >
                ‹
              </button>
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`pagination-item ${i === safePage ? 'active' : ''}`}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))}
              <button
                type="button"
                className="pagination-item"
                onClick={() => setPage(Math.min(totalPages - 1, safePage + 1))}
                disabled={safePage >= totalPages - 1}
              >
                ›
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
