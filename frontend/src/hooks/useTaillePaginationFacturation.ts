import { useEffect, useState } from 'react'
import { getConfigurationPublique, getConfigurationPubliqueCourante } from '@/utils/affichage'

/**
 * Nombre de lignes par page dans l'onglet « Facturation » de la fiche client
 * (PAGINATION_FACTURATION, 15 par défaut). La pagination globale des listes
 * est déterminée par PAGINATION_TAILLE_DEFAUT via useTaillePagination().
 */
export function useTaillePaginationFacturation(): number {
  const [taille, setTaille] = useState(
    () =>
      getConfigurationPubliqueCourante()?.taillePaginationFacturation ?? 15,
  )

  useEffect(() => {
    let ignore = false
    void getConfigurationPublique().then((publique) => {
      if (!ignore) setTaille(publique.taillePaginationFacturation)
    })
    return () => {
      ignore = true
    }
  }, [])

  return taille
}
