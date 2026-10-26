import { useApi } from '@/hooks/useApi'
import { backupApi } from '../services/backup.service'
import type { BackupInfos, Sauvegarde } from '../types'

export function useSauvegardes() {
  return useApi<Sauvegarde[]>(() => backupApi.list(), [])
}

export function useBackupInfos() {
  return useApi<BackupInfos>(() => backupApi.infos(), [])
}
