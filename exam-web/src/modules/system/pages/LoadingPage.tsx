import styles from './LoadingPage.module.css';

export function LoadingPage() {
  return (
    <main className={styles.page}>
      <section className={styles.card} aria-label="加载中">
        <span className={styles.dot} aria-hidden="true" />
        <p className={styles.text}>正在恢复管理端会话...</p>
      </section>
    </main>
  );
}
