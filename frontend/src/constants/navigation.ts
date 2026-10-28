import type { IconName } from '@/components/ui'

export type BadgeKey = 'demandes' | 'notifications'

export interface NavChild {
  label: string
  to: string
  /** Actif uniquement sur l'URL exacte (sinon sur tout le préfixe). */
  end?: boolean
  /**
   * Permission requise pour voir l'entrée (une seule ou liste = l'une suffit).
   * Absent = visible à tous les connectés (ex. notifications, sans permission dédiée).
   */
  permission?: string | string[]
}

export interface NavItem {
  label: string
  to: string
  icon: IconName
  end?: boolean
  badge?: BadgeKey
  hint?: string
  children?: NavChild[]
  /**
   * Permission requise pour voir l'entrée (une seule ou liste = l'une suffit).
   * Absent = visible à tous les connectés (ex. notifications, sans permission dédiée).
   */
  permission?: string | string[]
  /**
   * Menu déroulant : le clic déplie/replie le sous-menu au lieu de
   * naviguer (la navigation se fait par les sous-menus).
   */
  deroulant?: boolean
}

export interface NavSection {
  label: string
  items: NavItem[]
}

const ADMIN_MENU_PERMISSIONS = [
  'USER_VIEW',
  'AUDIT_VIEW',
  'CONFIG_VIEW',
  'PASSWORD_RESET_MANAGE',
  'BACKUP_VIEW',
] as const

const dashboard: NavItem = {
  label: 'Tableau de bord',
  to: '/',
  icon: 'grid',
  end: true,
  permission: 'DASHBOARD_VIEW',
}

const clients: NavItem = {
  label: 'Clients',
  to: '/clients',
  icon: 'users',
  deroulant: true,
  permission: ['CLIENT_VIEW', 'RESPONSABLE_VIEW'],
  children: [
    { label: 'Liste des clients', to: '/clients', end: true, permission: 'CLIENT_VIEW' },
    { label: 'Responsables', to: '/responsables', permission: 'RESPONSABLE_VIEW' },
  ],
}

const commerciaux: NavItem = {
  label: 'Commerciaux',
  to: '/commerciaux',
  icon: 'briefcase',
  deroulant: true,
  permission: 'COMMERCIAL_VIEW',
  children: [
    {
      label: 'Liste des commerciaux',
      to: '/commerciaux',
      end: true,
      permission: 'COMMERCIAL_VIEW',
    },
  ],
}

const services: NavItem = {
  label: 'Services',
  to: '/services',
  icon: 'layers',
  deroulant: true,
  permission: ['SERVICE_VIEW', 'DOMAINE_VIEW'],
  children: [
    { label: 'Liste des services', to: '/services', end: true, permission: 'SERVICE_VIEW' },
    { label: 'Domaines', to: '/domaines', permission: 'DOMAINE_VIEW' },
  ],
}

const demandes: NavItem = {
  label: 'Demandes',
  to: '/demandes',
  icon: 'file',
  deroulant: true,
  permission: 'DEMANDE_VIEW',
  children: [{ label: 'Liste des demandes', to: '/demandes', end: true, permission: 'DEMANDE_VIEW' }],
}

const paiements: NavItem = {
  label: 'Paiements',
  to: '/paiements',
  icon: 'card',
  deroulant: true,
  permission: 'PAIEMENT_VIEW',
  children: [
    { label: 'Paiements', to: '/paiements', end: true, permission: 'PAIEMENT_VIEW' },
    { label: 'Échéanciers', to: '/paiements/echeanciers', permission: 'PAIEMENT_VIEW' },
    { label: 'Historique', to: '/paiements/historique', permission: 'PAIEMENT_VIEW' },
  ],
}

const notifications: NavItem = {
  label: 'Notifications',
  to: '/notifications',
  icon: 'bell',
  badge: 'notifications',
}

const rapports: NavItem = {
  label: 'Rapports',
  to: '/rapports',
  icon: 'chart',
  permission: ['RAPPORT_DEMANDE_VIEW', 'RAPPORT_PAIEMENT_VIEW'],
}

const administration: NavItem = {
  label: 'Administration',
  to: '/administration',
  icon: 'gear',
  deroulant: true,
  permission: [...ADMIN_MENU_PERMISSIONS],
  children: [
    { label: 'Utilisateurs', to: '/administration/utilisateurs', permission: 'USER_VIEW' },
    { label: 'Rôles & permissions', to: '/administration/roles', permission: 'USER_VIEW' },
    {
      label: 'Réinitialisations',
      to: '/administration/reinitialisations',
      permission: 'PASSWORD_RESET_MANAGE',
    },
    { label: 'Audit', to: '/administration/audit', permission: 'AUDIT_VIEW' },
    { label: 'Configuration', to: '/administration/configuration', permission: 'CONFIG_VIEW' },
    { label: 'Statuts', to: '/administration/statuts', permission: 'CONFIG_VIEW' },
    {
      label: 'Modes de paiement',
      to: '/administration/modes-paiement',
      permission: 'MODE_PAIEMENT_VIEW',
    },
    { label: 'Sauvegardes', to: '/administration/sauvegardes', permission: 'BACKUP_VIEW' },
  ],
}

/**
 * Accès direct pour les rôles qui possèdent `MODE_PAIEMENT_VIEW` sans
 * accès au menu Administration (ex. Économat).
 */
const modesPaiement: NavItem = {
  label: 'Modes de paiement',
  to: '/administration/modes-paiement',
  icon: 'card',
  permission: 'MODE_PAIEMENT_VIEW',
}

/**
 * Navigation construite à partir des permissions effectives, et non d'une
 * liste figée par rôle : un droit accordé ou retiré apparaît/disparaît
 * immédiatement dans les menus, sans reconnexion.
 */
export function navigationPour(
  hasPermission: (permission: string) => boolean,
): NavSection[] {
  const adminVisible = ADMIN_MENU_PERMISSIONS.some(hasPermission)
  const rapportDemandes = hasPermission('RAPPORT_DEMANDE_VIEW')
  const rapportFinancier = hasPermission('RAPPORT_PAIEMENT_VIEW')
  const lectureSeuleDemandes =
    hasPermission('DEMANDE_VIEW') &&
    !hasPermission('DEMANDE_CREATE') &&
    !hasPermission('DEMANDE_UPDATE')

  const demandesItem: NavItem = {
    ...demandes,
    hint: lectureSeuleDemandes ? '' : undefined,
  }
  const rapportsItem: NavItem = {
    ...rapports,
    hint: rapportDemandes && !rapportFinancier
      ? ''
      : !rapportDemandes && rapportFinancier
        ? ''
        : undefined,
  }

  const pilotage = [notifications, rapportsItem]

  // Évite le doublon avec l'entrée « Modes de paiement » du menu Administration.
  if (!adminVisible) pilotage.push(modesPaiement)
  pilotage.push(administration)

  return [
    {
      label: 'Activité',
      items: [dashboard, clients, commerciaux, services, demandesItem, paiements],
    },
    { label: 'Pilotage', items: pilotage },
  ]
}
