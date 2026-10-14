import { useEffect, useState } from 'react'
import type { Role, Utilisateur } from '@/features/auth/types'
import { authApi } from '@/features/auth/services/auth.service'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { UtilisateurFormModal, UtilisateursTable } from '../components'
import { reinitialisationsApi, rolesApi, utilisateursApi } from '../services/administration.service'
import { useUtilisateursList, type StatutUtilisateurFilter } from '../hooks/useUtilisateursList'

const PAGE_SIZE = 8

/**
 * Utilisateurs branchés sur l'API : recherche, filtres rôle/statut,
 * création (mot de passe initial), activation, réinitialisation du
 * mot de passe via le circuit des demandes (création → approbation →
 * génération du temporaire affiché une seule fois).
 */
export function UtilisateursPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Utilisateur | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [confirmToggle, setConfirmToggle] = useState<{ id: number; actif: boolean } | null>(null)
  const [resetCible, setResetCible] = useState<Utilisateur | null>(null)
  const [resetEnCours, setResetEnCours] = useState(false)
  const [motDePasseTemp, setMotDePasseTemp] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, search, setSearch, role, setRole, statut, setStatut, reload } =
    useUtilisateursList(PAGE_SIZE)

  const { data: roles } = useApi<Role[]>(() => rolesApi.list(), [])
  const optionsRoles = (roles ?? []).map((r) => ({ id: r.id, nom: r.nomRole }))

  const actifs = items.filter((u) => u.actif).length
  const inactifs = items.length - actifs
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const openCreate = () => {
    setEditing(null)
    setModalOpen(true)
  }

  const handleSave = async (payload: {
    nom: string
    prenom: string
    username: string
    email: string
    password?: string
    roleId: number
  }) => {
    if (editing) {
      await utilisateursApi.update(editing.id, {
        nom: payload.nom,
        prenom: payload.prenom,
        username: payload.username,
        email: payload.email,
        roleId: payload.roleId,
      })
    } else {
      if (!payload.password) throw new Error('Le mot de passe est obligatoire')
      await utilisateursApi.create({
        nom: payload.nom,
        prenom: payload.prenom,
        username: payload.username,
        email: payload.email,
        password: payload.password,
        roleId: payload.roleId,
      })
    }
    setModalOpen(false)
    setEditing(null)
    reload()
  }

  const handleToggleActif = async (id: number, actif: boolean) => {
    setMessage(null)
    try {
      await utilisateursApi.toggleActivation(id, actif)
      reload()
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Échec du changement de statut')
    }
  }

  /** Chaîne admin : demande → approbation → mot de passe temporaire affiché une fois. */
  const handleResetPassword = async (u: Utilisateur) => {
    setResetCible(u)
    setResetEnCours(true)
    setMotDePasseTemp(null)
    setMessage(null)
    try {
      await authApi.forgotPassword({ email: u.email })
      const pageDemandes = await reinitialisationsApi.list('EN_ATTENTE', {
        page: 0,
        size: 50,
        recherche: null,
      })
      const demande = pageDemandes.content.find(
        (d) => d.email.toLowerCase() === u.email.toLowerCase(),
      )
      if (!demande) {
        throw new Error('Demande de réinitialisation introuvable après création')
      }
      await reinitialisationsApi.approuver(demande.id)
      const resultat = await reinitialisationsApi.reinitialiser(demande.id)
      setMotDePasseTemp(resultat.motDePasseTemporaire)
    } catch (err) {
      setResetCible(null)
      setMessage(err instanceof Error ? err.message : 'Échec de la réinitialisation')
    } finally {
      setResetEnCours(false)
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
          <h1>Utilisateurs</h1>
          <p>Gérez les comptes et leurs rôles. Réservé au SUPER_ADMIN.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-primary" onClick={openCreate}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Créer un utilisateur
          </button>
        </div>
      </header>

      {message && (
        <div className="alert alert-danger" role="alert">
          {message}
        </div>
      )}

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
            <div className="kpi-label">Utilisateurs</div>
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
            Recherche
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Nom, email…"
            />
          </label>
          <label className="field filter-field">
            Rôle
            <select value={role} onChange={(e) => setRole(e.target.value, optionsRoles)}>
              <option value="tous">Tous les rôles</option>
              {optionsRoles.map((r) => (
                <option key={r.id} value={r.nom}>
                  {r.nom}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Statut
            <select value={statut} onChange={(e) => setStatut(e.target.value as StatutUtilisateurFilter)}>
              <option value="tous">Tous</option>
              <option value="actifs">Actifs</option>
              <option value="inactifs">Inactifs</option>
            </select>
          </label>
          <button type="submit" className="btn-primary">
            Rechercher
          </button>
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des utilisateurs…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <UtilisateursTable
                items={items}
                onEdit={(u) => {
                  setEditing(u)
                  setModalOpen(true)
                }}
                onResetPassword={(u) => void handleResetPassword(u)}
                onToggleActif={(id, actif) => setConfirmToggle({ id, actif })}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
                <span>
                  Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                  {total} utilisateurs
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

      <UtilisateurFormModal
        open={modalOpen}
        utilisateur={editing}
        roles={optionsRoles}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={confirmToggle !== null}
        titre={confirmToggle?.actif ? 'Activer ce compte ?' : 'Désactiver ce compte ?'}
        message={
          confirmToggle?.actif
            ? 'Cet utilisateur pourra de nouveau se connecter à l’application.'
            : 'Cet utilisateur ne pourra plus se connecter. Ses données sont conservées.'
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

      {(resetCible !== null || motDePasseTemp !== null) && (
        <div
          className="modal-backdrop"
          onClick={() => {
            if (!resetEnCours) {
              setResetCible(null)
              setMotDePasseTemp(null)
            }
          }}
        >
          <div
            className="modal-card"
            style={{ maxWidth: 480 }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-label="Mot de passe temporaire"
          >
            <header className="modal-header">
              <h3 className="modal-title">Réinitialisation du mot de passe</h3>
              <button
                type="button"
                className="btn-ghost"
                disabled={resetEnCours}
                onClick={() => {
                  setResetCible(null)
                  setMotDePasseTemp(null)
                }}
                aria-label="Fermer"
              >
                ✕
              </button>
            </header>
            <div className="modal-body">
              {resetEnCours && <p className="text-muted">Génération en cours…</p>}
              {!resetEnCours && motDePasseTemp && resetCible && (
                <>
                  <p className="section-note">
                    Transmettez ce mot de passe temporaire à {resetCible.prenom} {resetCible.nom} :
                    il devra le changer à sa prochaine connexion. Il ne sera plus affiché.
                  </p>
                  <div className="mot-de-passe-temp">
                    <code className="code-inline">{motDePasseTemp}</code>
                    <button
                      type="button"
                      className="btn-ghost"
                      onClick={() => void navigator.clipboard.writeText(motDePasseTemp)}
                    >
                      Copier
                    </button>
                  </div>
                </>
              )}
            </div>
            <footer className="modal-footer">
              <button
                type="button"
                className="btn-primary"
                disabled={resetEnCours}
                onClick={() => {
                  setResetCible(null)
                  setMotDePasseTemp(null)
                  reload()
                }}
              >
                Fermer
              </button>
            </footer>
          </div>
        </div>
      )}
    </div>
  )
}
