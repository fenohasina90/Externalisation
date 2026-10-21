import { useApi } from '@/hooks/useApi'
import { dashboardApi } from '../services/dashboard.service'
import type { DashboardFiltreOptions } from '../types'

export function useDashboardFiltres() {
  return useApi<DashboardFiltreOptions>(dashboardApi.getFiltres)
}
