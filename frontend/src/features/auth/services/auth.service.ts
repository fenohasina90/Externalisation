import { http } from '@/api/http'
import type {
  AuthResponse,
  ChangePasswordPayload,
  ForgotPasswordPayload,
  LoginCredentials,
  MessageResponse,
} from '../types'

export const authApi = {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/auth/login', credentials)
    return data
  },

  async me(): Promise<AuthResponse> {
    const { data } = await http.get<AuthResponse>('/auth/me')
    return data
  },

  async forgotPassword(payload: ForgotPasswordPayload): Promise<MessageResponse> {
    const { data } = await http.post<MessageResponse>('/auth/forgot-password', payload)
    return data
  },

  async changePassword(payload: ChangePasswordPayload): Promise<MessageResponse> {
    const { data } = await http.post<MessageResponse>('/auth/change-password', payload)
    return data
  },

  async logout(): Promise<void> {
    try {
      await http.post('/auth/logout')
    } catch {
      // La déconnexion locale reste valide même si l'API est indisponible.
    }
  },
}