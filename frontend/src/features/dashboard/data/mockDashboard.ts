/** Données statiques du tableau de bord (étape 1 — sans backend). */

export const DASH_MOIS = ['Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sept']

/** Évolution des demandes (vues ACCUEIL et SUPER_ADMIN). */
export const EVOLUTION_DEMANDES = [14, 18, 16, 24, 21, 33]

/** Demandes par statut : 24 + 18 + 52 + 28 + 4 = 126. */
export const DEMANDES_PAR_STATUT = [
  { statut: 'En attente', nombre: 24, couleur: '#aeb9b0' },
  { statut: 'Validation', nombre: 18, couleur: '#8fd0a0' },
  { statut: 'En cours', nombre: 52, couleur: '#2D9A5C' },
  { statut: 'Terminées', nombre: 28, couleur: '#1d6b3f' },
  { statut: 'Annulées', nombre: 4, couleur: '#c0392b' },
]

/** Services les plus demandés (nombre de demandes). */
export const SERVICES_TOP = [
  { service: 'Audit comptable', nombre: 34 },
  { service: 'Conseil fiscal', nombre: 26 },
  { service: 'Paie & RH', nombre: 22 },
  { service: 'Expertise juridique', nombre: 18 },
  { service: 'Bilan annuel', nombre: 11 },
]

/** Paiements par mois en millions de MGA : attendu vs payé. */
export const PAIEMENTS_PAR_MOIS = [
  { mois: 'Avr', attendu: 8, paye: 6 },
  { mois: 'Mai', attendu: 10.5, paye: 8.5 },
  { mois: 'Juin', attendu: 9.5, paye: 7.5 },
  { mois: 'Juil', attendu: 12.5, paye: 10.5 },
  { mois: 'Août', attendu: 13, paye: 9.5 },
  { mois: 'Sept', attendu: 18, paye: 16.3 },
]

/** Échéances en retard par ancienneté. */
export const RETARDS = [
  { tranche: '1 à 15 jours', echeances: 4, montant: 3200000, ton: 'orange' as const },
  { tranche: '16 à 30 jours', echeances: 2, montant: 2100000, ton: 'orange' as const },
  { tranche: 'Plus de 30 jours', echeances: 1, montant: 1200000, ton: 'rouge' as const },
]

/** Montant facturé par service (vues ECONOMAT et SUPER_ADMIN). */
export const MONTANT_SERVICES = [
  { service: 'Audit comptable', montant: '29,4 M MGA', millions: 29.4 },
  { service: 'Conseil fiscal', montant: '21,5 M MGA', millions: 21.5 },
  { service: 'Paie & RH', montant: '14,8 M MGA', millions: 14.8 },
  { service: 'Expertise juridique', montant: '6,5 M MGA', millions: 6.5 },
  { service: 'Bilan annuel', montant: '5,2 M MGA', millions: 5.2 },
]

export interface AlerteDash {
  ton: 'gris' | 'orange' | 'rouge' | 'vert'
  etiquette: string
  titre: string
  detail: string
  lien: string
}

export const ALERTES_ACCUEIL: AlerteDash[] = [
  {
    ton: 'gris',
    etiquette: 'Demandes en attente',
    titre: '24 demandes à traiter',
    detail: 'En attente du 1er paiement',
    lien: '/demandes',
  },
]

export const ALERTES_ECONOMAT: AlerteDash[] = [
  {
    ton: 'orange',
    etiquette: 'Échéances proches',
    titre: '5 échéances sous 7 jours',
    detail: 'Relancez les clients concernés',
    lien: '/paiements/echeanciers',
  },
  {
    ton: 'rouge',
    etiquette: 'Paiements en retard',
    titre: '7 échéances dépassées',
    detail: '6 500 000 MGA concernés',
    lien: '/paiements',
  },
  {
    ton: 'vert',
    etiquette: 'Montants restant à payer',
    titre: '32 200 000 MGA',
    detail: 'sur 84 500 000 MGA au total',
    lien: '/paiements',
  },
]

export const ALERTES_SUPER_ADMIN: AlerteDash[] = [
  {
    ton: 'rouge',
    etiquette: 'Paiements en retard',
    titre: '7 échéances dépassées',
    detail: '6 500 000 MGA concernés',
    lien: '/paiements',
  },
  {
    ton: 'orange',
    etiquette: 'Échéances proches',
    titre: '5 échéances sous 7 jours',
    detail: 'Relancez les clients concernés',
    lien: '/paiements/echeanciers',
  },
  {
    ton: 'gris',
    etiquette: 'Demandes en attente',
    titre: '24 demandes à traiter',
    detail: 'En attente du 1er paiement',
    lien: '/demandes',
  },
  {
    ton: 'vert',
    etiquette: 'Montants restant à payer',
    titre: '32 200 000 MGA',
    detail: 'sur 84 500 000 MGA au total',
    lien: '/paiements',
  },
]

export function formatMGA(n: number): string {
  return `${n.toLocaleString('fr-FR')} MGA`
}
