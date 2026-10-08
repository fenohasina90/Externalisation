import { useEffect, useMemo, useState } from 'react'
import type { Utilisateur } from '@/features/auth/types'
import { UtilisateurFormModal, UtilisateursTable } from '../components'
import { MOCK_UTILISATEURS, type RoleFilter, type StatutUtilisateurFilter } from '../data/mockUtilisateurs'
import { useUtilisateursListStatic } from '../hooks'

const PAGE_SIZE = 8

/**
 * Utilisateurs — étape 1 : interface seule, données statiques.
 * Réservé au SUPER_ADMIN (contrôle backend à l'étape 2).
 */
export function UtilisateursPage() {
  const [utilisateurs, setUtilisateurs] = useState<Utilisateur[]>(MOCK_UTILISATEURS)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Utilisateur | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const { items, total, totalPages, page, setPage, search, setSearch, role, setRole, statut, setStatut } =
    useUtilisateursListStatic({ source: utilisateurs, size: PAGE_SIZE })

  const kpis = useMemo(() => {
    const actifs = utilisateurs.filter((u) => u.actif).length
    const totalCount = utilisateurs.length
    return {
      total: totalCount,
      actifs,
      inactifs: totalCount - actifs,
      pctActifs: totalCount === 0 ? 0 : Math.round((actifs / totalCount) * 100),
      connectes: utilisateurs.filter((u) => u.derniereConnexion?.startsWith("Aujourd'hui")).length,
    }
  }, [utilisateurs])

  const openCreate = () => {
    setEditing(null)
    setModalOpen(true)
  }

  const handleSave = (payload: {
    nom: string
    prenom: string
    username: string
    email: string
    role: RoleFilter
  }) => {
    if (editing) {
      // Modification : le statut existant est conservé (géré par l'action Activer/Désactiver).
      setUtilisateurs((prev) =>
        prev.map((u) =>
          u.id === editing.id
            ? {
                ...u,
                nom: payload.nom,
                prenom: payload.prenom,
                username: payload.username,
                email: payload.email,
                role:
                  payload.role === 'tous'
                    ? u.role
                    : { ...u.role, nomRole: payload.role as Utilisateur['role']['nomRole'] },
              }
            : u,
        ),
      )
    } else {
      // Création : compte actif par défaut.
      const nextId = Math.max(0, ...utilisateurs.map((u) => u.id)) + 1
      const nomRole = (payload.role === 'tous' ? 'ACCUEIL' : payload.role) as Utilisateur['role']['nomRole']
      setUtilisateurs((prev) => [
        ...prev,
        {
          id: nextId,
          nom: payload.nom,
          prenom: payload.prenom,
          username: payload.username,
          email: payload.email,
          actif: true,
          doitChangerMotDePasse: true,
          derniereConnexion: null,
          createdAt: '2026-10-04',
          role: { id: nextId, nomRole, description: null, actif: true },
          permissions: [],
        },
      ])
    }
    setModalOpen(false)
    setEditing(null)
  }

  const handleResetPassword = (u: Utilisateur) => {
    setMessage(
      `Mot de passe temporaire généré pour ${u.prenom} ${u.nom} : Cbx-${u.id}248 (démonstration, sans backend).`,
    )
  }

  const handleToggleActif = (id: number, actif: boolean) => {
    setUtilisateurs((prev) => prev.map((u) => (u.id === id ? { ...u, actif } : u)))
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
        <div className="alert alert-success" role="status">
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
            <div className="kpi-num">{kpis.total}</div>
            <div className="kpi-label">Utilisateurs</div>
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
            <div className="kpi-num">{kpis.inactifs}</div>
            <div className="kpi-label">Inactifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Dernières 24 h</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.connectes}</div>
            <div className="kpi-label">Connectés aujourd&apos;hui</div>
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
            <select value={role} onChange={(e) => setRole(e.target.value as RoleFilter)}>
              <option value="tous">Tous les rôles</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
              <option value="ACCUEIL">ACCUEIL</option>
              <option value="ECONOMAT">ECONOMAT</option>
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
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <UtilisateursTable
              items={items}
              onEdit={(u) => {
                setEditing(u)
                setModalOpen(true)
              }}
              onResetPassword={handleResetPassword}
              onToggleActif={handleToggleActif}
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
      </section>

      <UtilisateurFormModal
        open={modalOpen}
        utilisateur={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />
    </div>
  )
}
