import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function ConfigurationPage() {
  return (
    <PagePlaceholder
      titre="Configuration"
      sousTitre="Paramètres généraux de l’application"
      fonctionnalites={[
        'Nom de l’application et devise',
        'Nombre de jours avant alerte d’échéance',
        'Taux de TVA, taille maximale des fichiers, durée de session',
      ]}
    />
  )
}