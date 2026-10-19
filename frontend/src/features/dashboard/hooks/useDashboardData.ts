import { useApi } from '@/hooks/useApi'
import { dashboardApi } from '../services/dashboard.service'
import type { DashboardData } from '../types'

export type VueDashboard = 'ACCUEIL' | 'ECONOMAT' | 'SUPER_ADMIN'

/**
 * Charge uniquement les blocs autorisés pour le rôle : l'Accueil n'appelle pas
 * les endpoints financiers et l'Économat n'appelle pas les endpoints demandes.
 */
export function useDashboardData(vue: VueDashboard) {
  return useApi<DashboardData>(async () => {
    const summary = await dashboardApi.getSummary()

    if (vue === 'ACCUEIL') {
      const [statuts, demandesMois, servicesTop] = await Promise.all([
        dashboardApi.getDemandesParStatut(),
        dashboardApi.getDemandesParMois(),
        dashboardApi.getDemandesParService(),
      ])
      return {
        summary,
        statuts,
        demandesMois,
        servicesTop,
        paiements: null,
        echeances: null,
        servicesRevenu: [],
      }
    }

    if (vue === 'ECONOMAT') {
      const [paiements, echeances, servicesRevenu] = await Promise.all([
        dashboardApi.getPaiements(),
        dashboardApi.getEcheances(),
        dashboardApi.getServices(),
      ])
      return {
        summary,
        statuts: [],
        demandesMois: [],
        servicesTop: [],
        paiements,
        echeances,
        servicesRevenu,
      }
    }

    const [statuts, demandesMois, servicesTop, paiements, echeances, servicesRevenu] =
      await Promise.all([
        dashboardApi.getDemandesParStatut(),
        dashboardApi.getDemandesParMois(),
        dashboardApi.getDemandesParService(),
        dashboardApi.getPaiements(),
        dashboardApi.getEcheances(),
        dashboardApi.getServices(),
      ])
    return {
      summary,
      statuts,
      demandesMois,
      servicesTop,
      paiements,
      echeances,
      servicesRevenu,
    }
  }, [vue])
}
