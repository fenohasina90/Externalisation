import { describe, expect, it } from 'vitest'
import {
  DOSSIER_TRANSACTION_VIDE,
  dossierTransactionVersFormulaire,
  dossierTransactionVersPayload,
  type DossierTransactionImmobiliere,
} from './dossierTransaction'

const dossier: DossierTransactionImmobiliere = {
  id: 1,
  demandeId: 126,
  referenceDemande: 'REF-2026-126',
  dateDemande: '2026-10-01',
  nomCommercial: 'Jean Dupont',
  numeroIm: 'C001',
  nomSociete: 'Rakoto Immobilier',
  contactClient: '+261 34 00 000 00',
  emailClient: 'contact@rakoto.mg',
  fbClient: 'Rakoto Immobilier',
  adresseClient: 'Lot II A 12 Antananarivo',
  nomProprietairePrecedent: 'Rakoto Pierre',
  actionAchat: true,
  actionVente: false,
  actionLocation: false,
  actionRegularisationPapiers: false,
  actionGestionImmobiliere: false,
  actionAutre: false,
  actionAutrePrecision: null,
  objet: 'TERRAIN',
  objetAutrePrecision: null,
  achatMesureM2: 500,
  achatPosition: 'Plan A',
  achatAxeEmplacement: 'Axe nord',
  achatMontant: 1000000,
  achatPermisConstruire: null,
  venteMesureM2: null,
  ventePosition: null,
  venteAxeEmplacement: null,
  ventePrisePhoto: true,
  venteVisite: false,
  venteMontant: null,
  ventePermisConstruire: null,
  regularisationTypeTerrain: 'COMMUNAL_DOMAINE',
  regularisationSituationTerrain: 'TITRE_BORNE',
  documentPvReconnaissance: true,
  documentNumeroDemandeAcquisition: false,
  documentActeVentePriveeCommunale: true,
  documentSituationJuridique: false,
  documentActeNotoriete: false,
  observations: 'Observation importante',
  nombreCommerciauxTerrain: 2,
  commissionCn: 50000,
  prestationAbonnementMensuel: true,
}

describe('dossier transaction immobilière', () => {
  it('convertit la réponse API en formulaire éditable', () => {
    const f = dossierTransactionVersFormulaire(dossier)

    expect(f.actionAchat).toBe(true)
    expect(f.objet).toBe('TERRAIN')
    expect(f.achatPermisConstruire).toBe('')
    expect(f.ventePrisePhoto).toBe('oui')
    expect(f.venteVisite).toBe('non')
    expect(f.observations).toBe('Observation importante')
  })

  it('convertit le formulaire en payload API en nettoyant les champs vides', () => {
    const payload = dossierTransactionVersPayload({
      ...DOSSIER_TRANSACTION_VIDE,
      actionVente: true,
      nomProprietairePrecedent: '  Rakoto Pierre  ',
      objet: 'MAISON',
      achatPermisConstruire: 'oui',
      ventePrisePhoto: 'non',
      observations: '   ',
    })

    expect(payload.actionVente).toBe(true)
    expect(payload.nomProprietairePrecedent).toBe('Rakoto Pierre')
    expect(payload.objet).toBe('MAISON')
    expect(payload.achatPermisConstruire).toBe(true)
    expect(payload.ventePrisePhoto).toBe(false)
    expect(payload.observations).toBeNull()
  })

  it('conserve un aller-retour formulaire sans perte', () => {
    const payload = dossierTransactionVersPayload(dossierTransactionVersFormulaire(dossier))

    expect(payload.achatMesureM2).toBe(500)
    expect(payload.achatMontant).toBe(1000000)
    expect(payload.regularisationTypeTerrain).toBe('COMMUNAL_DOMAINE')
    expect(payload.documentPvReconnaissance).toBe(true)
    expect(payload.commissionCn).toBe(50000)
    expect(payload.prestationAbonnementMensuel).toBe(true)
  })
})
