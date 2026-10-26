import type { ApiError } from '@/types'

export const DEFAULT_ERROR_MESSAGE = 'Une erreur est survenue. Veuillez réessayer.'

export const MESSAGES = {
  required: {
    username: 'Veuillez saisir votre identifiant.',
    password: 'Veuillez saisir votre mot de passe.',
    email: 'Veuillez saisir votre adresse e-mail.',
  },
  invalidCredentials: 'Identifiant ou mot de passe incorrect. Veuillez vérifier.',
  accountDisabled: 'Ce compte est désactivé. Contactez votre administrateur.',
  network: 'Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.',
  generic: DEFAULT_ERROR_MESSAGE,
} as const

export function messageErreur(error: unknown): string {
  if (isApiError(error)) {
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return DEFAULT_ERROR_MESSAGE
}

function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'message' in error &&
    typeof (error as ApiError).message === 'string'
  )
}