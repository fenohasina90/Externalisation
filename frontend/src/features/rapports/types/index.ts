export interface RapportGeneral {
  totalClients: number
  totalDemandes: number
  totalPaiements: number
  montantTotal: number
  montantPaye: number
  resteAPayer: number
}

export interface RapportFinancier {
  totalEncaisse: number
  totalAttendu: number
  impayes: number
  tauxRecouvrement: number | null
}

export interface PeriodeFiltre {
  dateDebut?: string
  dateFin?: string
}