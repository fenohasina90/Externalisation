import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function CommerciauxPage() {
  return (
    <PagePlaceholder
      titre="Commerciaux"
      sousTitre="Gestion des commerciaux intervenant sur les demandes"
      fonctionnalites={[
        'Consulter la liste des commerciaux',
        'Créer et modifier un commercial',
        'Activer ou désactiver un commercial',
      ]}
    />
  )
}