import { useState } from "react"
import styles from './AuthPage.module.css'
import logo from '../../assets/logo.png'
import Input from "../../components/Input/Input"
import { login, register } from "../../api/auth"
import Spinner from "../../components/Spinner/Spinner"

function AuthPage({ onLogin }) {

    const [mode, setMode] = useState('login')
    const [displayName, setDisplayName] = useState('')
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [error, setError] = useState('')
    const [shaking, setShaking] = useState(false)
    const [leaving, setLeaving] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [notice, setNotice] = useState('')

    const switchMode = (newMode) => {
        if (newMode === mode) return
        setMode(newMode)
        setError('')
        setNotice('')
    }

    const showError = (message) => {
        setNotice('')
        setError(message)
        setShaking(true)
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (leaving || submitting) return
        if (!username.trim() || !password) {
            showError('Please fill all fields')
            return
        }
        if (mode === 'register') {
            if (!displayName.trim()) {
                showError('Please fill all fields')
                return
            }
            if (password.length < 6) {
                showError('Password must be atleast 6 characters')
                return
            }
            if (password !== confirmPassword) {
                showError('Password do not match')
                return
            }
        }
        setError('')
        setNotice('')
        setSubmitting(true)
        try {
            if (mode === 'register') {
                await register(displayName.trim(), username.trim(), password)
                setMode('login')
                setDisplayName('')
                setPassword('')
                setConfirmPassword('')
                setNotice('Account created. Please log in.')
                return
            }
            const user = await login(username.trim(), password)
            setLeaving(true)
            setTimeout(() => onLogin(user), 700)
        } catch (err) {
            showError(err.message)
        } finally {
            setSubmitting(false)
        }
    }


    return (
        <div className={styles.page}>
            <div className={leaving ? `${styles.card} ${styles.cardLeave}` : styles.card}>
                <div className={shaking ? styles.shake : undefined} onAnimationEnd={(e) => { if (e.target === e.currentTarget) setShaking(false) }}>

                    {/* header */}

                    <div key={mode} className={styles.logo}><img src={logo} alt="Miron Chat logo" /></div>
                    <h1 className={styles.title}>Miron Chat</h1>
                    <p key={`sub-${mode}`} className={styles.subtitle}>{mode === 'login' ? 'Sign in to continue' : 'Create a new account'}</p>

                    {/* tabs */}

                    <div className={styles.tabs}>
                        <span className={mode === 'login' ? styles.indicator : `${styles.indicator} ${styles.indicatorRight}`} />
                        <button type="button" className={mode === 'login' ? styles.tabActive : styles.tab} onClick={() => switchMode('login')}>Login</button>
                        <button type="button" className={mode === 'register' ? styles.tabActive : styles.tab} onClick={() => switchMode('register')}>Register</button>
                    </div>

                    {/* form */}

                    <form className={styles.form} onSubmit={handleSubmit}>
                        <div className={mode === 'register' ? `${styles.collapse} ${styles.collapseOpen}` : styles.collapse} inert={mode !== 'register'}>
                            <div className={styles.collapseInner}>
                                <Input type="text" label="Name" value={displayName} maxLength={64} onChange={(e) => setDisplayName(e.target.value)} />
                            </div>
                        </div>
                        <Input type="text" label="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
                        <Input type="password" label="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
                        <div className={mode === 'register' ? `${styles.collapse} ${styles.collapseOpen}` : styles.collapse} inert={mode !== 'register'}>
                            <div className={styles.collapseInner}>
                                <Input type="password" label="Confirm Password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
                            </div>
                        </div>
                        {error && <p key={error} className={styles.error}>{error}</p>}
                        {notice && <p className={styles.notice}>{notice}</p>}
                        <button className={leaving ? `${styles.submit} ${styles.submitDone}` : styles.submit} type="submit" aria-busy={submitting}>
                            {submitting ? (
                                <Spinner size={22} />
                            ) : (
                                <span key={leaving ? 'done' : mode} className={styles.submitText}>
                                    {leaving ? '✓' : mode === 'login' ? 'LOGIN' : 'REGISTER'}
                                </span>
                            )}
                        </button>
                    </form>

                </div>
            </div>
        </div>
    )
}

export default AuthPage
