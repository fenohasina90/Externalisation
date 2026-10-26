import { useState } from 'react'
import { useApi } from './useApi'
import { useDebounce } from './useDebounce'
import type { ApiPage, PageQuery } from '@/types'

interface PaginatedOptions<T> {
  loader: (query: PageQuery) => Promise<ApiPage<T>>
  size?: number
}

export function usePaginatedList<T>({ loader, size = 10 }: PaginatedOptions<T>) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const terme = useDebounce(search, 300)

  const { data, loading, error, reload } = useApi<ApiPage<T>>(
    () => loader({ page, size, recherche: terme.trim() || null }),
    [page, size, terme],
  )

  function handleSearch(value: string): void {
    setSearch(value)
    setPage(0)
  }

  return {
    items: data?.content ?? [],
    total: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    size,
    loading,
    error,
    reload,
    setPage,
    search,
    setSearch: handleSearch,
  }
}