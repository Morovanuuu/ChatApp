import { Fragment, useCallback, useEffect, useState } from 'react'
import './Chat.css'

// shortcut: mesajele noi apar prin polling la 3 s; cand vine WebSocket-ul, polling-ul dispare
const INTERVAL_MS = 3000

function initiale(nume) {
  const p = nume.trim().split(/\s+/)
  return (p.length > 1 ? p[0][0] + p[1][0] : nume.slice(0, 2)).toUpperCase()
}

// trimisLa vine fara fus orar (LocalDateTime), deci e citit ca ora locala
const ora = (iso) => new Date(iso).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })

function ziua(iso) {
  const d = new Date(iso).toDateString()
  const ieri = new Date()
  ieri.setDate(ieri.getDate() - 1)
  if (d === new Date().toDateString()) return 'Azi'
  if (d === ieri.toDateString()) return 'Ieri'
  return new Date(iso).toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })
}

const celalalt = (c, eu) => c.membri.find((m) => m.email !== eu.email)

const titlu = (c, eu) => c.nume || celalalt(c, eu)?.nume || 'Conversație'

function mesajEroare(err) {
  return err instanceof TypeError ? 'Serverul nu răspunde. Backend-ul este pornit?' : err.message
}

export default function Chat({ token, eu, onIesire }) {
  const [conversatii, setConversatii] = useState(null)
  const [selId, setSelId] = useState(null)
  const [mesaje, setMesaje] = useState(null)
  const [nou, setNou] = useState(false)
  const [text, setText] = useState('')
  const [seTrimite, setSeTrimite] = useState(false)
  const [eroare, setEroare] = useState(null)

  // GET fara body, POST cu body JSON; erorile vin ca ProblemDetail
  const api = useCallback(async (cale, body) => {
    const headers = { Authorization: `Bearer ${token}` }
    const res = await fetch(cale, body === undefined ? { headers } : {
      method: 'POST',
      headers: { ...headers, 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (res.status === 401) {
      onIesire() // token expirat: inapoi la login
      throw new Error('Sesiunea a expirat. Autentifică-te din nou.')
    }
    const date = await res.json().catch(() => null)
    if (!res.ok) {
      // validarea Spring (@Valid) intoarce un detail generic, in engleza
      throw new Error(date?.detail === 'Invalid request content.'
        ? 'Date invalide. Verifică ce ai completat.'
        : date?.detail ?? `Eroare ${res.status}`)
    }
    return date
  }, [token, onIesire])

  // Lista de conversatii si mesajele celei deschise, acum si apoi la fiecare INTERVAL_MS
  useEffect(() => {
    let anulat = false
    async function incarca() {
      try {
        const lista = await api('/api/conversatii')
        if (anulat) return
        setConversatii(lista)
        const id = selId ?? lista[0]?.id
        if (id == null) return
        if (selId == null) setSelId(id)
        const m = await api(`/api/conversatii/${id}/mesaje`)
        if (!anulat) {
          setMesaje(m)
          setEroare(null)
        }
      } catch (err) {
        if (!anulat) setEroare(mesajEroare(err))
      }
    }
    incarca()
    const t = setInterval(incarca, INTERVAL_MS)
    return () => {
      anulat = true
      clearInterval(t)
    }
  }, [api, selId])

  function alege(id) {
    if (id !== selId) {
      setSelId(id)
      setMesaje(null)
    }
    setNou(false)
  }

  function laCreare(c) {
    setConversatii((prev) => [c, ...(prev ?? []).filter((x) => x.id !== c.id)])
    alege(c.id)
  }

  async function trimite(e) {
    e.preventDefault()
    const continut = text.trim()
    if (!continut || seTrimite) return
    setSeTrimite(true)
    try {
      const m = await api(`/api/conversatii/${selId}/mesaje`, { continut })
      setMesaje((prev) => (prev ?? []).some((x) => x.id === m.id) ? prev : [...(prev ?? []), m])
      setText('')
    } catch (err) {
      setEroare(mesajEroare(err)) // textul ramane in camp, se poate retrimite
    } finally {
      setSeTrimite(false)
    }
  }

  const activa = conversatii?.find((c) => c.id === selId)

  return (
    <div className="chat">
      <header className="chat-antet">
        <div className="chat-logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
               strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <h1>ChatApp</h1>
        </div>
        <div className="chat-eu">
          <span>{eu.email}</span>
          <button type="button" className="buton-secundar" onClick={onIesire}>Ieșire</button>
        </div>
      </header>

      {eroare && <p className="eroare chat-eroare" role="alert">{eroare}</p>}

      <div className="chat-corp">
        <nav className="chat-lista" aria-label="Conversații">
          <button type="button" className="buton-principal" onClick={() => setNou(true)}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"
                 strokeLinecap="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Conversație nouă
          </button>
          <h2 className="chat-lista-titlu">Conversațiile mele</h2>
          {conversatii === null && <p className="info">Se încarcă…</p>}
          {conversatii?.length === 0 && <p className="info">Nicio conversație încă.</p>}
          {conversatii?.map((c) => {
            const t = titlu(c, eu)
            return (
              <button key={c.id} type="button" className="conv" onClick={() => alege(c.id)}
                      aria-current={c.id === selId && !nou ? 'true' : undefined}>
                <span className="avatar" aria-hidden="true">{initiale(t)}</span>
                <span className="conv-text">
                  <span className="conv-titlu">{t}</span>
                  <span className="conv-sub">{c.nume ? `${c.membri.length} membri` : 'Conversație privată'}</span>
                </span>
              </button>
            )
          })}
        </nav>

        <main className="chat-panou">
          {nou && <ConversatieNoua api={api} onCreata={laCreare} onAnuleaza={() => setNou(false)} />}

          {!nou && !activa && conversatii?.length === 0 && (
            <p className="info gol">Creează o conversație ca să începi.</p>
          )}

          {!nou && activa && (
            <>
              <div className="panou-antet">
                <span className="avatar" aria-hidden="true">{initiale(titlu(activa, eu))}</span>
                <div className="panou-titlu">
                  <h2>{titlu(activa, eu)}</h2>
                  <div className="panou-detalii">
                    {activa.nume ? activa.membri.map((m) => m.nume).join(', ') : celalalt(activa, eu)?.email}
                  </div>
                </div>
              </div>

              <div className="mesaje" role="log" aria-label="Mesaje">
                <ol>
                  {mesaje === null && <li className="info">Se încarcă…</li>}
                  {mesaje?.length === 0 && <li className="info">Niciun mesaj încă. Scrie primul mesaj mai jos.</li>}
                  {mesaje?.map((m, i) => {
                    const alMeu = m.autor.email === eu.email
                    const zi = ziua(m.trimisLa)
                    const ziNoua = i === 0 || ziua(mesaje[i - 1].trimisLa) !== zi
                    const arataNume = activa.nume && !alMeu && (ziNoua || mesaje[i - 1].autor.id !== m.autor.id)
                    return (
                      <Fragment key={m.id}>
                        {ziNoua && <li className="zi">{zi}</li>}
                        <li className={alMeu ? 'mesaj al-meu' : 'mesaj'}>
                          {arataNume && <span className="mesaj-autor">{m.autor.nume}</span>}
                          <div className="bula">{m.continut}</div>
                          <time className="mesaj-ora" dateTime={m.trimisLa}>{ora(m.trimisLa)}</time>
                        </li>
                      </Fragment>
                    )
                  })}
                </ol>
              </div>

              <form className="compune" onSubmit={trimite}>
                <label htmlFor="mesaj" className="sr-only">Mesaj</label>
                <input id="mesaj" type="text" autoComplete="off" maxLength={2000} placeholder="Scrie un mesaj…"
                       value={text} onChange={(e) => setText(e.target.value)} />
                <button type="submit" className="buton-principal" disabled={seTrimite}>
                  Trimite
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                       strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M22 2 11 13" />
                    <path d="M22 2 15 22l-4-9-9-4 20-7z" />
                  </svg>
                </button>
              </form>
            </>
          )}
        </main>
      </div>
    </div>
  )
}

function ConversatieNoua({ api, onCreata, onAnuleaza }) {
  const [nume, setNume] = useState('')
  const [membri, setMembri] = useState('')
  const [eroare, setEroare] = useState(null)
  const [seTrimite, setSeTrimite] = useState(false)

  async function creeaza(e) {
    e.preventDefault()
    const emailuri = membri.split(',').map((x) => x.trim()).filter(Boolean)
    if (emailuri.length === 0) {
      setEroare('Adaugă cel puțin un membru.')
      return
    }
    setEroare(null)
    setSeTrimite(true)
    try {
      // regulile (privata = exact 2 membri, emailuri existente) le verifica backend-ul
      onCreata(await api('/api/conversatii', { nume: nume.trim() || null, membri: emailuri }))
    } catch (err) {
      setEroare(mesajEroare(err))
      setSeTrimite(false)
    }
  }

  return (
    <div className="formular-nou-loc">
      <form className="formular-nou" onSubmit={creeaza}>
        <h2>Conversație nouă</h2>
        <p>Fără nume e o conversație privată cu o singură persoană. Cu nume devine grup.</p>
        <label htmlFor="nume">Nume grup (opțional)</label>
        <input id="nume" type="text" autoComplete="off" maxLength={100} placeholder="de exemplu Proiect Web"
               value={nume} onChange={(e) => setNume(e.target.value)} />
        <label htmlFor="membri">Membri</label>
        <input id="membri" type="text" autoComplete="off" placeholder="mihai@chatapp.ro, admin@chatapp.ro"
               aria-describedby="membri-ajutor" value={membri} onChange={(e) => setMembri(e.target.value)} />
        <span id="membri-ajutor" className="ajutor">Emailuri separate prin virgulă. Tu ești adăugat automat.</span>
        {eroare && <p className="eroare" role="alert">{eroare}</p>}
        <div className="formular-actiuni">
          <button type="button" className="buton-secundar" onClick={onAnuleaza}>Anulează</button>
          <button type="submit" className="buton-principal" disabled={seTrimite}>
            {seTrimite ? 'Se creează…' : 'Creează'}
          </button>
        </div>
      </form>
    </div>
  )
}
