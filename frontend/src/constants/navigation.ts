import type { IconName } from '@/components/ui'
import type { NomRole } from '@/types'

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

const dashboard: NavItem = {
  label: 'Tableau de bord',
  to: '/',
  icon: 'grid',
  end: true,
  permission: 'DASHBOARD_VIEW',
}

const clients: NavItem = { label: 'Clients', to: '/clients', icon: 'users' }
const clientsAvecSousMenus: NavItem = {
  ...clients,
  deroulant: true,
  permission: 'CLIENT_VIEW',
  children: [
    { label: 'Liste des clients', to: '/clients', end: true, permission: 'CLIENT_VIEW' },
    { label: 'Responsables', to: '/responsables', permission: 'RESPONSABLE_VIEW' },
    { label: 'Domaines', to: '/domaines', permission: 'DOMAINE_VIEW' },
  ],
}
const clientsSimple: NavItem = {
  ...clients,
  deroulant: true,
  permission: 'CLIENT_VIEW',
  children: [{ label: 'Liste des clients', to: '/clients', end: true, permission: 'CLIENT_VIEW' }],
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
  permission: 'SERVICE_VIEW',
  children: [{ label: 'Liste des services', to: '/services', end: true, permission: 'SERVICE_VIEW' }],
}

const demandes: NavItem = {
  label: 'Demandes',
  to: '/demandes',
  icon: 'file',
  badge: 'demandes',
  deroulant: true,
  permission: 'DEMANDE_VIEW',
  children: [{ label: 'Liste des demandes', to: '/demandes', end: true, permission: 'DEMANDE_VIEW' }],
}
const demandesLectureSeule: NavItem = {
  label: 'Demandes',
  to: '/demandes',
  icon: 'file',
  // hint: 'lecture seule',
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
const rapportsDemandes: NavItem = {
  label: 'Rapports',
  to: '/rapports',
  icon: 'chart',
  hint: 'demandes',
  permission: 'RAPPORT_DEMANDE_VIEW',
}

const administration: NavItem = {
  label: 'Administration',
  to: '/administration',
  icon: 'gear',
  permission: ['USER_VIEW', 'AUDIT_VIEW', 'CONFIG_VIEW', 'PASSWORD_RESET_MANAGE', 'BACKUP_VIEW'],
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
    { label: 'Sauvegardes', to: '/administration/sauvegardes', permission: 'BACKUP_VIEW' },
  ],
}

/** Menu par rôle. */
export const NAVIGATION: Record<NomRole, NavSection[]> = {
  SUPER_ADMIN: [
    {
      label: 'Activité',
      items: [
        dashboard,
        clientsAvecSousMenus,
        commerciaux,
        services,
        demandes,
        paiements,
      ],
    },
    { label: 'Pilotage', items: [notifications, rapports, administration] },
  ],
  ADMIN: [
    {
      label: 'Activité',
      items: [
        dashboard,
        clientsAvecSousMenus,
        commerciaux,
        services,
        demandes,
        paiements,
      ],
    },
    { label: 'Pilotage', items: [notifications, rapports, administration] },
  ],
  ACCUEIL: [
    {
      label: 'Activité',
      items: [dashboard, clientsAvecSousMenus, commerciaux, services, demandes],
    },
    { label: 'Pilotage', items: [rapportsDemandes, notifications] },
  ],
  ECONOMAT: [
    {
      label: 'Activité',
      items: [dashboard, clientsSimple, demandesLectureSeule, paiements],
    },
    { label: 'Pilotage', items: [rapports, notifications] },
  ],
}