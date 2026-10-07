import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function PaiementsPage() {
  return (
    <PagePlaceholder
      titre="Paiements"
      sousTitre="Enregistrement et suivi des paiements"
      fonctionnalites={[
        'Enregistrer un paiement (espèces ou virement avec référence)',
        'Consulter les paiements par période, client ou mode',
        'Générer le reçu de paiement PDF',
      ]}
    />
  )
}