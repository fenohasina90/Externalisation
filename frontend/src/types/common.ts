export interface ApiError {
  status: number
  message: string
  detail?: string
  fieldErrors?: Record<string, string>
}

export interface ApiPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export interface PageRequest {
  page?: number
  size?: number
  recherche?: string
}

export interface PageQuery {
  page: number
  size: number
  recherche: string | null
}

export type NomRole = 'SUPER_ADMIN' | 'ADMIN' | 'ACCUEIL' | 'ECONOMAT'