import { usePaginatedList } from '@/hooks/usePaginatedList'
import { clientsApi } from '../services/clients.service'

export function useClientsList() {
  return usePaginatedList({ loader: (query) => clientsApi.list(query) })
}