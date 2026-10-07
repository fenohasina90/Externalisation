import { STORAGE_KEYS } from '@/config/env'

export function getToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.token)
}

export function saveSession(token: string, utilisateur: unknown): void {
  localStorage.setItem(STORAGE_KEYS.token, token)
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(utilisateur))
}

export function getSessionUtilisateur<T>(): T | null {
  const raw = localStorage.getItem(STORAGE_KEYS.user)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEYS.token)
  localStorage.removeItem(STORAGE_KEYS.user)
}