import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function RapportsPage() {
  return (
    <PagePlaceholder
      titre="Rapports"
      sousTitre="Rapports par période, client, service, statut ou commercial"
      fonctionnalites={[
        'Rapport général et rapport financier',
        'Rapports par client, service ou commercial',
        'Export des rapports selon la permission de l’utilisateur',
      ]}
    />
  )
}