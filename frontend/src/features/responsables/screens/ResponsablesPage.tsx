import { useEffect, useState } from 'react'
import { ResponsableFormModal } from '../components/ResponsableFormModal'
import { ResponsablesTable } from '../components/ResponsablesTable'
import { useResponsablesList } from '../hooks/useResponsablesList'
import type { Responsable, StatutFilter } from '../types'
import { responsablesApi } from '../services/responsables.service'

export default function ResponsablesPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Responsable | null>(null)
  const [showFilters, setShowFilters] = useState(true)

  const { items, total, totalPages, page, loading, error, setPage, search, setSearch, statut, setStatut, reload } =
    useResponsablesList(8)

  const actifs = items.filter((r) => r.actif).length ?? 0
  const inactifs = total - actifs
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const openCreate = () => {
    setEditing(null)
    setModalOpen(true)
  }

  const openEdit = (r: Responsable) => {
    setEditing(r)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setEditing(null)
  }

  const handleSaved = () => {
    closeModal()
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    try {
      await responsablesApi.setActif(id, actif)
      reload()
    } catch (err) {
      console.error('Erreur lors du changement de statut du responsable', err)
    }
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
          <h1>Responsables clients</h1>
          <p>Gérez les interlocuteurs de chaque société cliente.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setShowFilters((v) => !v)}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
          <button type="button" className="btn-primary" onClick={openCreate}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Ajouter un responsable
          </button>
        </div>
      </header>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Responsables</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{actifs}</div>
            <div className="kpi-label">Actifs</div>
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
            <div className="kpi-num">{inactifs}</div>
            <div className="kpi-label">Inactifs</div>
          </div>
        </div>
      </section>

      {showFilters && (
        <section className="card card-pad">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              reload()
            }}
            className="filters-inline"
          >
            <label className="field filter-field">
              Recherche
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Nom, société, email…"
              />
            </label>

            <label className="field filter-field">
              Statut
              <select
                value={statut}
                onChange={(e) => {
                  const v = e.target.value as StatutFilter
                  setStatut(v)
                }}
              >
                <option value="tous">Tous</option>
                <option value="actifs">Actifs</option>
                <option value="inactifs">Inactifs</option>
              </select>
            </label>

            {/* <div className="filter-actions">
              <button type="submit" className="btn-primary" disabled={loading}>
                Rechercher
              </button>
              <button
                type="button"
                className="btn-ghost"
                onClick={() => {
                  setSearch('')
                  setStatut('tous')
                  setPage(0)
                }}
                disabled={loading || !hasActiveFilters}
              >
                Réinitialiser
              </button>
            </div> */}
          </form>
        </section>
      )}

      <section className="card">
        {error && (
          <div className="card-pad" style={{ color: 'var(--danger)' }}>
            {error}
          </div>
        )}

        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <ResponsablesTable
              items={items}
              loading={loading}
              onEdit={openEdit}
              onToggleActif={handleToggleActif}
            />
          </div>

          {!loading && total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {page * 8 + 1} à {Math.min((page + 1) * 8, total)} sur {total} responsables
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
              {Array.from({ length: Math.max(1, totalPages) }).map((_, i) => {
                const pageNum = i
                const isActive = pageNum === page
                const show =
                  pageNum === 0 ||
                  pageNum === totalPages - 1 ||
                  Math.abs(pageNum - page) <= 1

                if (!show && pageNum !== 1 && pageNum !== totalPages - 2) {
                  if (pageNum === page - 2 || pageNum === page + 2) {
                    return (
                      <span
                        key={pageNum}
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
                    key={pageNum}
                    type="button"
                    className={`pagination-item ${isActive ? 'active' : ''}`}
                    onClick={() => setPage(pageNum)}
                  >
                    {pageNum + 1}
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

      <ResponsableFormModal
        open={modalOpen}
        responsable={editing}
        onClose={closeModal}
        onSaved={handleSaved}
      />
    </div>
  )
}
