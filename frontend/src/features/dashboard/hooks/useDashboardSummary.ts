import { useApi } from '@/hooks/useApi'
import { dashboardApi } from '../services/dashboard.service'
import type { DashboardSummary } from '../types'

export function useDashboardSummary() {
  return useApi<DashboardSummary>(dashboardApi.getSummary)
}