import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function NotificationsPage() {
  return (
    <PagePlaceholder
      titre="Notifications"
      sousTitre="Alertes et notifications de l’application"
      fonctionnalites={[
        'Consulter les notifications (nouvelle demande, statut, échéance)',
        'Marquer une notification comme lue',
        'Marquer toutes les notifications comme lues',
      ]}
    />
  )
}