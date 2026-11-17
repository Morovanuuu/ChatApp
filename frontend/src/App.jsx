import { useEffect, useState } from 'react'
import Login from './Login.jsx'
import Chat from './Chat.jsx'
import './App.css'

const CHEIE_TOKEN = 'chatapp.token'

// shortcut: tokenul sta in localStorage (citibil de orice script de pe pagina, deci expus la XSS);
// pentru productie se muta intr-un cookie HttpOnly.
function citesteToken() {
  try { return localStorage.getItem(CHEIE_TOKEN) } catch { return null }
}

function salveazaToken(token) {
  try {
    if (token) localStorage.setItem(CHEIE_TOKEN, token)
    else localStorage.removeItem(CHEIE_TOKEN)
  } catch { /* fara localStorage: tokenul ramane doar pana la refresh */ }
}

export default function App() {
  const [token, setToken] = useState(citesteToken)
  const [eu, setEu] = useState(null)
  const [eroare, setEroare] = useState(null)

  // Cu un token (nou sau din localStorage) aflam cine e logat; 401 = token expirat sau invalid
  useEffect(() => {
    if (!token) return
    let anulat = false
    fetch('/api/auth/eu', { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => {
        if (res.status === 401) {
          salveazaToken(null)
          if (!anulat) setToken(null)
          return null
        }
        if (!res.ok) throw new Error(res.status)
        return res.json()
      })
      .then((date) => { if (date && !anulat) setEu(date) })
      .catch(() => { if (!anulat) setEroare('Serverul nu răspunde. Backend-ul este pornit?') })
    return () => { anulat = true }
  }, [token])

  function laLogin(tokenNou) {
    salveazaToken(tokenNou)
    setEroare(null)
    setToken(tokenNou)
  }

  function iesire() {
    salveazaToken(null)
    setEu(null)
    setToken(null)
  }

  if (!token) return <Login onLogin={laLogin} />
  if (eu) return <Chat token={token} eu={eu} onIesire={iesire} />

  return (
    <main className="card">
      <h1>ChatApp</h1>
      {eroare && <p className="eroare" role="alert">{eroare}</p>}
      {!eroare && <p className="subtitlu">Se încarcă…</p>}
      <button type="button" onClick={iesire}>Ieșire</button>
    </main>
  )
}
