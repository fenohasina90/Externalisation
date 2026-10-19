import { http } from '@/api/http'
import type {
  ClientRevenu,
  DashboardSummary,
  DemandeMois,
  EcheanceDashboard,
  PaiementDashboard,
  PerformancesDashboard,
  ServiceRevenu,
  ServiceTop,
  StatutCount,
} from '../types'

export const dashboardApi = {
  async getSummary(): Promise<DashboardSummary> {
    const { data } = await http.get<DashboardSummary>('/dashboard/summary')
    return data
  },

  async getPerformances(): Promise<PerformancesDashboard> {
    const { data } = await http.get<PerformancesDashboard>('/dashboard/performances')
    return data
  },

  async getDemandesParStatut(): Promise<StatutCount[]> {
    const { data } = await http.get<StatutCount[]>('/dashboard/demandes')
    return data
  },

  async getDemandesParMois(): Promise<DemandeMois[]> {
    const { data } = await http.get<DemandeMois[]>('/dashboard/demandes/mois')
    return data
  },

  async getDemandesParService(): Promise<ServiceTop[]> {
    const { data } = await http.get<ServiceTop[]>('/dashboard/demandes/services')
    return data
  },

  async getEcheances(): Promise<EcheanceDashboard> {
    const { data } = await http.get<EcheanceDashboard>('/dashboard/echeances')
    return data
  },

  async getPaiements(): Promise<PaiementDashboard> {
    const { data } = await http.get<PaiementDashboard>('/dashboard/paiements')
    return data
  },

  async getServices(): Promise<ServiceRevenu[]> {
    const { data } = await http.get<ServiceRevenu[]>('/dashboard/services')
    return data
  },

  async getClients(): Promise<ClientRevenu[]> {
    const { data } = await http.get<ClientRevenu[]>('/dashboard/clients')
    return data
  },
}
