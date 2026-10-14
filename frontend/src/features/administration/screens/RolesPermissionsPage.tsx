import { useEffect, useMemo, useState } from 'react'
import type { Role } from '@/features/auth/types'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { PermissionsMatrix, type PermissionLigne } from '../components'
import { permissionsApi, rolesApi, utilisateursApi } from '../services/administration.service'
import type { Permission, RoleDetail } from '../types'

type Champ = 'voir' | 'creer' | 'modifier' | 'desactiver'

const ACTIONS: { suffixe: string; champ: Champ }[] = [
  { suffixe: '_VIEW', champ: 'voir' },
  { suffixe: '_CREATE', champ: 'creer' },
  { suffixe: '_UPDATE', champ: 'modifier' },
  { suffixe: '_DELETE', champ: 'desactiver' },
]

const MODULES_ORDRE: { code: string; libelle: string }[] = [
  { code: 'DASHBOARD', libelle: 'Tableau de bord' },
  { code: 'UTILISATEUR', libelle: 'Utilisateurs' },
  { code: 'CLIENT', libelle: 'Clients' },
  { code: 'RESPONSABLE', libelle: 'Responsables' },
  { code: 'COMMERCIAL', libelle: 'Commerciaux' },
  { code: 'SERVICE', libelle: 'Services & domaines' },
  { code: 'DEMANDE', libelle: 'Demandes' },
  { code: 'DOSSIER', libelle: 'Dossiers de création' },
  { code: 'PAIEMENT', libelle: 'Paiements' },
  { code: 'DOCUMENT', libelle: 'Documents' },
  { code: 'RAPPORT', libelle: 'Rapports' },
  { code: 'AUDIT', libelle: 'Audit' },
  { code: 'CONFIGURATION', libelle: 'Configuration' },
]

function estStandard(code: string): boolean {
  return ACTIONS.some((a) => code.endsWith(a.suffixe))
}

/**
 * Rôles et permissions branchés sur l'API : cartes des rôles, matrice
 * construite depuis les permissions réelles (regroupées par module),
 * enregistrement via PUT /roles/{id} (liste des permissionIds).
 * SUPER_ADMIN : lecture seule.
 */
