export type StatutClient = 'Actif' | 'Inactif' | 'Archivé'

export interface ClientResume {
  code: string
  nom: string
  activite: string
  email: string
  telephone: string
  responsable: string
  nbDemandes: number
  statut: StatutClient
}

export interface ResponsableClient {
  nom: string
  principal: boolean
  fonction: string
  telephone: string
  email: string
  actif: boolean
}

/** 8 clients de la maquette (ordre d'affichage conservé). */
const HEROS: ClientResume[] = [
  { code: 'CLI-0248', nom: 'Atlas Conseil', activite: 'Services financiers', email: 'contact@atlas-conseil.mg', telephone: '+261 34 12 345 67', responsable: 'Amina Neme', nbDemandes: 15, statut: 'Actif' },
  { code: 'CLI-0247', nom: 'Innovatech SARL', activite: 'Solutions numériques', email: 'contact@innovatech.mg', telephone: '+261 32 23 456 78', responsable: 'Marge Neme', nbDemandes: 23, statut: 'Actif' },
  { code: 'CLI-0246', nom: 'Globex France', activite: 'Technologie', email: 'hello@globex.mg', telephone: '+261 33 34 567 89', responsable: 'Chloé Neme', nbDemandes: 2, statut: 'Inactif' },
  { code: 'CLI-0245', nom: 'Matrizel SARL', activite: 'Vente en gros', email: 'gestion@matrizel.mg', telephone: '+261 34 45 678 90', responsable: 'Manon Demes', nbDemandes: 26, statut: 'Actif' },
  { code: 'CLI-0244', nom: 'Ammontech SAS', activite: 'Vente au détail', email: 'admin@ammontech.mg', telephone: '+261 32 56 789 01', responsable: 'Arnaud Martinez', nbDemandes: 1, statut: 'Actif' },
  { code: 'CLI-0243', nom: 'Cobalt Consulting', activite: 'Conseil RH', email: 'contact@cobalt.mg', telephone: '+261 33 67 890 12', responsable: 'Mélissa Neme', nbDemandes: 15, statut: 'Actif' },
  { code: 'CLI-0242', nom: 'Novacore Services', activite: 'Services externalisés', email: 'bureau@novacore.mg', telephone: '+261 34 78 901 23', responsable: 'Pierre Legrand', nbDemandes: 8, statut: 'Actif' },
  { code: 'CLI-0241', nom: 'Hélios Groupe', activite: 'Ingénierie', email: 'contact@helios.mg', telephone: '+261 32 89 012 34', responsable: 'Luc Bernard', nbDemandes: 4, statut: 'Archivé' },
]

const PREFIXES = ['Sahan', 'Tien', 'Vola', 'Niri', 'Tant', 'Fano', 'Mamy', 'Lova', 'Noro', 'Hasina', 'Voahangy', 'Tahina', 'Nirina', 'Hery', 'Ketaka', 'Mialy', 'Rado', 'Sitraka', 'Fanilo', 'Anja']
const SUFFIXES = ['Conseil', 'Services', 'SARL', 'Consulting', 'Groupe', 'Solutions', 'Industries', 'Négoce', 'BTP', 'Transit', 'Immo', 'Agro']
const ACTIVITES = ['Services financiers', 'Solutions numériques', 'Technologie', 'Vente en gros', 'Vente au détail', 'Conseil RH', 'Ingénierie', 'BTP', 'Transit', 'Agroalimentaire']

function slug(nom: string): string {
  return nom.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, '')
}

/**
 * 240 clients générés : 208 actifs, 21 inactifs, 11 archivés
 * (totaux : 214 actifs, 22 inactifs, 12 archivés).
 */
function generer(): ClientResume[] {
  const lignes: ClientResume[] = []
  for (let i = 0; i < 240; i++) {
    const num = 240 - i
    const nom = `${PREFIXES[i % PREFIXES.length]} ${SUFFIXES[(i * 7) % SUFFIXES.length]}`
    const prenomResp = PREFIXES[(i * 3 + 1) % PREFIXES.length]
    const nomResp = PREFIXES[(i * 5 + 2) % PREFIXES.length]
    const statut: StatutClient = i < 208 ? 'Actif' : i < 229 ? 'Inactif' : 'Archivé'
    lignes.push({
      code: `CLI-${String(num).padStart(4, '0')}`,
      nom,
      activite: ACTIVITES[i % ACTIVITES.length],
      email: `contact@${slug(nom)}.mg`,
      telephone: `+261 3${(i % 3) + 2} ${(10 + (i % 89)).toString()} ${(100 + ((i * 37) % 899)).toString()} ${(10 + ((i * 13) % 89)).toString()}`,
      responsable: `${prenomResp} ${nomResp}`,
      nbDemandes: (i * 7) % 30,
      statut,
    })
  }
  return lignes
}

/** 248 clients : 8 héros puis le complément (codes uniques). */
export const MOCK_CLIENTS: ClientResume[] = [...HEROS, ...generer()]

export const CLIENT_OPTIONS = {
  activites: [...new Set(MOCK_CLIENTS.map((c) => c.activite))],
  responsables: [...new Set(HEROS.map((c) => c.responsable))],
}

export type TriClients = 'nom-az' | 'nom-za' | 'demandes-asc' | 'demandes-desc'

/** Initiales « AN » pour Amina Neme (cf. maquette). */
export function initialesClient(nomComplet: string): string {
  const parts = nomComplet.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '—'
  if (parts.length === 1) return (parts[0][0] ?? '—').toUpperCase()
  return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase()
}

