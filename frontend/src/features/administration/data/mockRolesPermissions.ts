export interface PermissionLigne {
  module: string
  voir: boolean
  creer: boolean
  modifier: boolean
  desactiver: boolean
}

export interface RoleCarte {
  nom: 'SUPER_ADMIN' | 'ACCUEIL' | 'ECONOMAT'
  utilisateurs: number
  description: string
  modifiable: boolean
}

export const ROLES_CARTES: RoleCarte[] = [
  {
    nom: 'SUPER_ADMIN',
    utilisateurs: 1,
    description: 'Accès complet. Non modifiable.',
    modifiable: false,
  },
  {
    nom: 'ACCUEIL',
    utilisateurs: 4,
    description: 'Clients, responsables, commerciaux, services et demandes.',
    modifiable: true,
  },
  {
    nom: 'ECONOMAT',
    utilisateurs: 4,
    description: 'Clients, demandes, paiements, échéanciers et rapports.',
    modifiable: true,
  },
]

const MODULES = [
  'Tableau de bord',
  'Clients',
  'Responsables',
  'Commerciaux',
  'Services',
  'Demandes',
  'Paiements',
  'Échéanciers',
  'Rapports (demandes)',
  'Notifications',
  'Administration',
] as const

function ligne(
  module: (typeof MODULES)[number],
  voir: boolean,
  creer: boolean,
  modifier: boolean,
  desactiver: boolean,
): PermissionLigne {
  return { module, voir, creer, modifier, desactiver }
}

/** Matrices statiques par rôle (cf. maquette pour ACCUEIL). */
export const PERMISSIONS_PAR_ROLE: Record<string, PermissionLigne[]> = {
  SUPER_ADMIN: MODULES.map((m) => ligne(m, true, true, true, true)),
  ACCUEIL: [
    ligne('Tableau de bord', true, false, false, false),
    ligne('Clients', true, true, true, true),
    ligne('Responsables', true, true, true, true),
    ligne('Commerciaux', true, true, true, true),
    ligne('Services', true, true, true, true),
    ligne('Demandes', true, true, true, false),
    ligne('Paiements', false, false, false, false),
    ligne('Échéanciers', false, false, false, false),
    ligne('Rapports (demandes)', true, false, false, false),
    ligne('Notifications', true, false, false, false),
    ligne('Administration', false, false, false, false),
  ],
  ECONOMAT: [
    ligne('Tableau de bord', true, false, false, false),
    ligne('Clients', true, false, false, false),
    ligne('Responsables', false, false, false, false),
    ligne('Commerciaux', false, false, false, false),
    ligne('Services', true, false, false, false),
    ligne('Demandes', true, false, false, false),
    ligne('Paiements', true, true, true, true),
    ligne('Échéanciers', true, true, true, false),
    ligne('Rapports (demandes)', true, false, false, false),
    ligne('Notifications', true, false, false, false),
    ligne('Administration', false, false, false, false),
  ],
}
