import { useEffect, useState } from 'react'

function read<T>(key: string, fallback: T): T {
  try {
    const stored = window.sessionStorage.getItem(key)
    return stored ? { ...fallback, ...JSON.parse(stored) } : fallback
  } catch {
    return fallback
  }
}

export function usePersistedState<T extends object>(key: string, initial: T) {
  const [value, setValue] = useState<T>(() => read(key, initial))
  useEffect(() => {
    try {
      window.sessionStorage.setItem(key, JSON.stringify(value))
    } catch {
      return
    }
  }, [key, value])
  return [value, setValue] as const
}
