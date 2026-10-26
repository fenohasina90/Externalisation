import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function DocumentsPage() {
  return (
    <PagePlaceholder
      titre="Documents"
      sousTitre="Pièces jointes et documents générés"
      fonctionnalites={[
        'Consulter les documents d’une demande',
        'Télécharger le récapitulatif de demande PDF',
        'Télécharger le reçu de paiement PDF',
        'Gérer les pièces jointes',
      ]}
    />
  )
}