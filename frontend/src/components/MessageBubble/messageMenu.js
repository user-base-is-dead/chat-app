import ReplyIcon from '../Icons/ReplyIcon'
import EditIcon from '../Icons/EditIcon'
import CopyIcon from '../Icons/CopyIcon'
import TranslateIcon from '../Icons/TranslateIcon'
import LinkIcon from '../Icons/LinkIcon'
import PinIcon from '../Icons/PinIcon'
import ForwardIcon from '../Icons/ForwardIcon'
import SelectIcon from '../Icons/SelectIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import ResendIcon from '../Icons/ResendIcon'
import CancelIcon from '../Icons/CancelIcon'

export function messageMenuItems({ message, canDeleteOthers, onResend, onDelete, onSelect }) {
    const deleteItem = { label: 'Delete', Icon: DeleteIcon, danger: true, onClick: onDelete }
    const sent = [
        { label: 'Reply', Icon: ReplyIcon },
        { label: 'Edit', Icon: EditIcon },
        { label: 'Copy', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(message.text) },
        { label: 'Translate', Icon: TranslateIcon },
        { label: 'Copy Message Link', Icon: LinkIcon },
        { label: 'Pin', Icon: PinIcon },
        { label: 'Forward', Icon: ForwardIcon },
        { label: 'Select', Icon: SelectIcon, onClick: onSelect },
        deleteItem,
    ]
    if (!message.mine) return sent.filter((item) => item.label !== 'Edit' && (canDeleteOthers || item !== deleteItem))
    if (message.status === 'failed') return [{ label: 'Resend', Icon: ResendIcon, onClick: onResend }, { label: 'Select', Icon: SelectIcon, onClick: onSelect }, deleteItem]
    if (message.status === 'sending') return [{ label: 'Cancel Sending', Icon: CancelIcon, onClick: onDelete }, deleteItem]
    return sent
}
