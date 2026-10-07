export interface DashboardSummary {
  nombreClients: number | null
  nombreDemandes: number | null
  nombreServices: number | null
  montantTotal: number | null
  montantPaye: number | null
  resteAPayer: number | null
  echeancesEnRetard: number | null
  echeancesProches: number | null
  demandesEnAttente: number | null
  demandesEnCours: number | null
}

export interface StatutCount {
  statut: string
  nombre: number
}

export interface MoisMontant {
  mois: string
  montant: number
}

export interface ModeMontant {
  mode: string
  montant: number
}

export interface EcheanceAlerte {
  id: number
  referenceDemande: string
  numeroEcheance: number
  dateEcheance: string
  reste: number
  statut: string
  nomSociete: string | null
}

export interface EcheanceDashboard {
  enRetard: EcheanceAlerte[]
  aVenir: EcheanceAlerte[]
}

export interface PaiementDashboard {
  parMode: ModeMontant[]
  parMois: MoisMontant[]
}

export interface PerformancesDashboard {
  delaiMoyenTraitementJours: number | null
  demandesTerminees: number | null
  tauxDemandesTerminees: number | null
  montantMoyenParDemande: number | null
  tauxPaiementATemps: number | null
}