import { accessToken, refreshSession } from './client'

const UNAUTHORIZED = 4001
const MAX_DELAY = 15000

export function connectRealtime({ onEvent, onReady }) {
    let socket = null
    let closed = false
    let attempt = 0
    let timer = null

    const scheduleReconnect = () => {
        if (closed) return
        const delay = Math.min(MAX_DELAY, 1000 * 2 ** attempt)
        attempt += 1
        timer = setTimeout(connect, delay)
    }

    const connect = () => {
        if (closed) return
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
        socket = new WebSocket(`${protocol}://${window.location.host}/v1/ws`)

        socket.onopen = () => {
            socket.send(JSON.stringify({ event: 'auth', data: { token: accessToken() } }))
        }

        socket.onmessage = (e) => {
            let message
            try {
                message = JSON.parse(e.data)
            } catch {
                return
            }
            if (message.event === 'ready') {
                attempt = 0
                onReady()
                return
            }
            onEvent(message.event, message.data)
        }

        socket.onclose = async (e) => {
            socket = null
            if (closed) return
            if (e.code === UNAUTHORIZED) {
                try {
                    await refreshSession()
                } catch (error) {
                    if (error.status === 401) {
                        closed = true
                        return
                    }
                }
            }
            scheduleReconnect()
        }
    }

    const handleOnline = () => {
        if (socket || closed) return
        clearTimeout(timer)
        attempt = 0
        connect()
    }

    window.addEventListener('online', handleOnline)
    connect()

    return {
        send(event, data) {
            if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ event, data }))
        },
        close() {
            closed = true
            clearTimeout(timer)
            window.removeEventListener('online', handleOnline)
            socket?.close()
        },
    }
}
