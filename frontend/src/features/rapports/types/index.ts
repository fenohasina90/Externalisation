import type { Demande } from '@/features/demandes/types'

export type CategorieRapport =
  | 'general'
  | 'financier'
  | 'clients'
  | 'services'
  | 'demandes'
  | 'paiements'

export type FormatExport = 'pdf' | 'xlsx' | 'csv'

export interface RapportFiltres {
  dateDebut?: string
  dateFin?: string
  clientId?: number
  serviceId?: number
  domaineId?: number
  commercialId?: number
  statut?: string
}

export interface RapportGeneral {
  nombreClients: number
  nombreDemandes: number
  nombreServices: number
  montantTotalPrevu: number
  montantEncaisse: number
  resteAPayer: number
  montantEnRetard: number
  echeancesEnRetard: number
}

export interface RapportFinancierMode {
  mode: string
  montant: number
}

export interface RapportFinancier {
  montantTotalPrevu: number
  montantEncaisse: number
  resteAPayer: number
  montantEnRetard: number
  paiementsParMode: RapportFinancierMode[]
}

export interface RapportClientLigne {
  clientId: number | null
  nomSociete: string
  nombreDemandes: number
  montantTotal: number
  montantPaye: number
  reste: number
}

export interface RapportServiceLigne {
  serviceId: number
  nomService: string
  nomDomaine: string | null
  nombreDemandes: number
  revenu: number
}

export interface RapportPaiementLigne {
  id: number
  referencePaiement: string
  referenceDemande: string | null
  nomSociete: string | null
  modePaiement: string | null
  datePaiement: string
  montant: number
}

export type RapportDemande = Demande
