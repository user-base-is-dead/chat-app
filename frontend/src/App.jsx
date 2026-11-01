import { useEffect, useState } from "react"
import ChatPage from "./pages/Chat/ChatPage"
import AuthPage from "./pages/Auth/AuthPage"
import Splash from "./components/Splash/Splash"
import { currentUser, logout } from "./api/auth"
import { clearSession, hasSession, watchSession } from "./api/client"
import './App.css'

function App() {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(hasSession)

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Tab') e.preventDefault()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  useEffect(() => {
    if (!hasSession()) return
    currentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setChecking(false))
  }, [])

  useEffect(() => watchSession(() => {
    if (!hasSession()) {
      setUser(null)
      return
    }
    currentUser()
      .then((next) => setUser((prev) => (prev?.id === next.id ? prev : next)))
      .catch((error) => {
        if (error.status === 401) setUser(null)
      })
  }), [])

  const handleLogout = () => {
    logout()
    setUser(null)
  }

  const handleAccountGone = () => {
    clearSession()
    setUser(null)
  }

  if (checking) return <Splash />

  return (
    <>
    {user ? <ChatPage key={user.id} user={user} onLogout={handleLogout} onUserChange={setUser} onAccountGone={handleAccountGone} /> : <AuthPage onLogin={setUser} />}
    </>
  )
}

export default App
