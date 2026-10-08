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
}

export interface NavSection {
  label: string
  items: NavItem[]
}

const dashboard: NavItem = { label: 'Tableau de bord', to: '/', icon: 'grid', end: true }

const clients: NavItem = { label: 'Clients', to: '/clients', icon: 'users' }
const clientsAvecResponsables: NavItem = {
  ...clients,
  children: [{ label: 'Responsables', to: '/responsables' }],
}

const commerciaux: NavItem = { label: 'Commerciaux', to: '/commerciaux', icon: 'briefcase' }

const servicesAvecEnfants: NavItem = {
  label: 'Services',
  to: '/services',
  icon: 'layers',
  children: [
    { label: 'Domaines', to: '/services/domaines' },
    { label: 'Liste des services', to: '/services', end: true },
  ],
}

const servicesSimple: NavItem = { label: 'Services', to: '/services', icon: 'layers' }

const demandes: NavItem = {
  label: 'Demandes de service',
  to: '/demandes',
  icon: 'file',
  badge: 'demandes',
}
const demandesLectureSeule: NavItem = {
  label: 'Demandes de service',
  to: '/demandes',
  icon: 'file',
  hint: 'lecture seule',
}

const paiements: NavItem = {
  label: 'Paiements',
  to: '/paiements',
  icon: 'card',
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
        clientsAvecResponsables,
        commerciaux,
        servicesAvecEnfants,
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
        clientsAvecResponsables,
        commerciaux,
        servicesAvecEnfants,
        demandes,
        paiements,
      ],
    },
    { label: 'Pilotage', items: [notifications, rapports, administration] },
  ],
  ACCUEIL: [
    {
      label: 'Activité',
      items: [dashboard, clientsAvecResponsables, commerciaux, servicesSimple, demandes],
    },
    { label: 'Pilotage', items: [rapportsDemandes, notifications] },
  ],
  ECONOMAT: [
    {
      label: 'Activité',
      items: [dashboard, clients, demandesLectureSeule, paiements],
    },
    { label: 'Pilotage', items: [rapports, notifications] },
  ],
}