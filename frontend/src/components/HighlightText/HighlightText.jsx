import styles from './HighlightText.module.css'

function HighlightText({ text, query }) {
    const needle = query?.trim().toLowerCase()
    if (!needle) return text

    const parts = []
    const lower = text.toLowerCase()
    let from = 0
    let index = lower.indexOf(needle)
    while (index !== -1) {
        if (index > from) parts.push(text.slice(from, index))
        parts.push(<mark key={index} className={styles.match}>{text.slice(index, index + needle.length)}</mark>)
        from = index + needle.length
        index = lower.indexOf(needle, from)
    }
    if (from < text.length) parts.push(text.slice(from))
    return parts
}

export default HighlightText
