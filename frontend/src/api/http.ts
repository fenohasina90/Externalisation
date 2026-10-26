import axios, { AxiosError } from 'axios'
import { API_URL } from '@/config/env'
import type { ApiError, PageQuery } from '@/types'
import { clearSession, getToken } from '@/utils/storage'

export const http = axios.create({
  baseURL: API_URL,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let unauthorizedHandler: (() => void) | null = null

export function registerUnauthorizedHandler(handler: () => void): () => void {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = null
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string }>) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      clearSession()
      unauthorizedHandler?.()
    }
    let message = error.response?.data?.message ?? error.message
    // Réponses binaires (téléchargements) : le message d'erreur est un Blob JSON.
    const donnees: unknown = error.response?.data
    if (donnees instanceof Blob) {
      try {
        const texte = await donnees.text()
        const parsed = JSON.parse(texte) as { message?: string }
        if (parsed.message) message = parsed.message
      } catch {
        // Corps non JSON : on conserve le message générique.
      }
    }
    const apiError: ApiError = {
      status: error.response?.status ?? 0,
      message,
    }
    return Promise.reject(apiError)
  },
)

export interface QueryParams {
  [key: string]: string | number | boolean | null | undefined
}

export function buildQuery(params: QueryParams, page?: PageQuery): URLSearchParams {
  const search = new URLSearchParams()
  const all: QueryParams = { ...params }
  if (page) {
    all.page = page.page
    all.size = page.size
    if (page.recherche) all.recherche = page.recherche
  }
  Object.entries(all).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  })
  return search
}