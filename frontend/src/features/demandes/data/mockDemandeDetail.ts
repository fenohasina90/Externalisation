import { MOCK_DEMANDES, type DemandeResume } from './mockDemandes'

export interface EcheanceDetail {
  numero: number
  dateEcheance: string
  montantAttendu: number
  montantPaye: number
}

export interface HistoriqueDetail {
  dateHeure: string
  type: 'Création' | 'Modification' | 'Statut'
  texte: string
  utilisateur: string
  commentaire: string
}

export interface DocumentDetail {
  nom: string
  type: string
  taille: string
  dateAjout: string
  utilisateur: string
}

export interface DemandeDetailData {
  demande: DemandeResume
  montantPaye: number
  resteAPayer: number
  statutPaiement: 'Payé' | 'Partiel'
  echeances: EcheanceDetail[]
  historique: HistoriqueDetail[]
  documents: DocumentDetail[]
}

/** Détail complet statique de REF-2026-014 (cf. maquettes). */
function detail014(d: DemandeResume): DemandeDetailData {
  return {
    demande: d,
    montantPaye: 700000,
    resteAPayer: 300000,
    statutPaiement: 'Partiel',
    echeances: [
      { numero: 1, dateEcheance: '10/09/2026', montantAttendu: 300000, montantPaye: 300000 },
      { numero: 2, dateEcheance: '10/10/2026', montantAttendu: 300000, montantPaye: 200000 },
      { numero: 3, dateEcheance: '10/11/2026', montantAttendu: 400000, montantPaye: 200000 },
    ],
    historique: [
      { dateHeure: '25/09/2026 16:05', type: 'Modification', texte: 'Date de fin : 31/01/2027 → 28/02/2027', utilisateur: 'Amine Martin', commentaire: 'À la demande du client.' },
      { dateHeure: '08/09/2026 14:20', type: 'Statut', texte: 'Validation → En cours (automatique)', utilisateur: 'Système', commentaire: '—' },
      { dateHeure: '08/09/2026 14:20', type: 'Statut', texte: 'En attente → Validation (automatique)', utilisateur: 'Système', commentaire: 'Paiement de la 1ʳᵉ échéance enregistré.' },
      { dateHeure: '02/09/2026 11:02', type: 'Modification', texte: 'Tarif saisi : 500 000 MGA', utilisateur: 'Sarah Cohen', commentaire: '—' },
      { dateHeure: '02/09/2026 10:15', type: 'Création', texte: 'Création de la demande', utilisateur: 'Sarah Cohen', commentaire: '—' },
    ],
    documents: [
      { nom: 'Bon de commande.pdf', type: 'PDF', taille: '245 Ko', dateAjout: '02/09/2026', utilisateur: 'Sarah Cohen' },
      { nom: 'Contrat de service.pdf', type: 'PDF', taille: '1,2 Mo', dateAjout: '02/09/2026', utilisateur: 'Sarah Cohen' },
      { nom: 'Cahier des charges.docx', type: 'DOCX', taille: '96 Ko', dateAjout: '02/09/2026', utilisateur: 'Sarah Cohen' },
      { nom: 'Justificatif virement 1.jpg', type: 'JPG', taille: '380 Ko', dateAjout: '08/09/2026', utilisateur: 'Amine Martin' },
    ],
  }
}

/** Détail générique pour les autres demandes (une échéance, historique minimal). */
function detailGenerique(d: DemandeResume): DemandeDetailData {
  const paye = d.situationPaiement === 'Payé'
  return {
    demande: d,
    montantPaye: paye ? d.montantTotal : 0,
    resteAPayer: paye ? 0 : d.montantTotal,
    statutPaiement: paye ? 'Payé' : 'Partiel',
    echeances: [
      { numero: 1, dateEcheance: d.dateFin, montantAttendu: d.montantTotal, montantPaye: paye ? d.montantTotal : 0 },
    ],
    historique: [
      { dateHeure: `${d.dateDemande} 10:15`, type: 'Création', texte: 'Création de la demande', utilisateur: d.commercial, commentaire: '—' },
    ],
    documents: [],
  }
}

export function getDemandeDetail(reference: string): DemandeDetailData | null {
  const demande = MOCK_DEMANDES.find((d) => d.reference === reference)
  if (!demande) return null
  return reference === 'REF-2026-014' ? detail014(demande) : detailGenerique(demande)
}
