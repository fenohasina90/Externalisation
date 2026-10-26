import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { NotificationUser, NotificationsNonLues } from '../types'

export interface NotificationFiltres {
  type?: string
  nonLues?: boolean
}

export const notificationsApi = {
  async list(
    page?: PageQuery,
    filtres: NotificationFiltres = {},
  ): Promise<ApiPage<NotificationUser>> {
    const { data } = await http.get<ApiPage<NotificationUser>>('/notifications', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async listNonLues(): Promise<NotificationsNonLues> {
    const { data } = await http.get<NotificationsNonLues>('/notifications/non-lues')
    return data
  },

  async marquerLue(id: number): Promise<NotificationUser> {
    const { data } = await http.patch<NotificationUser>(`/notifications/${id}/lu`)
    return data
  },

  async toutLire(): Promise<number> {
    const { data } = await http.patch<{ misAJour: number }>('/notifications/tout-lire')
    return data.misAJour
  },

  async supprimer(id: number): Promise<void> {
    await http.delete(`/notifications/${id}`)
  },
}
