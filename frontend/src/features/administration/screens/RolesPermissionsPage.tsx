import { useState } from 'react'
import { PermissionsMatrix } from '../components'
import {
  PERMISSIONS_PAR_ROLE,
  ROLES_CARTES,
  type PermissionLigne,
} from '../data/mockRolesPermissions'

/**
 * Rôles et permissions — étape 1 : interface seule, données statiques.
 * Sélection du rôle à gauche, matrice des permissions à droite.
 */
export function RolesPermissionsPage() {
  const [roleSelectionne, setRoleSelectionne] = useState('ACCUEIL')
  const [matrices, setMatrices] = useState<Record<string, PermissionLigne[]>>(PERMISSIONS_PAR_ROLE)
  const [message, setMessage] = useState<string | null>(null)

  const carte = ROLES_CARTES.find((r) => r.nom === roleSelectionne)
  const lignes = matrices[roleSelectionne] ?? []

  const handleToggle = (module: string, champ: 'voir' | 'creer' | 'modifier' | 'desactiver') => {
    if (!carte?.modifiable) return
    setMatrices((prev) => ({
      ...prev,
      [roleSelectionne]: prev[roleSelectionne].map((l) =>
        l.module === module ? { ...l, [champ]: !l[champ] } : l,
      ),
    }))
    setMessage(null)
  }

  const handleAnnuler = () => {
    setMatrices(PERMISSIONS_PAR_ROLE)
    setMessage(null)
  }

  const handleEnregistrer = () => {
    setMessage(`Permissions du rôle ${roleSelectionne} enregistrées (démonstration, sans backend).`)
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Rôles et permissions</h1>
          <p>Définissez ce que chaque rôle peut voir et faire.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleAnnuler}>
            Annuler
          </button>
          <button type="button" className="btn-primary" onClick={handleEnregistrer}>
            Enregistrer les permissions
          </button>
        </div>
      </header>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}

      <div className="roles-layout">
        <section aria-label="Rôles">
          {ROLES_CARTES.map((r) => (
            <button
              key={r.nom}
              type="button"
              onClick={() => {
                setRoleSelectionne(r.nom)
                setMessage(null)
              }}
              className={`card role-card ${r.nom === roleSelectionne ? 'role-card--selected' : ''}`}
            >
              <span className="role-card__head">
                <span className="role-card__name">{r.nom}</span>
                <span className="pill wt">
                  ● {r.utilisateurs} utilisateur{r.utilisateurs > 1 ? 's' : ''}
                </span>
              </span>
              <span className="role-card__desc">{r.description}</span>
            </button>
          ))}
        </section>

        <section className="card">
          <div className="card-pad" style={{ paddingBottom: 0 }}>
            <h3 style={{ margin: 0 }}>Permissions du rôle {roleSelectionne}</h3>
            {!carte?.modifiable && (
              <p className="section-note">Accès complet. Non modifiable.</p>
            )}
          </div>
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <PermissionsMatrix
                lignes={lignes}
                modifiable={carte?.modifiable ?? false}
                onToggle={handleToggle}
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
