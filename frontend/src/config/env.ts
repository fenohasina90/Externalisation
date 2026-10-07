export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export const STORAGE_KEYS = {
  token: 'cabex.token',
  user: 'cabex.user',
} as const