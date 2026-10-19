import { http } from '@/api/http'
import type { BackupInfos, RestaurationResultat, Sauvegarde } from '../types'

/** API de sauvegarde et de restauration de la base (module administration). */
export const backupApi = {
  async list(): Promise<Sauvegarde[]> {
    const { data } = await http.get<Sauvegarde[]>('/admin/backup')
    return data
  },

  async infos(): Promise<BackupInfos> {
    const { data } = await http.get<BackupInfos>('/admin/backup/infos')
    return data
  },

  async creer(): Promise<Sauvegarde> {
    const { data } = await http.post<Sauvegarde>('/admin/backup')
    return data
  },

  async supprimer(nomFichier: string): Promise<void> {
    await http.delete(`/admin/backup/${encodeURIComponent(nomFichier)}`)
  },

  /** Télécharge une sauvegarde conservée sur le serveur. */
  async telecharger(nomFichier: string): Promise<Blob> {
    const { data } = await http.get<Blob>(`/admin/backup/${encodeURIComponent(nomFichier)}`, {
      responseType: 'blob',
    })
    return data
  },

  /** Génère et télécharge une sauvegarde ponctuelle, éventuellement chiffrée. */
  async telechargerNouvelle(chiffre: boolean, phrase?: string): Promise<Blob> {
    const { data } = await http.get<Blob>('/admin/backup/download', {
      responseType: 'blob',
      params: { chiffre },
      headers: phrase ? { 'X-Backup-Passphrase': phrase } : undefined,
    })
    return data
  },

  async restaurer(fichier: File, phrase?: string): Promise<RestaurationResultat> {
    const formulaire = new FormData()
    formulaire.append('file', fichier)
    if (phrase && phrase.trim()) formulaire.append('phrase', phrase.trim())
    const { data } = await http.post<RestaurationResultat>('/admin/backup/restore', formulaire)
    return data
  },
}
