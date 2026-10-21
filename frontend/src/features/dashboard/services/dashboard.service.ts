import { http } from '@/api/http'
import type {
  ClientRevenu,
  DashboardFiltres,
  DashboardFiltreOptions,
  DashboardSummary,
  DemandeMois,
  EcheanceDashboard,
  PaiementDashboard,
  PerformancesDashboard,
  ServiceRevenu,
  ServiceTop,
  StatutCount,
} from '../types'

/** Ne transmet que les filtres réellement renseignés. */
function parametres(filtres?: DashboardFiltres): Record<string, string | number> {
  if (!filtres) return {}
  const params: Record<string, string | number> = {}
  if (filtres.dateDebut) params.dateDebut = filtres.dateDebut
  if (filtres.dateFin) params.dateFin = filtres.dateFin
  if (filtres.clientId) params.clientId = Number(filtres.clientId)
  if (filtres.serviceId) params.serviceId = Number(filtres.serviceId)
  return params
}

export const dashboardApi = {
  async getSummary(filtres?: DashboardFiltres): Promise<DashboardSummary> {
    const { data } = await http.get<DashboardSummary>('/dashboard/summary', {
      params: parametres(filtres),
    })
    return data
  },

  async getPerformances(filtres?: DashboardFiltres): Promise<PerformancesDashboard> {
    const { data } = await http.get<PerformancesDashboard>('/dashboard/performances', {
      params: parametres(filtres),
    })
    return data
  },

  async getDemandesParStatut(filtres?: DashboardFiltres): Promise<StatutCount[]> {
    const { data } = await http.get<StatutCount[]>('/dashboard/demandes', {
      params: parametres(filtres),
    })
    return data
  },

  async getDemandesParMois(filtres?: DashboardFiltres): Promise<DemandeMois[]> {
    const { data } = await http.get<DemandeMois[]>('/dashboard/demandes/mois', {
      params: parametres(filtres),
    })
    return data
  },

  async getDemandesParService(filtres?: DashboardFiltres): Promise<ServiceTop[]> {
    const { data } = await http.get<ServiceTop[]>('/dashboard/demandes/services', {
      params: parametres(filtres),
    })
    return data
  },

  async getEcheances(filtres?: DashboardFiltres): Promise<EcheanceDashboard> {
    const { data } = await http.get<EcheanceDashboard>('/dashboard/echeances', {
      params: parametres(filtres),
    })
    return data
  },

  async getPaiements(filtres?: DashboardFiltres): Promise<PaiementDashboard> {
    const { data } = await http.get<PaiementDashboard>('/dashboard/paiements', {
      params: parametres(filtres),
    })
    return data
  },

  async getServices(filtres?: DashboardFiltres): Promise<ServiceRevenu[]> {
    const { data } = await http.get<ServiceRevenu[]>('/dashboard/services', {
      params: parametres(filtres),
    })
    return data
  },

  async getClients(filtres?: DashboardFiltres): Promise<ClientRevenu[]> {
    const { data } = await http.get<ClientRevenu[]>('/dashboard/clients', {
      params: parametres(filtres),
    })
    return data
  },

  async getFiltres(): Promise<DashboardFiltreOptions> {
    const { data } = await http.get<DashboardFiltreOptions>('/dashboard/filtres')
    return data
  },
}
