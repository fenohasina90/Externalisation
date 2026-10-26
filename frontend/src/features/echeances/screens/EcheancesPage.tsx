import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function EcheancesPage() {
  return (
    <PagePlaceholder
      titre="Échéances"
      sousTitre="Suivi des échéances de paiement"
      fonctionnalites={[
        'Consulter les échéances à venir et en retard',
        'Visualiser les états À JOUR / PROCHE / EN RETARD',
        'Enregistrer les paiements au regard d’une échéance',
      ]}
    />
  )
}