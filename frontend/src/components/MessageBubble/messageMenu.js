import ReplyIcon from '../Icons/ReplyIcon'
import EditIcon from '../Icons/EditIcon'
import CopyIcon from '../Icons/CopyIcon'
import LinkIcon from '../Icons/LinkIcon'
import PinIcon from '../Icons/PinIcon'
import ForwardIcon from '../Icons/ForwardIcon'
import SelectIcon from '../Icons/SelectIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import ResendIcon from '../Icons/ResendIcon'
import CancelIcon from '../Icons/CancelIcon'

export function messageMenuItems({ message, canDeleteOthers, actions = {}, onResend, onDelete, onSelect }) {
    const deleteItem = { label: 'Delete', Icon: DeleteIcon, danger: true, onClick: onDelete }
    if (message.mine && message.status === 'failed') return [{ label: 'Resend', Icon: ResendIcon, onClick: onResend }, deleteItem]
    if (message.status === 'sending') return [{ label: 'Cancel Sending', Icon: CancelIcon, danger: true, onClick: onDelete }]
    return [
        actions.canReply && { label: 'Reply', Icon: ReplyIcon, onClick: () => actions.reply(message) },
        message.mine && actions.canEdit && { label: 'Edit', Icon: EditIcon, onClick: () => actions.edit(message) },
        { label: 'Copy', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(message.text).catch(() => {}) },
        actions.copyLink && { label: 'Copy Link', Icon: LinkIcon, onClick: () => actions.copyLink(message) },
        actions.canPin && { label: actions.pinnedId === message.id ? 'Unpin' : 'Pin', Icon: PinIcon, onClick: () => actions.pin(message) },
        actions.forward && { label: 'Forward', Icon: ForwardIcon, onClick: () => actions.forward([message]) },
        onSelect && { label: 'Select', Icon: SelectIcon, onClick: onSelect },
        (message.mine || canDeleteOthers) && deleteItem,
    ].filter(Boolean)
}
