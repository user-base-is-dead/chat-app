import styles from './GroupsPage.module.css'
import GroupsIcon from '../../components/Icons/GroupsIcon'

function GroupsPage() {
    return (
        <div className={styles.page}>
            <span className={styles.icon}><GroupsIcon /></span>
            <p className={styles.text}>No groups yet</p>
        </div>
    )
}

export default GroupsPage
