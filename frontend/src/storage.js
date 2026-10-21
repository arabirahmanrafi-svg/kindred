// Saved therapists live only on this device. Kindred never learns who saved whom.
const KEY = 'kindred.saved'

function read() {
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(parsed) ? parsed.filter((n) => Number.isInteger(n)) : []
  } catch {
    return []
  }
}

function write(ids) {
  try { localStorage.setItem(KEY, JSON.stringify(ids)) } catch { /* private mode */ }
  window.dispatchEvent(new Event('kindred-saved'))
}

export const saved = {
  all: read,
  has: (id) => read().includes(id),
  toggle(id) {
    const ids = read()
    write(ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id])
  },
  clear: () => write([]),
}
