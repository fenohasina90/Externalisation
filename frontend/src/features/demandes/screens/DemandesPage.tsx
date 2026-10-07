import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function DemandesPage() {
  return (
    <PagePlaceholder
      titre="Demandes de service"
      sousTitre="Cycle de vie : EN ATTENTE → VALIDATION → EN COURS → TERMINÉE"
      fonctionnalites={[
        'Créer une demande avec référence unique et tarif instantané',
        'Consulter les demandes et leur historique de statuts',
        'Changer le statut d’une demande (validation, clôture, annulation)',
      ]}
    />
  )
}