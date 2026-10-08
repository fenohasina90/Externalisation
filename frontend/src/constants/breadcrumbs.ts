export interface Crumb {
  label: string
  /** Absent pour la page courante. */
  to?: string
}

const SEGMENT_LABELS: Record<string, string> = {
  dashboard: 'Tableau de bord',
  clients: 'Clients',
  responsables: 'Responsables',
  commerciaux: 'Commerciaux',
  services: 'Services',
  domaines: 'Domaines',
  tarifs: 'Tarifs',
  demandes: 'Demandes',
  nouveau: 'Nouvelle demande',
  paiements: 'Paiements',
  echeanciers: 'Échéanciers',
  historique: 'Historique',
  notifications: 'Notifications',
  rapports: 'Rapports',
  administration: 'Administration',
  utilisateurs: 'Utilisateurs',
  reinitialisations: 'Réinitialisations',
  roles: 'Rôles et permissions',
  audit: 'Audit',
  configuration: 'Configuration',
  profil: 'Mon profil',
}

/** Construit le fil d'Ariane à partir de l'URL. */
export function buildBreadcrumbs(pathname: string): Crumb[] {
  const segments = pathname.split('/').filter(Boolean)
  let path = ''
  return segments.map((segment, index) => {
    path += `/${segment}`
    let label: string
    if (/^\d+$/.test(segment)) {
      label = 'Détail'
    } else if (segment === 'nouveau') {
      // « Nouveau » dépend du module parent (demandes / paiements).
      label = segments[index - 1] === 'paiements' ? 'Nouveau paiement' : 'Nouvelle demande'
    } else {
      label = SEGMENT_LABELS[segment] ?? segment
    }
    return { label, to: index < segments.length - 1 ? path : undefined }
  })
}