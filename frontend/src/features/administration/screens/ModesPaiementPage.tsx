import { useEffect, useState } from 'react'
import { useTaillePagination } from '@/hooks/useTaillePagination'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Can } from '@/components/shared/Can'
import { Alert, Spinner } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { ModePaiementFormModal, ModesPaiementTable } from '../components'
import { useModesPaiementList, type ModePaiementFilter } from '../hooks/useModesPaiementList'
import { modesPaiementApi } from '../services/administration.service'
import type { CreateModePaiementPayload, ModePaiementAdmin } from '../types'

/**
 * Modes de paiement branchés sur l'API : filtre actif, tri par code,
 * pagination serveur, création, modification et activation/désactivation.
 */
export function ModesPaiementPage() {
  // Nombre de lignes par page issu de la configuration (PAGINATION_TAILLE_DEFAUT).
  const PAGE_SIZE = useTaillePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ModePaiementAdmin | null>(null)
  const [confirmToggle, setConfirmToggle] = useState<{ id: number; actif: boolean } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, statut, setStatut, reload } =
    useModesPaiementList(PAGE_SIZE)

  const actifs = items.filter((m) => m.actif).length
  const inactifs = items.length - actifs
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const handleSave = async (payload: CreateModePaiementPayload, id?: number) => {
    if (id != null) {
      await modesPaiementApi.update(id, payload)
    } else {
      await modesPaiementApi.create(payload)
    }
    setModalOpen(false)
    setEditing(null)
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    setActionError(null)
    try {
      await modesPaiementApi.setActif(id, actif)
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
          <h1>Modes de paiement</h1>
          <p>Gérez les moyens de règlement proposés lors de l&apos;enregistrement des paiements.</p>
        </div>
        <div className="actions">
          <Can permission="MODE_PAIEMENT_UPDATE">
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
              Ajouter un mode
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
              <rect x="2" y="5" width="20" height="14" rx="2" />
              <path d="M2 10h20" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Modes de paiement</div>
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
              onChange={(e) => setStatut(e.target.value as ModePaiementFilter)}
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
          <Spinner label="Chargement des modes de paiement…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <ModesPaiementTable
                items={items}
                onEdit={(m) => {
                  setEditing(m)
                  setModalOpen(true)
                }}
                onToggleActif={(id, actif) => setConfirmToggle({ id, actif })}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à{' '}
                  {Math.min((page + 1) * PAGE_SIZE, total)} sur {total} modes de paiement
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

      <ModePaiementFormModal
        open={modalOpen}
        mode={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmToggle !== null}
        titre={confirmToggle?.actif ? 'Activer ce mode ?' : 'Désactiver ce mode ?'}
        message={
          confirmToggle?.actif
            ? 'Ce mode sera de nouveau proposé lors de l’enregistrement des paiements.'
            : 'Ce mode ne sera plus proposé pour les nouveaux paiements. Les paiements existants sont conservés.'
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
