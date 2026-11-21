import { useEffect, useRef } from 'react'
import styles from './AppAlerts.module.css'
import BellIcon from '../Icons/BellIcon'
import BackIcon from '../Icons/BackIcon'
import ArrowDownIcon from '../Icons/ArrowDownIcon'
import { dayKey, dayLabel } from '../../utils/dayLabel'
import { APP_VERSION, isNewAlert, markAlertsSeen, useAppUpdate } from '../../utils/appUpdate'

const reload = () => window.location.reload()

const pad = (value) => String(value).padStart(2, '0')

function stamp(ms) {
    const day = new Date(ms)
    return `${pad(day.getDate())}/${pad(day.getMonth() + 1)}/${String(day.getFullYear()).slice(2)} · ${pad(day.getHours())}:${pad(day.getMinutes())}`
}

export function AlertButtons({ onOpen }) {
    const { available, unread } = useAppUpdate()
    return (
        <div className={styles.buttons}>
            {available && (
                <button className={styles.update} type="button" onClick={reload}>Update</button>
            )}
            <button className={styles.bell} type="button" aria-label="Notifications" onClick={onOpen}>
                <BellIcon />
                {unread > 0 && <span className={styles.badge}>{unread > 9 ? '9+' : unread}</span>}
            </button>
        </div>
    )
}

export function AlertsPage({ onBack }) {
    const { alerts, seenAt, latest } = useAppUpdate()
    const seenBefore = useRef(seenAt)

    useEffect(() => {
        markAlertsSeen()
    }, [])

    const groups = []
    alerts.forEach((alert) => {
        const last = groups.at(-1)
        if (last && last.key === dayKey(alert.at)) last.items.push(alert)
        else groups.push({ key: dayKey(alert.at), label: dayLabel(alert.at), items: [alert] })
    })

    return (
        <div className={styles.page}>
            <div className={styles.top}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onBack}>
                    <BackIcon />
                </button>
                <h1 className={styles.title}>Notifications</h1>
            </div>
            {alerts.length === 0 && <div className={styles.empty}>No notifications yet</div>}
            {alerts.length > 0 && <div className={styles.list}>
                {groups.map((group) => (
                    <section key={group.key} className={styles.group}>
                        <div className={styles.day}>{group.label}</div>
                        {group.items.map((alert) => {
                            const fresh = isNewAlert(alert, seenBefore.current)
                            const installed = alert.kind === 'update' && alert.versionCode === Number(APP_VERSION)
                            const canUpdate = alert.kind === 'update' && alert.versionCode > Number(APP_VERSION) && alert.versionCode === latest?.versionCode
                            return (
                                <article key={alert.id} className={fresh ? `${styles.card} ${styles.fresh}` : styles.card}>
                                    <div className={styles.cardTop}>
                                        <span className={styles.icon}>{alert.kind === 'update' ? <ArrowDownIcon /> : <BellIcon />}</span>
                                        <span className={styles.heading}>
                                            <span className={styles.cardTitle}>
                                                <span className={styles.name}>{alert.title}</span>
                                                {fresh && <span className={styles.tagNew}>New</span>}
                                                {!fresh && installed && <span className={styles.tag}>Installed</span>}
                                            </span>
                                            <span className={styles.meta}>{alert.kind === 'update' ? 'Update' : 'Notice'} · {stamp(alert.at)}</span>
                                        </span>
                                    </div>
                                    <p className={styles.body}>{alert.body}</p>
                                    {canUpdate && <button className={styles.updateWide} type="button" onClick={reload}>Update now</button>}
                                </article>
                            )
                        })}
                    </section>
                ))}
            </div>}
        </div>
    )
}
