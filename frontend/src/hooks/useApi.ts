import { useCallback, useEffect, useState } from 'react'
import { messageErreur } from '@/constants/messages'

interface ApiState<T> {
  data: T | null
  loading: boolean
  error: string | null
}

type Loader<T> = () => Promise<T>

export function useApi<T>(loader: Loader<T>, deps: unknown[] = []) {
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: true,
    error: null,
  })
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let stale = false

    async function load(): Promise<void> {
      setState((previous) => ({ ...previous, loading: true, error: null }))
      try {
        const data = await loader()
        if (!stale) setState({ data, loading: false, error: null })
      } catch (error) {
        if (!stale) setState({ data: null, loading: false, error: messageErreur(error) })
      }
    }

    void load()
    return () => {
      stale = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  return { ...state, reload }
}