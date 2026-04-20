import styles from './DashboardPage.module.css';

export function DashboardPage() {
  return (
    <section className={styles.page}>
      <div className={styles.hero}>
        <h1 className={styles.title}>管理首页</h1>
        <p className={styles.description}>当前模块已经具备登录、鉴权、权限路由和基础日志链路能力，后续业务模块可以在这套基础之上继续扩展。</p>
      </div>
      <div className={styles.grid}>
        <article className={styles.card}>
          <h2 className={styles.cardTitle}>登录与鉴权</h2>
          <p className={styles.cardText}>已支持管理员登录、会话恢复、退出登录以及未登录路由保护。</p>
        </article>
        <article className={styles.card}>
          <h2 className={styles.cardTitle}>角色与权限</h2>
          <p className={styles.cardText}>基础菜单权限和接口权限已打通，可继续承接后续模块的授权控制。</p>
        </article>
      </div>
    </section>
  );
}
