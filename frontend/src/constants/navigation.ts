import type { NomRole } from '@/types'

export interface NavItem {
  to: string
  label: string
  roles?: NomRole[]
}

const TOUS = ['SUPER_ADMIN', 'ACCUEIL', 'ECONOMAT'] as NomRole[]

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Tableau de bord', roles: TOUS },
  { to: '/clients', label: 'Clients', roles: TOUS },
  { to: '/responsables', label: 'Responsables', roles: ['SUPER_ADMIN', 'ACCUEIL'] },
  { to: '/commerciaux', label: 'Commerciaux', roles: ['SUPER_ADMIN', 'ACCUEIL'] },
  { to: '/services', label: 'Services & domaines', roles: ['SUPER_ADMIN', 'ACCUEIL'] },
  { to: '/demandes', label: 'Demandes de service', roles: TOUS },
  { to: '/plans-paiement', label: 'Plans de paiement', roles: ['SUPER_ADMIN', 'ECONOMAT'] },
  { to: '/echeances', label: 'Échéances', roles: ['SUPER_ADMIN', 'ECONOMAT'] },
  { to: '/paiements', label: 'Paiements', roles: ['SUPER_ADMIN', 'ECONOMAT'] },
  { to: '/documents', label: 'Documents', roles: ['SUPER_ADMIN', 'ACCUEIL'] },
  { to: '/notifications', label: 'Notifications', roles: TOUS },
  { to: '/rapports', label: 'Rapports', roles: TOUS },
  { to: '/recherche', label: 'Recherche', roles: TOUS },
  {
    to: '/administration',
    label: 'Administration',
    roles: ['SUPER_ADMIN'],
  },
]