export type StatutDemandeUI = 'EN_ATTENTE' | 'VALIDATION' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE'
export type SituationPaiement = 'Payé' | 'Partiel' | 'En attente' | 'En retard'

export interface DemandeResume {
  reference: string
  client: string
  responsable: string
  commercial: string
  service: string
  domaine: string
  dateDebut: string
  dateFin: string
  dateDemande: string
  tarif: number
  quantite: number
  montantTotal: number
  commentaire: string
  statut: StatutDemandeUI
  situationPaiement: SituationPaiement
}

export const STATUT_LABEL: Record<StatutDemandeUI, string> = {
  EN_ATTENTE: 'En attente',
  VALIDATION: 'Validation',
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée',
  ANNULEE: 'Annulée',
}

/** 8 demandes de la maquette (ordre d'affichage conservé). */
const HEROS: DemandeResume[] = [
  { reference: 'REF-2026-014', client: 'Atlas Conseil', responsable: 'Amina Neme', commercial: 'Jean Dupont', service: 'Audit comptable', domaine: 'Comptabilité', dateDebut: '01/09/2026', dateFin: '28/02/2027', dateDemande: '02/09/2026', tarif: 500000, quantite: 2, montantTotal: 1000000, commentaire: 'Audit annuel des comptes, deux missions.', statut: 'EN_COURS', situationPaiement: 'Partiel' },
  { reference: 'REF-2026-013', client: 'Innovatech SARL', responsable: 'Lina Meyer', commercial: 'Marie Martin', service: 'Conseil fiscal', domaine: 'Fiscalité', dateDebut: '15/08/2026', dateFin: '15/11/2026', dateDemande: '10/08/2026', tarif: 800000, quantite: 1, montantTotal: 800000, commentaire: '', statut: 'TERMINEE', situationPaiement: 'Payé' },
  { reference: 'REF-2026-012', client: 'Globex France', responsable: 'Karim Haddad', commercial: 'Luc Moreau', service: 'Paie & RH', domaine: 'Social', dateDebut: '01/10/2026', dateFin: '31/03/2027', dateDemande: '25/09/2026', tarif: 600000, quantite: 3, montantTotal: 1800000, commentaire: '', statut: 'EN_ATTENTE', situationPaiement: 'En attente' },
  { reference: 'REF-2026-011', client: 'Matrizel SARL', responsable: 'Nadia Bello', commercial: 'Jean Dupont', service: 'Expertise juridique', domaine: 'Droit', dateDebut: '01/08/2026', dateFin: '31/01/2027', dateDemande: '28/07/2026', tarif: 900000, quantite: 1, montantTotal: 900000, commentaire: '', statut: 'EN_COURS', situationPaiement: 'En retard' },
  { reference: 'REF-2026-010', client: 'Ammontech SAS', responsable: 'Paul Ndiaye', commercial: 'Anne Petit', service: 'Bilan annuel', domaine: 'Comptabilité', dateDebut: '01/07/2026', dateFin: '30/09/2026', dateDemande: '20/06/2026', tarif: 1200000, quantite: 1, montantTotal: 1200000, commentaire: '', statut: 'TERMINEE', situationPaiement: 'Payé' },
  { reference: 'REF-2026-009', client: 'Cobalt Consulting', responsable: 'Mélissa Neme', commercial: 'Antoine Leroy', service: 'Accompagnement RH', domaine: 'Social', dateDebut: '15/07/2026', dateFin: '15/01/2027', dateDemande: '01/07/2026', tarif: 500000, quantite: 2, montantTotal: 1000000, commentaire: '', statut: 'EN_COURS', situationPaiement: 'Partiel' },
  { reference: 'REF-2026-008', client: 'Novacore Services', responsable: 'Pierre Legrand', commercial: 'Chloé Fournier', service: 'Déclaration TVA', domaine: 'Fiscalité', dateDebut: '01/09/2026', dateFin: '31/12/2026', dateDemande: '20/08/2026', tarif: 350000, quantite: 1, montantTotal: 350000, commentaire: '', statut: 'EN_ATTENTE', situationPaiement: 'En retard' },
  { reference: 'REF-2026-007', client: 'Hélios Groupe', responsable: 'Luc Bernard', commercial: 'Marie Martin', service: 'Restructuration', domaine: 'Conseil', dateDebut: '15/06/2026', dateFin: '15/09/2026', dateDemande: '01/06/2026', tarif: 2000000, quantite: 1, montantTotal: 2000000, commentaire: '', statut: 'TERMINEE', situationPaiement: 'Payé' },
]

const CLIENTS = ['Atlas Conseil', 'Innovatech SARL', 'Globex France', 'Matrizel SARL', 'Ammontech SAS', 'Cobalt Consulting', 'Novacore Services', 'Hélios Groupe']
const SERVICES: Array<[string, string, number]> = [
  ['Audit comptable', 'Comptabilité', 1500000],
  ['Conseil fiscal', 'Fiscalité', 800000],
  ['Paie & RH', 'Social', 600000],
  ['Tenue comptable', 'Comptabilité', 450000],
  ['Bilan annuel', 'Comptabilité', 1200000],
  ['Déclaration TVA', 'Fiscalité', 350000],
  ['Externalisation DAF', 'Finance', 1800000],
  ['Diagnostic financier', 'Finance', 750000],
]
const COMMERCIAUX = ['Jean Dupont', 'Marie Martin', 'Luc Moreau', 'Anne Petit', 'Antoine Leroy', 'Chloé Fournier']
const RESPONSABLES = ['Amina Neme', 'Lina Meyer', 'Karim Haddad', 'Nadia Bello', 'Paul Ndiaye', 'Mélissa Neme']

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/**
 * Complément déterministe : 49 En cours, 22 En attente, 27 Terminée,
 * 12 Validation, 8 Annulée (totaux : 52 En cours, 24 En attente).
 * 5 situations « En retard » supplémentaires (7 au total).
 */
