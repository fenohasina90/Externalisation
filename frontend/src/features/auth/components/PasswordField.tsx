import { useState, type InputHTMLAttributes } from 'react'
import { Field, Input } from '@/components/ui'

type PasswordFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
  id: string
  label: string
  error?: string
  hint?: string
}

const iconProps = {
  width: 22,
  height: 22,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
} as const

function EyeIcon() {
  return (
    <svg {...iconProps}>
      <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg {...iconProps}>
      <path d="M3 3l18 18" />
      <path d="M10.6 6.1A10 10 0 0 1 12 6c6 0 10 6 10 6a17 17 0 0 1-3 3.5M6.5 7.5A17 17 0 0 0 2 12s4 6 10 6a9 9 0 0 0 3.5-.7" />
    </svg>
  )
}

export function PasswordField({ id, label, error, hint, ...input }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)

  return (
    <Field label={label} id={id} error={error} hint={hint}>
      <div className="password-field">
        <Input id={id} type={visible ? 'text' : 'password'} {...input} />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((value) => !value)}
          aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
          aria-pressed={visible}
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      </div>
    </Field>
  )
}