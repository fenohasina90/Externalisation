import { useApi } from '@/hooks/useApi'
import { dashboardApi } from '../services/dashboard.service'
import type { DashboardData, DashboardFiltres } from '../types'

export type VueDashboard = 'ACCUEIL' | 'ECONOMAT' | 'SUPER_ADMIN'

/**
 * Charge uniquement les blocs autorisés pour le rôle : l'Accueil n'appelle pas
 * les endpoints financiers et l'Économat n'appelle pas les endpoints demandes.
 * Les filtres période / client / service sont appliqués à tous les blocs.
 */
export function useDashboardData(vue: VueDashboard, filtres: DashboardFiltres) {
  const { dateDebut, dateFin, clientId, serviceId } = filtres

  return useApi<DashboardData>(async () => {
    const filtresApi = { dateDebut, dateFin, clientId, serviceId }

    if (vue === 'ACCUEIL') {
      const [summary, performances, statuts, demandesMois, servicesTop] = await Promise.all([
        dashboardApi.getSummary(filtresApi),
        dashboardApi.getPerformances(filtresApi),
        dashboardApi.getDemandesParStatut(filtresApi),
        dashboardApi.getDemandesParMois(filtresApi),
        dashboardApi.getDemandesParService(filtresApi),
      ])
      return {
        summary,
        statuts,
        demandesMois,
        servicesTop,
        paiements: null,
        echeances: null,
        servicesRevenu: [],
        performances,
      }
    }

    if (vue === 'ECONOMAT') {
      const [summary, performances, paiements, echeances, servicesRevenu] = await Promise.all([
        dashboardApi.getSummary(filtresApi),
        dashboardApi.getPerformances(filtresApi),
        dashboardApi.getPaiements(filtresApi),
        dashboardApi.getEcheances(filtresApi),
        dashboardApi.getServices(filtresApi),
      ])
      return {
        summary,
        statuts: [],
        demandesMois: [],
        servicesTop: [],
        paiements,
        echeances,
        servicesRevenu,
        performances,
      }
    }

    const [summary, performances, statuts, demandesMois, servicesTop, paiements, echeances, servicesRevenu] =
      await Promise.all([
        dashboardApi.getSummary(filtresApi),
        dashboardApi.getPerformances(filtresApi),
        dashboardApi.getDemandesParStatut(filtresApi),
        dashboardApi.getDemandesParMois(filtresApi),
        dashboardApi.getDemandesParService(filtresApi),
        dashboardApi.getPaiements(filtresApi),
        dashboardApi.getEcheances(filtresApi),
        dashboardApi.getServices(filtresApi),
      ])
    return {
      summary,
      statuts,
      demandesMois,
      servicesTop,
      paiements,
      echeances,
      servicesRevenu,
      performances,
    }
  }, [vue, dateDebut, dateFin, clientId, serviceId])
}
