import { http } from '@/api/http'
import type {
  DashboardSummary,
  EcheanceDashboard,
  PaiementDashboard,
  PerformancesDashboard,
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

  async getEcheances(): Promise<EcheanceDashboard> {
    const { data } = await http.get<EcheanceDashboard>('/dashboard/echeances')
    return data
  },

  async getPaiements(): Promise<PaiementDashboard> {
    const { data } = await http.get<PaiementDashboard>('/dashboard/paiements')
    return data
  },
}