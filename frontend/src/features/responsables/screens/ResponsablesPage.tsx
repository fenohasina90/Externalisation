import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function ResponsablesPage() {
  return (
    <PagePlaceholder
      titre="Responsables client"
      sousTitre="Gestion des responsables des sociétés clientes"
      fonctionnalites={[
        'Consulter la liste des responsables (nom, fonction, e-mail, téléphone)',
        'Créer et modifier un responsable rattaché à une société',
        'Activer ou désactiver un responsable',
      ]}
    />
  )
}