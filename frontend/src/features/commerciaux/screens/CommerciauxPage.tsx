import { useEffect, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { CommercialFormModal, CommerciauxTable } from '../components'
import { useCommerciauxList, type StatutCommercialFilter } from '../hooks/useCommerciauxList'
import { commerciauxApi } from '../services/commerciaux.service'
import type { Commercial, CreateCommercialPayload } from '../types'


/**
 * Liste des commerciaux branchée sur l'API :
 * recherche, filtre statut, pagination serveur, création avec
 * immatricule générée côté backend (C001, C002, … unique).
 */
export function CommerciauxPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Commercial | null>(null)
  const [showFilters, setShowFilters] = useState(true)
  const [confirmToggle, setConfirmToggle] = useState<{ id: number; actif: boolean } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, search, setSearch, statut, setStatut, reload } =
    useCommerciauxList(PAGE_SIZE)

  const actifs = items.filter((c) => c.actif).length
  const inactifs = items.filter((c) => !c.actif).length
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const openCreate = () => {
    setEditing(null)
    setModalOpen(true)
  }

  const openEdit = (c: Commercial) => {
    setEditing(c)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setEditing(null)
  }

  const handleSave = async (payload: CreateCommercialPayload, id?: number) => {
    if (id != null) {
      await commerciauxApi.update(id, payload)
    } else {
      await commerciauxApi.create(payload)
    }
    closeModal()
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    setActionError(null)
    try {
      await commerciauxApi.setActif(id, actif)
      reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Échec du changement de statut')
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
          <h1>Liste des commerciaux</h1>
          <p>Gérez l&apos;équipe commerciale et ses accès.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setShowFilters((v) => !v)}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
          <Can permission="COMMERCIAL_CREATE">
            <button type="button" className="btn-primary" onClick={openCreate}>
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Ajouter un commercial
            </button>
          </Can>
        </div>
      </header>

      {actionError && (
        <div className="alert alert-danger" role="alert">
          {actionError}
        </div>
      )}

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <rect x="3" y="7" width="18" height="13" rx="2" />
              <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3 13h18" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Commerciaux</div>
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
            }}
            className="filters-inline"
          >
            <label className="field filter-field">
              Recherche
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Nom, matricule, email…"
              />
            </label>

            <label className="field filter-field">
              Statut
              <select
                value={statut}
                onChange={(e) => setStatut(e.target.value as StatutCommercialFilter)}
              >
                <option value="tous">Tous</option>
                <option value="actifs">Actifs</option>
                <option value="inactifs">Inactifs</option>
              </select>
            </label>
          </form>
        </section>
      )}

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des commerciaux…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <CommerciauxTable
                items={items}
                onEdit={openEdit}
                onToggleActif={(id, actif) => setConfirmToggle({ id, actif })}
              />
            </div>

            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                  {total} commerciaux
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
                      pageNum === 0 || pageNum === totalPages - 1 || Math.abs(pageNum - page) <= 1

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
        )}
      </section>

      <CommercialFormModal
        open={modalOpen}
        commercial={editing}
        onClose={closeModal}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmToggle !== null}
        titre={confirmToggle?.actif ? 'Activer ce commercial ?' : 'Désactiver ce commercial ?'}
        message={
          confirmToggle?.actif
            ? 'Ce commercial sera de nouveau disponible pour les demandes de service.'
            : 'Ce commercial ne sera plus proposé dans les nouvelles demandes. L’historique est conservé.'
        }
        confirmer={confirmToggle?.actif ? 'Activer' : 'Désactiver'}
        ton={confirmToggle?.actif ? 'success' : 'danger'}
        onAnnuler={() => setConfirmToggle(null)}
        onConfirmer={() => {
          if (!confirmToggle) return
          const { id, actif } = confirmToggle
          setConfirmToggle(null)
          void handleToggleActif(id, actif)
        }}
      />
    </div>
  )
}
