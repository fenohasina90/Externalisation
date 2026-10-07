import { PagePlaceholder } from '@/components/shared/PagePlaceholder'

export function AuditPage() {
  return (
    <PagePlaceholder
      titre="Journal d'audit"
      sousTitre="Trace des opérations importantes"
      fonctionnalites={[
        'Consulter la liste des opérations (connexions, CRUD, statuts, paiements)',
        'Filtrer par utilisateur, action, module, entité et période',
        'Consulter le détail d’une trace (valeurs avant/après, adresse IP)',
      ]}
    />
  )
}