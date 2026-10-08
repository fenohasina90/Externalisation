import { useEffect, useMemo, useState } from 'react'
import { DomaineFormModal, DomainesTable } from '../components'
import { MOCK_DOMAINES, type StatutDomaineFilter } from '../data/mockDomaines'
import { useDomainesListStatic } from '../hooks'
import type { Domaine } from '../types'

const PAGE_SIZE = 8

/**
 * Domaines de société — étape 1 : interface seule, données statiques.
 * Même structure que les autres pages liste.
 */
export function DomainesPage() {
  const [domaines, setDomaines] = useState<Domaine[]>(MOCK_DOMAINES)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Domaine | null>(null)

  const { items, total, totalPages, page, setPage, search, setSearch, statut, setStatut } =
    useDomainesListStatic({ source: domaines, size: PAGE_SIZE })

  const kpis = useMemo(() => {
    const actifs = domaines.filter((d) => d.actif).length
    const totalCount = domaines.length
    return {
      total: totalCount,
      actifs,
      pctActifs: totalCount === 0 ? 0 : Math.round((actifs / totalCount) * 100),
      societes: domaines.reduce((sum, d) => sum + d.nombreSocietes, 0),
    }
  }, [domaines])

  const handleSave = (payload: { nomDomaine: string }, id?: number) => {
    if (id != null) {
      // Modification : le statut existant est conservé (géré par l'action Activer/Désactiver).
      setDomaines((prev) =>
        prev.map((d) => (d.id === id ? { ...d, nomDomaine: payload.nomDomaine } : d)),
      )
    } else {
      // Création : domaine actif par défaut.
      const nextId = Math.max(0, ...domaines.map((d) => d.id)) + 1
      setDomaines((prev) => [
        ...prev,
        { id: nextId, nomDomaine: payload.nomDomaine, description: null, actif: true, nombreSocietes: 0 },
      ])
    }
    setModalOpen(false)
    setEditing(null)
  }

  const handleToggleActif = (id: number, actif: boolean) => {
    setDomaines((prev) => prev.map((d) => (d.id === id ? { ...d, actif } : d)))
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
          <h1>Domaines de société</h1>
          <p>Organisez les sociétés clientes par domaine d&apos;activité.</p>
        </div>
        <div className="actions">
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              setEditing(null)
              setModalOpen(true)
            }}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Ajouter un domaine
          </button>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.total}</div>
            <div className="kpi-label">Domaines</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.actifs}</div>
            <div className="kpi-label">Actifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Au total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.societes}</div>
            <div className="kpi-label">Sociétés associées</div>
          </div>
        </div>
      </section>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Nom du domaine…"
            />
          </label>
          <label className="field filter-field">
            Statut
            <select value={statut} onChange={(e) => setStatut(e.target.value as StatutDomaineFilter)}>
              <option value="tous">Tous</option>
              <option value="actifs">Actifs</option>
              <option value="inactifs">Inactifs</option>
            </select>
          </label>
          {/* <button type="submit" className="btn-primary">
            Rechercher
          </button> */}
        </form>
      </section>

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <DomainesTable
              items={items}
              onEdit={(d) => {
                setEditing(d)
                setModalOpen(true)
              }}
              onToggleActif={handleToggleActif}
            />
          </div>
          {total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} domaines
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
      </section>

      <DomaineFormModal
        open={modalOpen}
        domaine={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />
    </div>
  )
}
