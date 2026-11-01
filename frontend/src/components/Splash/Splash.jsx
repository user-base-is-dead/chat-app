import styles from './Splash.module.css'
import logo from '../../assets/logo.png'
import Spinner from '../Spinner/Spinner'

function Splash() {
    return (
        <div className={styles.splash}>
            <img className={styles.logo} src={logo} alt="Miron Chat" />
            <span className={styles.spinner}>
                <Spinner size={24} />
            </span>
        </div>
    )
}

export default Splash