export function RolesPermissionsPage() {
  const { utilisateur, refresh } = useAuth()
  const [choix, setChoix] = useState<number | null>(null)
  const [selection, setSelection] = useState<Set<number>>(new Set())
  const [selectionPour, setSelectionPour] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)
  const [compteurs, setCompteurs] = useState<Record<number, number>>({})

  const { data: roles, loading: chargementRoles, error: erreurRoles } = useApi<Role[]>(
    () => rolesApi.list(),
    [],
  )
  const { data: toutesPermissions } = useApi<Permission[]>(() => permissionsApi.list(), [])

  // Rôle effectif : choix explicite, sinon ACCUEIL par défaut, sinon premier rôle.
  const roleId = choix ?? roles?.find((r) => r.nomRole === 'ACCUEIL')?.id ?? roles?.[0]?.id ?? null

  const {
    data: detail,
    loading: chargementDetail,
    reload: rechargerDetail,
  } = useApi<RoleDetail | null>(
    () => (roleId === null ? Promise.resolve(null) : rolesApi.get(roleId)),
    [roleId],
  )

  // Resynchronise la sélection à chaque changement de rôle (ajustement pendant
  // le rendu, pas dans un effet : pas de rendus en cascade).
  if ((detail?.id ?? null) !== selectionPour) {
    setSelectionPour(detail?.id ?? null)
    setSelection(new Set((detail?.permissions ?? []).map((p) => p.id)))
  }

  useEffect(() => {
    if (!roles) return
    let actif = true
    void Promise.all(
      roles.map(async (r) => {
        const page = await utilisateursApi.list({ page: 0, size: 1, recherche: null }, { roleId: r.id })
        return [r.id, page.totalElements] as const
      }),
    )
      .then((paires) => {
        if (actif) setCompteurs(Object.fromEntries(paires))
      })
      .catch(() => undefined)
    return () => {
      actif = false
    }
  }, [roles])

  const parId = useMemo(
    () => new Map((toutesPermissions ?? []).map((p) => [p.id, p])),
    [toutesPermissions],
  )

  const modules = useMemo(() => {
    const presents = new Set((toutesPermissions ?? []).map((p) => p.module))
    const connus = MODULES_ORDRE.filter((m) => presents.has(m.code))
    const inconnus = [...presents]
      .filter((code) => !MODULES_ORDRE.some((m) => m.code === code))
      .sort()
      .map((code) => ({ code, libelle: code }))
    return [...connus, ...inconnus]
  }, [toutesPermissions])

  const lignes: PermissionLigne[] = useMemo(
    () =>
      modules.map(({ code, libelle }) => {
        const ligne: PermissionLigne = {
          module: libelle,
          voir: false,
          creer: false,
          modifier: false,
          desactiver: false,
          indisponibles: [],
        }
        for (const { suffixe, champ } of ACTIONS) {
          const codes = (toutesPermissions ?? []).filter(
            (p) => p.module === code && p.codePermission.endsWith(suffixe),
          )
          if (codes.length === 0) {
            ligne.indisponibles!.push(champ)
          } else {
            ligne[champ] = codes.every((p) => selection.has(p.id))
          }
        }
        return ligne
      }),
    [modules, toutesPermissions, selection],
  )

  const speciales = useMemo(
    () => (toutesPermissions ?? []).filter((p) => !estStandard(p.codePermission)),
    [toutesPermissions],
  )

  const carte = roles?.find((r) => r.id === roleId) ?? null
  const modifiable = carte !== null && carte.nomRole !== 'SUPER_ADMIN'

  const codesStandards = (module: string, champ: Champ): Permission[] => {
    const suffixe = ACTIONS.find((a) => a.champ === champ)!.suffixe
    return (toutesPermissions ?? []).filter(
      (p) => p.module === module && p.codePermission.endsWith(suffixe),
    )
  }

  const moduleDeLibelle = (libelle: string): string =>
    MODULES_ORDRE.find((m) => m.libelle === libelle)?.code ?? libelle

  const handleToggle = (libelle: string, champ: Champ) => {
    if (!modifiable) return
    const ids = codesStandards(moduleDeLibelle(libelle), champ).map((p) => p.id)
    setSelection((prev) => {
      const next = new Set(prev)
      if (ids.every((id) => next.has(id))) {
        ids.forEach((id) => next.delete(id))
      } else {
        ids.forEach((id) => next.add(id))
      }
      return next
    })
    setMessage(null)
    setErreur(null)
  }

  const handleToggleSpeciale = (id: number) => {
    if (!modifiable) return
    setSelection((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
    setMessage(null)
    setErreur(null)
  }

  const handleAnnuler = () => {
    setSelection(new Set((detail?.permissions ?? []).map((p) => p.id)))
    setMessage(null)
    setErreur(null)
  }

  const handleEnregistrer = async () => {
    if (roleId === null) return
    setEnregistrement(true)
    setMessage(null)
    setErreur(null)
    try {
      await rolesApi.update(roleId, { permissionIds: [...selection] })
      // Si le rôle modifié est celui de l'utilisateur connecté, ses droits
      // affichés (menus, gardes de route) sont rechargés aussitôt, sans reconnexion.
      if (utilisateur !== null && utilisateur.role.id === roleId) {
        await refresh()
      }
      setMessage(`Permissions du rôle ${carte?.nomRole} enregistrées.`)
      rechargerDetail()
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Échec de l’enregistrement')
    } finally {
      setEnregistrement(false)
    }
  }

  if (chargementRoles) return <Spinner label="Chargement des rôles…" />
  if (erreurRoles || !roles) {
    return (
      <div className="app-page">
        <Alert tone="danger">{erreurRoles ?? 'Impossible de charger les rôles.'}</Alert>
      </div>
    )
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Rôles et permissions</h1>
          <p>Définissez ce que chaque rôle peut voir et faire.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleAnnuler} disabled={!modifiable}>
            Annuler
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => void handleEnregistrer()}
            disabled={!modifiable || enregistrement}
          >
            {enregistrement ? 'Enregistrement…' : 'Enregistrer les permissions'}
          </button>
        </div>
      </header>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}
      {erreur && (
        <div className="alert alert-danger" role="alert">
          {erreur}
        </div>
      )}

      <div className="roles-layout">
        <section aria-label="Rôles">
          {roles.map((r) => {
            const nb = compteurs[r.id] ?? 0
            return (
              <button
                key={r.id}
                type="button"
                onClick={() => {
                  setChoix(r.id)
                  setMessage(null)
                  setErreur(null)
                }}
                className={`card role-card ${r.id === roleId ? 'role-card--selected' : ''}`}
              >
                <span className="role-card__head">
                  <span className="role-card__name">{r.nomRole}</span>
                  <span className="pill wt">
                    ● {nb} utilisateur{nb > 1 ? 's' : ''}
                  </span>
                </span>
                <span className="role-card__desc">{r.description ?? '—'}</span>
              </button>
            )
          })}
        </section>

        <section className="card">
          <div className="card-pad" style={{ paddingBottom: 0 }}>
            <h3 style={{ margin: 0 }}>Permissions du rôle {carte?.nomRole}</h3>
            {!modifiable && <p className="section-note">Accès complet. Non modifiable.</p>}
          </div>
          {chargementDetail ? (
            <div className="card-pad">
              <Spinner label="Chargement des permissions…" />
            </div>
          ) : (
            <div className="table-scroll-container no-global-scroll">
              <div className="table-wrap">
                <PermissionsMatrix lignes={lignes} modifiable={modifiable} onToggle={handleToggle} />
              </div>
              {speciales.length > 0 && (
                <div className="card-pad" style={{ paddingTop: 0 }}>
                  <h4 style={{ margin: '12px 0 8px' }}>Permissions spéciales</h4>
                  <ul className="speciales-list">
                    {speciales.map((p) => (
                      <li key={p.id}>
                        <label>
                          <input
                            type="checkbox"
                            className="perm-check"
                            checked={selection.has(p.id)}
                            disabled={!modifiable}
                            onChange={() => handleToggleSpeciale(p.id)}
                          />
                          <span>
                            {p.nomPermission}{' '}
                            <code className="code-inline">{p.codePermission}</code>
                            {parId.get(p.id)?.description && (
                              <span className="text-muted"> — {parId.get(p.id)!.description}</span>
                            )}
                          </span>
                        </label>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
