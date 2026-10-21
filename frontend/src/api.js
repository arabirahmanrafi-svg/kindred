// Thin fetch wrapper. Backend errors are JSON "problem details" with a readable `detail`
// and, for forms, a `fields` map of field → message.

const TOKEN_KEY = 'kindred.token'

export const tokenStore = {
  get() {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set(token) {
    try { localStorage.setItem(TOKEN_KEY, token) } catch { /* private mode */ }
  },
  clear() {
    try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ }
  },
}

export class ApiError extends Error {
  constructor(status, detail, fields) {
    super(detail || `Something went wrong (${status})`)
    this.status = status
    this.fields = fields || {}
  }
}

async function request(path, { method = 'GET', body, auth = false } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = tokenStore.get()
    if (token) headers.Authorization = `Bearer ${token}`
  }
  let res
  try {
    res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(0, "Can't reach Kindred right now. Check your connection and try again.")
  }
  if (!res.ok) {
    let problem = {}
    try { problem = await res.json() } catch { /* not JSON */ }
    throw new ApiError(res.status, problem.detail, problem.fields)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  options: () => request('/api/options'),
  search: (params) => request(`/api/search?${params.toString()}`),
  therapist: (id) => request(`/api/therapists/${id}`),

  register: (email, password) => request('/api/auth/register', { method: 'POST', body: { email, password } }),
  login: (email, password) => request('/api/auth/login', { method: 'POST', body: { email, password } }),
  logout: () => request('/api/auth/logout', { method: 'POST', auth: true }),
  me: () => request('/api/auth/me', { auth: true }),

  myProfile: () => request('/api/me/profile', { auth: true }),
  saveProfile: (profile) => request('/api/me/profile', { method: 'PUT', body: profile, auth: true }),
  confirmAvailability: (accepting) =>
    request('/api/me/profile/availability', { method: 'POST', body: { acceptingNewClients: accepting }, auth: true }),

  reviewQueue: (status) => request(`/api/admin/profiles?status=${status}`, { auth: true }),
  approve: (id) => request(`/api/admin/profiles/${id}/approve`, { method: 'POST', auth: true }),
  reject: (id, reason) => request(`/api/admin/profiles/${id}/reject`, { method: 'POST', body: { reason }, auth: true }),
}

// Options never change while the page is open, so fetch them once.
let optionsPromise
export function loadOptions() {
  if (!optionsPromise) optionsPromise = api.options().catch((e) => { optionsPromise = null; throw e })
  return optionsPromise
}
