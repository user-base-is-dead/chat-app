import { useEffect, useState } from 'react'
import { listMembers, MEMBER_PAGE_SIZE } from '../../api/rooms'
import { listRoles } from '../../api/roles'

export const MEMBER_LIMIT = 1000

async function loadMembers(roomId, channelId) {
    const all = []
    while (all.length < MEMBER_LIMIT) {
        const page = await listMembers(roomId, all.length, channelId)
        all.push(...page)
        if (page.length < MEMBER_PAGE_SIZE) break
    }
    return all.filter((member) => member.status === 'active').slice(0, MEMBER_LIMIT)
}

export function useGroupMembers(groupId, channelId, refreshKey) {
    const [state, setState] = useState({ key: null, members: [], roles: [], loadedAt: 0 })

    useEffect(() => {
        if (!groupId) return
        let active = true
        Promise.all([loadMembers(groupId, channelId), listRoles(groupId)])
            .then(([members, roles]) => active && setState({ key: `${groupId}:${channelId}`, members, roles, loadedAt: Date.now() }))
            .catch(() => active && setState({ key: `${groupId}:${channelId}`, members: [], roles: [], loadedAt: Date.now() }))
        return () => {
            active = false
        }
    }, [groupId, channelId, refreshKey])

    return state.key === `${groupId}:${channelId}` ? state : null
}
