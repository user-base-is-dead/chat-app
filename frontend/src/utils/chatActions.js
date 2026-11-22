import PinIcon from '../components/Icons/PinIcon'
import MuteIcon from '../components/Icons/MuteIcon'
import UnmuteIcon from '../components/Icons/UnmuteIcon'
import DeleteIcon from '../components/Icons/DeleteIcon'
import RestrictIcon from '../components/Icons/RestrictIcon'
import CopyIcon from '../components/Icons/CopyIcon'
import LogoutIcon from '../components/Icons/LogoutIcon'
import { roomLabel } from './roomLink'

export function chatActions(chat, { onPin, onMute, onRestrict, onUnrestrict, onLeave, onDelete }) {
    return [
        onPin && { label: chat.pinned ? 'Unpin' : 'Pin', Icon: PinIcon, onClick: onPin },
        onMute && (chat.muted ? { label: 'Unmute', Icon: UnmuteIcon, onClick: onMute } : { label: 'Mute', Icon: MuteIcon, onClick: onMute }),
        onRestrict && { label: 'Restrict', Icon: RestrictIcon, onClick: onRestrict },
        onUnrestrict && { label: 'Unrestrict', Icon: RestrictIcon, onClick: onUnrestrict },
        !chat.kind && { label: 'Copy ID', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(chat.peerId).catch(() => {}) },
        onLeave && { label: `Leave ${roomLabel(chat.kind)}`, Icon: LogoutIcon, danger: true, onClick: onLeave },
        onDelete && { label: chat.kind ? `Delete ${roomLabel(chat.kind)}` : 'Delete Chat', Icon: DeleteIcon, danger: true, onClick: onDelete },
    ].filter(Boolean)
}
