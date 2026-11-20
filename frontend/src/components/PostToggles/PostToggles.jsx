import styles from './PostToggles.module.css'

const toggles = [
    { key: 'allowReactions', label: 'Reactions' },
    { key: 'allowComments', label: 'Comments' },
]

function PostToggles({ value, onChange }) {
    return (
        <div className={styles.toggles}>
            {toggles.map(({ key, label }) => (
                <button
                    key={key}
                    className={value[key] ? `${styles.toggle} ${styles.on}` : styles.toggle}
                    type="button"
                    aria-pressed={value[key]}
                    onClick={() => onChange({ ...value, [key]: !value[key] })}
                >
                    {label} {value[key] ? 'on' : 'off'}
                </button>
            ))}
        </div>
    )
}

export default PostToggles
