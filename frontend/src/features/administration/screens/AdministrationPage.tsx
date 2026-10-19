import { Link } from 'react-router-dom'
import { PageHeader } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import type { Role, Utilisateur } from '@/features/auth/types'
import type { AuditEntry } from '../types'
import { auditApi, rolesApi, utilisateursApi } from '../services/administration.service'

const SECTIONS = [
  {
    to: '/administration/utilisateurs',
    titre: 'Utilisateurs',
    description: 'Créez les comptes, attribuez les rôles, activez ou désactivez les accès.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="9" cy="8" r="3.5" />
        <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
      </svg>
    ),
  },
  {
    to: '/administration/roles',
    titre: 'Rôles & permissions',
    description: 'Définissez ce que chaque rôle peut voir et faire, module par module.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
      </svg>
    ),
  },
  {
    to: '/administration/reinitialisations',
    titre: 'Demandes de réinitialisation',
    description: 'Approuvez ou refusez les demandes de nouveau mot de passe.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <rect x="3" y="7" width="18" height="13" rx="2" />
        <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3 13h18" />
      </svg>
    ),
  },
  {
    to: '/administration/audit',
    titre: "Journal d'audit",
    description: 'Tracez les opérations importantes : connexions, créations, paiements, statuts.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
      </svg>
    ),
  },
  {
    to: '/administration/configuration',
    titre: 'Configuration',
    description: 'Nom de l’application, devise, alertes, fichiers, session et affichage.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="3" />
        <path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2" />
      </svg>
    ),
  },
  {
    to: '/administration/sauvegardes',
    titre: 'Sauvegardes',
    description: 'Sauvegardez, téléchargez et restaurez la base de données, localement ou chiffrée.',
    icone: (
      <svg className="sv" viewBox="0 0 24 24">
        <ellipse cx="12" cy="5" rx="8" ry="3" />
        <path d="M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 11v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6" />
      </svg>
    ),
  },
]

/** Accueil de l'administration : chiffres clés + accès aux sections. */
export function AdministrationPage() {
  const { data: pageTotal } = useApi<ApiPage<Utilisateur>>(
    () => utilisateursApi.list({ page: 0, size: 1, recherche: null }),
    [],
  )
  const { data: pageActifs } = useApi<ApiPage<Utilisateur>>(
    () => utilisateursApi.list({ page: 0, size: 1, recherche: null }, { actif: true }),
    [],
  )
  const { data: roles } = useApi<Role[]>(() => rolesApi.list(), [])
  const nbUtilisateurs = pageTotal?.totalElements ?? 0
  const nbActifs = pageActifs?.totalElements ?? 0
  const { data: audit } = useApi<ApiPage<AuditEntry>>(
    () => auditApi.list({ page: 0, size: 1, recherche: null }),
    [],
  )
  const nbTraces = audit?.totalElements ?? 0

  return (
    <div className="app-page responsables-page">
      <PageHeader titre="Administration" sousTitre="Pilotage des accès, de la traçabilité et des paramètres" />

      <section className="kpi-grid">
        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nbUtilisateurs}</div>
            <div className="kpi-label">Utilisateurs ({nbActifs} actifs)</div>
          </div>
        </div>

        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{roles?.length ?? 0}</div>
            <div className="kpi-label">Rôles configurés</div>
          </div>
        </div>

        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{nbTraces}</div>
            <div className="kpi-label">Traces d&apos;audit</div>
          </div>
        </div>
      </section>

      <div className="cards-grid">
        {SECTIONS.map((section) => (
          <Link key={section.to} to={section.to} className="admin-card">
            <span style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span className="kpi-icon">{section.icone}</span>
              <h3 style={{ margin: 0 }}>{section.titre}</h3>
            </span>
            <p>{section.description}</p>
            <span className="lk">Ouvrir ›</span>
          </Link>
        ))}
      </div>

      <div className="alert alert-info" style={{ marginBottom: 0 }}>
        Réservé aux administrateurs : chaque action sensible (création de compte, changement de
        rôle, réinitialisation) est enregistrée dans le journal d&apos;audit.
      </div>
    </div>
  )
}
