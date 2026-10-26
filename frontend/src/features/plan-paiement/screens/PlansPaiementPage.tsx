import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function PlansPaiementPage() {
  return (
    <PagePlaceholder
      titre="Plans de paiement"
      sousTitre="Planification des paiements d’une demande"
      fonctionnalites={[
        'Créer un plan de paiement pour une demande (dès EN ATTENTE)',
        'Découper le plan en échéances dans la période de service',
        'Suivre le reste à payer et les échéances payées',
      ]}
    />
  )
}