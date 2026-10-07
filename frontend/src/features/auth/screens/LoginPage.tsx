import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { Button, Field, Input } from '@/components/ui'
import { MESSAGES } from '@/constants/messages'
import type { ApiError } from '@/types'
import { AuthLayout, PasswordField } from '../components'
import { useAuth } from '../hooks/useAuth'

interface FieldErrors {
  username?: string
  password?: string
}

function messageFor(error: ApiError): string {
  if (error.status === 401) return MESSAGES.invalidCredentials
  if (error.status === 403) return MESSAGES.accountDisabled
  if (error.status === 0) return MESSAGES.network
  return error.message || MESSAGES.generic
}

export function LoginPage() {
  const { utilisateur, login } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (utilisateur) return <Navigate to={from} replace />

  function validate(): FieldErrors {
    const errors: FieldErrors = {}
    if (!username.trim()) errors.username = MESSAGES.required.username
    if (!password) errors.password = MESSAGES.required.password
    return errors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setFormError(null)

    const errors = validate()
    setFieldErrors(errors)
    if (errors.username || errors.password) {
      document.getElementById(errors.username ? 'username' : 'password')?.focus()
      return
    }

    setSubmitting(true)
    try {
      await login({ username: username.trim(), password })
    } catch (error) {
      setFormError(messageFor(error as ApiError))
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout title="Connexion" subtitle="Accédez à votre espace de gestion.">
      <form className="auth-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
        {formError ? (
          <div className="form-alert" role="alert">
            {formError}
          </div>
        ) : null}

        <Field label="Identifiant" id="username" error={fieldErrors.username}>
          <Input
            id="username"
            placeholder="Votre identifiant"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoFocus
          />
        </Field>

        <PasswordField
          id="password"
          label="Mot de passe"
          placeholder="Votre mot de passe"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
        />

        <div className="auth-form__row">
          <Link className="link" to="/mot-de-passe-oublie">
            Mot de passe oublié ?
          </Link>
        </div>

        <Button type="submit" loading={submitting} loadingLabel="Connexion en cours…">
          Se connecter
        </Button>
      </form>
    </AuthLayout>
  )
}