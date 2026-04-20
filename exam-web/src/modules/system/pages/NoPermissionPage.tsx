import styles from './NoPermissionPage.module.css';

export function NoPermissionPage() {
  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <span className={styles.badge}>403</span>
        <h1 className={styles.title}>暂无权限</h1>
        <p className={styles.description}>当前账号没有访问该页面的权限。你可以返回可见菜单，或联系管理员补充对应角色权限。</p>
      </div>
    </section>
  );
}