/* ---------------- Détail Atlas Conseil (maquette) ---------------- */

export interface ClientDetailData {
  client: ClientResume
  montantTotal: number
  montantPaye: number
}

export const ATLAS_DETAIL: ClientDetailData = {
  client: HEROS[0],
  montantTotal: 18400000,
  montantPaye: 12600000,
}

export const ATLAS_RESPONSABLES: ResponsableClient[] = [
  { nom: 'Amina Neme', principal: true, fonction: 'Directrice financière', telephone: '+261 34 12 345 67', email: 'amina.neme@atlas-conseil.mg', actif: true },
  { nom: 'Rivo Randria', principal: false, fonction: 'Responsable comptable', telephone: '+261 32 98 765 43', email: 'r.randria@atlas-conseil.mg', actif: true },
]

export interface DemandeClient {
  reference: string
  service: string
  dateDebut: string
  dateFin: string
  statut: 'En cours' | 'En attente' | 'Terminée' | 'Annulée'
  paiement: 'Payé' | 'Partiel' | 'En attente'
}

export const ATLAS_DEMANDES: DemandeClient[] = [
  { reference: 'REF-2026-014', service: 'Audit comptable', dateDebut: '01/09/2026', dateFin: '28/02/2027', statut: 'En cours', paiement: 'Partiel' },
  { reference: 'REF-2026-019', service: 'Déclaration TVA', dateDebut: '01/10/2026', dateFin: '31/12/2026', statut: 'En attente', paiement: 'En attente' },
  { reference: 'REF-2026-006', service: 'Conseil fiscal', dateDebut: '01/03/2026', dateFin: '30/06/2026', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2025-041', service: 'Bilan annuel', dateDebut: '01/09/2025', dateFin: '31/12/2025', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2025-028', service: 'Paie & RH', dateDebut: '01/04/2025', dateFin: '30/09/2025', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2025-019', service: 'Tenue comptable', dateDebut: '01/02/2025', dateFin: '31/07/2025', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2024-112', service: 'Audit comptable', dateDebut: '01/10/2024', dateFin: '31/03/2025', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2024-098', service: 'Conseil fiscal', dateDebut: '01/07/2024', dateFin: '31/12/2024', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2024-076', service: 'Bilan annuel', dateDebut: '01/04/2024', dateFin: '30/09/2024', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2024-051', service: 'Paie & RH', dateDebut: '01/01/2024', dateFin: '30/06/2024', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2023-143', service: 'Audit comptable', dateDebut: '01/09/2023', dateFin: '28/02/2024', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2023-121', service: 'Tenue comptable', dateDebut: '01/06/2023', dateFin: '30/11/2023', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2023-089', service: 'Conseil fiscal', dateDebut: '01/03/2023', dateFin: '31/08/2023', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2023-054', service: 'Bilan annuel', dateDebut: '01/01/2023', dateFin: '30/06/2023', statut: 'Terminée', paiement: 'Payé' },
  { reference: 'REF-2022-201', service: 'Audit comptable', dateDebut: '01/09/2022', dateFin: '28/02/2023', statut: 'Terminée', paiement: 'Payé' },
]

export interface DocumentClient {
  nom: string
  type: string
  demande: string | null
  taille: string
  dateAjout: string
  utilisateur: string
}

export const ATLAS_DOCUMENTS: DocumentClient[] = [
  { nom: 'Bon de commande.pdf', type: 'PDF', demande: 'REF-2026-014', taille: '245 Ko', dateAjout: '02/09/2026', utilisateur: 'Sarah Cohen' },
  { nom: 'Contrat de service.pdf', type: 'PDF', demande: 'REF-2026-014', taille: '1,2 Mo', dateAjout: '02/09/2026', utilisateur: 'Sarah Cohen' },
  { nom: 'Justificatif virement 1.jpg', type: 'JPG', demande: 'REF-2026-014', taille: '380 Ko', dateAjout: '08/09/2026', utilisateur: 'Amine Martin' },
  { nom: 'Statuts Atlas Conseil.pdf', type: 'PDF', demande: null, taille: '820 Ko', dateAjout: '14/01/2026', utilisateur: 'Sarah Cohen' },
]

export interface EvenementClient {
  dateHeure: string
  categorie: 'Paiement' | 'Demande' | 'Client'
  texte: string
  utilisateur: string
  commentaire: string
}

export const ATLAS_HISTORIQUE: EvenementClient[] = [
  { dateHeure: '03/10/2026 15:10', categorie: 'Paiement', texte: 'Paiement de 200 000 MGA enregistré (REF-2026-014)', utilisateur: 'Amine Martin', commentaire: '—' },
  { dateHeure: '25/09/2026 16:05', categorie: 'Demande', texte: 'Date de fin modifiée (REF-2026-014)', utilisateur: 'Amine Martin', commentaire: 'À la demande du client.' },
  { dateHeure: '02/09/2026 10:15', categorie: 'Demande', texte: 'Création de la demande REF-2026-014', utilisateur: 'Sarah Cohen', commentaire: '—' },
  { dateHeure: '12/06/2026 09:30', categorie: 'Client', texte: 'Responsable principal : Rivo Randria → Amina Neme', utilisateur: 'Amine Martin', commentaire: '—' },
  { dateHeure: '14/01/2026 11:00', categorie: 'Client', texte: 'Création du client', utilisateur: 'Sarah Cohen', commentaire: '—' },
]
