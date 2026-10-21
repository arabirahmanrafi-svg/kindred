import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, tokenStore } from './api.js'

const AuthContext = createContext(null)

/** Knows who is logged in (therapists and admins only; people searching never log in). */
export function AuthProvider({ children }) {
  const [me, setMe] = useState(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!tokenStore.get()) {
      setMe(null)
      setLoading(false)
      return null
    }
    try {
      const m = await api.me()
      setMe(m)
      return m
    } catch {
      tokenStore.clear()
      setMe(null)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const signIn = useCallback((result) => {
    tokenStore.set(result.token)
    setMe(result.me)
  }, [])

  const signOut = useCallback(async () => {
    try { await api.logout() } catch { /* already logged out */ }
    tokenStore.clear()
    setMe(null)
  }, [])

  return (
    <AuthContext.Provider value={{ me, loading, refresh, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
