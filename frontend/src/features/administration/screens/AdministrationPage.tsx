import { PageHeader } from '@/components/ui'
import { Link } from 'react-router-dom'

const SECTIONS_ADMINISTRATION = [
  {
    to: '/administration/utilisateurs',
    titre: 'Utilisateurs',
    description: 'Gestion des comptes et de leurs rôles.',
  },
  {
    to: '/administration/roles',
    titre: 'Rôles & permissions',
    description: 'Définir ce que chaque rôle peut voir et faire.',
  },
  {
    to: '/administration/reinitialisations',
    titre: 'Demandes de réinitialisation',
    description: 'Approuver ou refuser les demandes de réinitialisation de mot de passe.',
  },
  {
    to: '/administration/audit',
    titre: "Journal d'audit",
    description: 'Traçabilité des opérations importantes de l’application.',
  },
  {
    to: '/administration/configuration',
    titre: 'Configuration',
    description: 'Paramètres généraux de l’application.',
  },
]

export function AdministrationPage() {
  return (
    <>
      <PageHeader
        titre="Administration"
        sousTitre="Réservé aux administrateurs"
      />
      <div className="cards-grid">
        {SECTIONS_ADMINISTRATION.map((section) => (
          <Link key={section.to} to={section.to} className="admin-card">
            <h3>{section.titre}</h3>
            <p>{section.description}</p>
          </Link>
        ))}
      </div>
    </>
  )
}