import styles from './ChannelsPage.module.css'
import ChannelsIcon from '../../components/Icons/ChannelsIcon'

function ChannelsPage() {
    return (
        <div className={styles.page}>
            <span className={styles.icon}><ChannelsIcon /></span>
            <p className={styles.text}>No channels yet</p>
        </div>
    )
}

export default ChannelsPage
