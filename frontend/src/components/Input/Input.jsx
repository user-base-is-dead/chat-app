import styles from './Input.module.css'

function Input({label, type = 'text', value, onChange, maxLength}){
    return(
        <div className={styles.field}>
            <input className={styles.input} type={type} value={value} onChange={onChange} maxLength={maxLength} placeholder=" "/>
            <label className={styles.label}>{label}</label>
        </div>
    )
}

export default Input
