import styles from './CandidateLoadingPage.module.css';

export function CandidateLoadingPage() {
  return (
    <main className={styles.page}>
      <section className={styles.card} aria-label="加载中">
        <span className={styles.dot} aria-hidden="true" />
        <p className={styles.text}>正在恢复考生会话...</p>
      </section>
    </main>
  );
}
