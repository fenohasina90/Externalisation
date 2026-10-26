import { useEffect, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { StatutFormModal, StatutsTable } from '../components'
import { useStatutsList, type StatutConfigFilter } from '../hooks/useStatutsList'
import { statutsApi } from '../services/administration.service'
import type { CreateStatutPayload, StatutDemandeConfig } from '../types'

/**
 * Statuts de demande branchés sur l'API : filtre actif, tri par ordre,
 * pagination serveur, création, modification et activation/désactivation.
 */
export function StatutsPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<StatutDemandeConfig | null>(null)
  const [confirmToggle, setConfirmToggle] = useState<{ id: number; actif: boolean } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, statut, setStatut, reload } =
    useStatutsList(PAGE_SIZE)

  const actifs = items.filter((s) => s.actif).length
  const inactifs = items.length - actifs
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const handleSave = async (payload: CreateStatutPayload, id?: number) => {
    if (id != null) {
      await statutsApi.update(id, payload)
    } else {
      await statutsApi.create(payload)
    }
    setModalOpen(false)
    setEditing(null)
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    setActionError(null)
    try {
      await statutsApi.setActif(id, actif)
      reload()
    } catch (err) {
      setActionError(messageErreur(err))
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
          <h1>Statuts de demande</h1>
          <p>Gérez les étapes du cycle de vie des demandes de service.</p>
        </div>
        <div className="actions">
          <Can permission="CONFIG_UPDATE">
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
              Ajouter un statut
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
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12h8M12 8v8" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Statuts</div>
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

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Statut
            <select
              value={statut}
              onChange={(e) => setStatut(e.target.value as StatutConfigFilter)}
            >
              <option value="tous">Tous</option>
              <option value="actifs">Actifs</option>
              <option value="inactifs">Inactifs</option>
            </select>
          </label>
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des statuts…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <StatutsTable
                items={items}
                onEdit={(s) => {
                  setEditing(s)
                  setModalOpen(true)
                }}
                onToggleActif={(id, actif) => setConfirmToggle({ id, actif })}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à{' '}
                  {Math.min((page + 1) * PAGE_SIZE, total)} sur {total} statuts
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

      <StatutFormModal
        open={modalOpen}
        statut={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmToggle !== null}
        titre={confirmToggle?.actif ? 'Activer ce statut ?' : 'Désactiver ce statut ?'}
        message={
          confirmToggle?.actif
            ? 'Ce statut sera de nouveau proposé dans le cycle de vie des demandes.'
            : 'Ce statut ne sera plus proposé pour les nouvelles demandes. Les demandes existantes sont conservées.'
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
