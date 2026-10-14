import type { IconName } from '@/components/ui'
import type { NomRole } from '@/types'

export type BadgeKey = 'demandes' | 'notifications'

export interface NavChild {
  label: string
  to: string
  /** Actif uniquement sur l'URL exacte (sinon sur tout le préfixe). */
  end?: boolean
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
   * Menu déroulant : le clic déplie/replie le sous-menu au lieu de
   * naviguer (la navigation se fait par les sous-menus).
   */
  deroulant?: boolean
}

export interface NavSection {
  label: string
  items: NavItem[]
}

const dashboard: NavItem = { label: 'Tableau de bord', to: '/', icon: 'grid', end: true }

const clients: NavItem = { label: 'Clients', to: '/clients', icon: 'users' }
const clientsAvecSousMenus: NavItem = {
  ...clients,
  deroulant: true,
  children: [
    { label: 'Liste des clients', to: '/clients', end: true },
    { label: 'Responsables', to: '/responsables' },
    { label: 'Domaines', to: '/domaines' },
  ],
}
const clientsSimple: NavItem = {
  ...clients,
  deroulant: true,
  children: [{ label: 'Liste des clients', to: '/clients', end: true }],
}

const commerciaux: NavItem = {
  label: 'Commerciaux',
  to: '/commerciaux',
  icon: 'briefcase',
  deroulant: true,
  children: [{ label: 'Liste des commerciaux', to: '/commerciaux', end: true }],
}

const services: NavItem = {
  label: 'Services',
  to: '/services',
  icon: 'layers',
  deroulant: true,
  children: [{ label: 'Liste des services', to: '/services', end: true }],
}

const demandes: NavItem = {
  label: 'Demandes',
  to: '/demandes',
  icon: 'file',
  badge: 'demandes',
  deroulant: true,
  children: [{ label: 'Liste des demandes', to: '/demandes', end: true }],
}
const demandesLectureSeule: NavItem = {
  label: 'Demandes de service',
  to: '/demandes',
  icon: 'file',
  hint: 'lecture seule',
  deroulant: true,
  children: [{ label: 'Liste des demandes', to: '/demandes', end: true }],
}

const paiements: NavItem = {
  label: 'Paiements',
  to: '/paiements',
  icon: 'card',
  deroulant: true,
  children: [
    { label: 'Paiements', to: '/paiements', end: true },
    { label: 'Échéanciers', to: '/paiements/echeanciers' },
    { label: 'Historique', to: '/paiements/historique' },
  ],
}
const notifications: NavItem = {
  label: 'Notifications',
  to: '/notifications',
  icon: 'bell',
  badge: 'notifications',
}

const rapports: NavItem = { label: 'Rapports', to: '/rapports', icon: 'chart' }
const rapportsDemandes: NavItem = {
  label: 'Rapports',
  to: '/rapports',
  icon: 'chart',
  hint: 'demandes',
}

const administration: NavItem = {
  label: 'Administration',
  to: '/administration',
  icon: 'gear',
  children: [
    { label: 'Utilisateurs', to: '/administration/utilisateurs' },
    { label: 'Rôles & permissions', to: '/administration/roles' },
    { label: 'Réinitialisations', to: '/administration/reinitialisations' },
    { label: 'Audit', to: '/administration/audit' },
    { label: 'Configuration', to: '/administration/configuration' },
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