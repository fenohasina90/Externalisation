/** KPI principaux — les champs financiers sont `null` pour l'Accueil. */
export interface DashboardSummary {
  nombreClients: number | null
  nombreDemandes: number | null
  nombreServices: number | null
  montantTotal: number | null
  montantPaye: number | null
  resteAPayer: number | null
  paiementsEnRetard: number | null
  demandesEnAttente: number | null
  demandesEnCours: number | null
}

export interface StatutCount {
  statut: string
  nombre: number
}

/** Demande(s) par mois ; `mois` est au format `AAAA-MM`. */
export interface DemandeMois {
  mois: string
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

/** Service le plus demandé (sans donnée financière). */
export interface ServiceTop {
  serviceId: number
  nomService: string
  nombreDemandes: number
}

/** Revenus par service — réservé aux rôles financiers. */
export interface ServiceRevenu extends ServiceTop {
  revenu: number
}

export interface PaiementRetard {
  factureValideeId: number
  societeId: number | null
  nomSociete: string | null
  demandeId: number | null
  referenceDemande: string | null
  dateEmission: string
  montantTotal: number
  montantPaye: number
  reste: number
  statut: string
}

export interface PaiementDashboard {
  parMode: ModeMontant[]
  parMois: MoisMontant[]
}

/** Filtres appliqués au tableau de bord. Les identifiants sont des chaînes issues des `<select>`. */
export interface DashboardFiltres {
  dateDebut: string
  dateFin: string
  clientId: string
  serviceId: string
}

export interface FiltreOption {
  id: number
  libelle: string
}

export interface DashboardFiltreOptions {
  clients: FiltreOption[]
  services: FiltreOption[]
}

export const FILTRES_DASHBOARD_VIDES: DashboardFiltres = {
  dateDebut: '',
  dateFin: '',
  clientId: '',
  serviceId: '',
}

export interface PerformancesDashboard {
  demandesTerminees: number | null
  tauxDemandesTerminees: number | null
  servicePlusDemande: ServiceTop | null
  commercialTop: { commercialId: number; nomCommercial: string; nombreDemandes: number } | null
  demandesTermineesParMois: DemandeMois[]
}

export interface ClientRevenu {
  clientId: number
  nomSociete: string | null
  nombreDemandes: number | null
  montantTotal: number | null
  montantPaye: number | null
}

export type TonAlerte = 'gris' | 'orange' | 'rouge' | 'vert'

export interface AlerteDashboard {
  ton: TonAlerte
  etiquette: string
  titre: string
  detail: string
  lien: string
}

/** Données agrégées par la page, limitées au périmètre du rôle. */
export interface DashboardData {
  summary: DashboardSummary
  statuts: StatutCount[]
  demandesMois: DemandeMois[]
  servicesTop: ServiceTop[]
  paiements: PaiementDashboard | null
  paiementsEnRetard: PaiementRetard[]
  servicesRevenu: ServiceRevenu[]
  performances: PerformancesDashboard
}
