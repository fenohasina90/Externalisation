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

const services: NavItem = { label: 'Services & domaines', to: '/services', icon: 'layers' }

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

const plansPaiement: NavItem = { label: 'Plans de paiement', to: '/plans-paiement', icon: 'calendar' }
const echeances: NavItem = { label: 'Échéances', to: '/echeances', icon: 'clock' }
const paiements: NavItem = { label: 'Paiements', to: '/paiements', icon: 'card' }
const documents: NavItem = { label: 'Documents', to: '/documents', icon: 'folder' }

const notifications: NavItem = {
  label: 'Notifications',
  to: '/notifications',
  icon: 'bell',
  badge: 'notifications',
}

const rapports: NavItem = { label: 'Rapports', to: '/rapports', icon: 'chart' }
const recherche: NavItem = { label: 'Recherche', to: '/recherche', icon: 'search' }

const administration: NavItem = {
  label: 'Administration',
  to: '/administration',
  icon: 'gear',
  children: [
    { label: 'Utilisateurs', to: '/administration/utilisateurs' },
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
        services,
        demandes,
        plansPaiement,
        echeances,
        paiements,
        documents,
      ],
    },
    { label: 'Pilotage', items: [notifications, rapports, recherche, administration] },
  ],
  ADMIN: [
    {
      label: 'Activité',
      items: [
        dashboard,
        clientsAvecResponsables,
        commerciaux,
        services,
        demandes,
        plansPaiement,
        echeances,
        paiements,
        documents,
      ],
    },
    { label: 'Pilotage', items: [notifications, rapports, recherche, administration] },
  ],
  ACCUEIL: [
    {
      label: 'Activité',
      items: [dashboard, clientsAvecResponsables, commerciaux, services, demandes, documents],
    },
    { label: 'Pilotage', items: [notifications, rapports, recherche] },
  ],
  ECONOMAT: [
    {
      label: 'Activité',
      items: [dashboard, clients, demandesLectureSeule, plansPaiement, echeances, paiements],
    },
    { label: 'Pilotage', items: [notifications, rapports, recherche] },
  ],
}