function generer(): DemandeResume[] {
  const statuts: StatutDemandeUI[] = []
  for (let i = 0; i < 49; i++) statuts.push('EN_COURS')
  for (let i = 0; i < 22; i++) statuts.push('EN_ATTENTE')
  for (let i = 0; i < 27; i++) statuts.push('TERMINEE')
  for (let i = 0; i < 12; i++) statuts.push('VALIDATION')
  for (let i = 0; i < 8; i++) statuts.push('ANNULEE')

  const numeros: number[] = []
  for (let n = 126; n >= 15; n--) numeros.push(n)
  for (let n = 6; n >= 1; n--) numeros.push(n)

  return numeros.map((num, i) => {
    const statut = statuts[i]
    const [service, domaine, tarifBase] = SERVICES[i % SERVICES.length]
    const quantite = (i % 3) + 1
    const jour = pad((i % 27) + 1)
    const mois = pad((i % 9) + 1)
    // 5 retards générés (7 au total avec REF-2026-011 et REF-2026-008).
    const enRetard = [4, 27, 50, 55, 100].includes(i)
    const situation: SituationPaiement =
      statut === 'TERMINEE' ? 'Payé'
      : statut === 'EN_ATTENTE' ? (enRetard ? 'En retard' : 'En attente')
      : enRetard ? 'En retard'
      : i % 2 === 0 ? 'Partiel' : 'Payé'
    return {
      reference: `REF-2026-${String(num).padStart(3, '0')}`,
      client: CLIENTS[i % CLIENTS.length],
      responsable: RESPONSABLES[i % RESPONSABLES.length],
      commercial: COMMERCIAUX[i % COMMERCIAUX.length],
      service,
      domaine,
      dateDebut: `${jour}/${mois}/2026`,
      dateFin: `${jour}/${pad(((i % 9) + 3) % 12 + 1)}/2027`,
      dateDemande: `${jour}/${mois}/2026`,
      tarif: tarifBase,
      quantite,
      montantTotal: tarifBase * quantite,
      commentaire: '',
      statut,
      situationPaiement: situation,
    }
  })
}

/** 126 demandes : les 8 de la maquette puis le complément. */
export const MOCK_DEMANDES: DemandeResume[] = [...HEROS, ...generer()]

export const DEMANDE_OPTIONS = {
  clients: CLIENTS,
  services: [...new Set(SERVICES.map(([s]) => s))],
  domaines: ['Comptabilité', 'Fiscalité', 'Finance', 'Social', 'Droit', 'Conseil'],
  commerciaux: COMMERCIAUX,
}

/** Transitions manuelles autorisées depuis chaque statut. */
export const TRANSITIONS_MANUELLES: Record<StatutDemandeUI, StatutDemandeUI[]> = {
  EN_ATTENTE: ['ANNULEE'],
  VALIDATION: ['ANNULEE'],
  EN_COURS: ['TERMINEE', 'ANNULEE'],
  TERMINEE: [],
  ANNULEE: [],
}

export interface DemandeFormValues {
  client: string
  responsable: string
  commercial: string
  domaine: string
  service: string
  dateDemande: string
  debutService: string
  finService: string
  montantTotal: number
  commentaire: string
}

export const DEMANDE_FORM_VIDE: DemandeFormValues = {
  client: 'Atlas Conseil',
  responsable: 'Amina Neme',
  commercial: 'Jean Dupont',
  domaine: 'Comptabilité',
  service: 'Audit comptable',
  dateDemande: '2026-09-02',
  debutService: '2026-09-01',
  finService: '2027-02-28',
  montantTotal: 1000000,
  commentaire: '',
}

/** Convertit aaaa-mm-jj → jj/mm/aaaa pour l'enregistrement statique. */
export function versDateFR(iso: string): string {
  const [a, m, j] = iso.split('-')
  return `${j}/${m}/${a}`
}
export const COMMERCIAUX_OPTIONS = [
  { nom: 'Jean Dupont', matricule: 'C001' },
  { nom: 'Marie Martin', matricule: 'C002' },
  { nom: 'Luc Moreau', matricule: 'C005' },
  { nom: 'Anne Petit', matricule: 'C006' },
  { nom: 'Antoine Leroy', matricule: 'C007' },
  { nom: 'Chloé Fournier', matricule: 'C008' },
]

/** Responsables proposés selon le client sélectionné. */
export const RESPONSABLES_PAR_CLIENT: Record<string, string[]> = {
  'Atlas Conseil': ['Amina Neme', 'Karim Haddad'],
  'Innovatech SARL': ['Lina Meyer'],
  'Globex France': ['Karim Haddad'],
  'Matrizel SARL': ['Nadia Bello'],
  'Ammontech SAS': ['Paul Ndiaye'],
  'Cobalt Consulting': ['Mélissa Neme'],
  'Novacore Services': ['Pierre Legrand'],
  'Hélios Groupe': ['Luc Bernard'],
}

/** Tarif courant par service (récupéré automatiquement dans le formulaire). */
export const TARIF_PAR_SERVICE: Record<string, number> = Object.fromEntries(
  SERVICES.map(([nom, , tarif]) => [nom, tarif]),
)
