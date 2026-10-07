import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type { NotificationUser } from '../types'

export const notificationsApi = {
  async list(page?: PageQuery): Promise<ApiPage<NotificationUser>> {
    const { data } = await http.get<ApiPage<NotificationUser>>('/notifications', {
      params: buildQuery({}, page),
    })
    return data
  },

  async listNonLues(): Promise<NotificationUser[]> {
    const { data } = await http.get<NotificationUser[]>('/notifications/non-lues')
    return data
  },

  async marquerLue(id: number): Promise<NotificationUser> {
    const { data } = await http.patch<NotificationUser>(`/notifications/${id}/lu`)
    return data
  },

  async toutLire(): Promise<void> {
    await http.patch('/notifications/tout-lire')
  },

  async supprimer(id: number): Promise<void> {
    await http.delete(`/notifications/${id}`)
  },
}