import styles from './PostFooter.module.css'
import EyeIcon from '../Icons/EyeIcon'
import CommentIcon from '../Icons/CommentIcon'
import ChevronIcon from '../Icons/ChevronIcon'
import MoreIcon from '../Icons/MoreIcon'
import { formatCount } from '../../utils/formatCount'

function PostFooter({ stats, time, onReact, onOpenComments, onManage }) {
    const reactions = stats?.allowReactions ? stats.reactions : []

    return (
        <div className={styles.footer}>
            <div className={styles.row}>
                <div className={styles.chips}>
                    {reactions.map(({ emoji, count }) => (
                        <button
                            key={emoji}
                            className={stats.myReaction === emoji ? `${styles.chip} ${styles.chipMine}` : styles.chip}
                            type="button"
                            aria-pressed={stats.myReaction === emoji}
                            aria-label={`${emoji} ${count}`}
                            onClick={() => onReact(emoji)}
                        >
                            <span className={styles.emoji}>{emoji}</span>
                            {formatCount(count)}
                        </button>
                    ))}
                </div>
                <span className={styles.meta}>
                    {stats && (
                        <span className={styles.views} aria-label={`${stats.views} views`}>
                            <EyeIcon />
                            {formatCount(stats.views)}
                        </span>
                    )}
                    <span>{time}</span>
                </span>
            </div>
            {stats && onManage && (
                <div className={styles.settings}>
                    <span className={stats.allowComments ? styles.settingOn : styles.settingOff}>Comments {stats.allowComments ? 'on' : 'off'}</span>
                    <span className={styles.dot} aria-hidden="true" />
                    <span className={stats.allowReactions ? styles.settingOn : styles.settingOff}>Reactions {stats.allowReactions ? 'on' : 'off'}</span>
                    <button className={styles.more} type="button" aria-label="Post options" onClick={onManage}>
                        <MoreIcon />
                    </button>
                </div>
            )}
            {stats?.allowComments && onOpenComments && (
                <button className={styles.comments} type="button" onClick={onOpenComments}>
                    <CommentIcon />
                    <span className={styles.commentsText}>
                        {stats.comments === 0 ? 'Leave a comment' : `${formatCount(stats.comments)} ${stats.comments === 1 ? 'comment' : 'comments'}`}
                    </span>
                    <ChevronIcon />
                </button>
            )}
        </div>
    )
}

export default PostFooter
