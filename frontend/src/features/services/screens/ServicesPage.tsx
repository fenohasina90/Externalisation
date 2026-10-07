import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function ServicesPage() {
  return (
    <PagePlaceholder
      titre="Services & domaines"
      sousTitre="Référentiel des domaines d’activité et des services externalisés"
      fonctionnalites={[
        'Gérer les domaines d’activité',
        'Gérer les services et leurs tarifs',
        'Consulter l’historique des tarifs d’un service',
      ]}
    />
  )
}