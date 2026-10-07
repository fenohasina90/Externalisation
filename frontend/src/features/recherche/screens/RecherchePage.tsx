import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function RecherchePage() {
  return (
    <PagePlaceholder
      titre="Recherche globale"
      sousTitre="Recherche dans l’ensemble des objets de l’application"
      fonctionnalites={[
        'Rechercher des clients, demandes, paiements, responsables, services',
        'Résultats filtrés selon la permission de l’utilisateur',
      ]}
    />
  )
}