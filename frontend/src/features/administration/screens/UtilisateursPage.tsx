import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function UtilisateursPage() {
  return (
    <PagePlaceholder
      titre="Utilisateurs & rôles"
      sousTitre="Gestion des comptes, rôles et permissions"
      fonctionnalites={[
        'Créer et modifier un utilisateur (identifiant unique, e-mail, rôle)',
        'Activer ou désactiver un compte',
        'Consulter les rôles et permissions (Super Administrateur, Accueil, Économat)',
      ]}
    />
  )
}