import { Link } from 'react-router-dom';
import type { PropsWithChildren } from 'react';
import type { CurrentUser } from '../../modules/auth/types';
import styles from './AdminLayout.module.css';

type AdminLayoutProps = PropsWithChildren<{
  currentUser: CurrentUser;
  onLogout: () => void;
}>;

export function AdminLayout({ currentUser, onLogout, children }: AdminLayoutProps) {
  return (
    <main className={styles.shell}>
      <div className={styles.frame}>
        <header className={styles.header}>
          <div className={styles.brand}>
            <p className={styles.eyebrow}>Exam Admin</p>
            <h1 className={styles.title}>管理端控制台</h1>
          </div>
          <div className={styles.toolbar}>
            <span className={styles.user}>{currentUser.displayName}</span>
            <button className={styles.logout} type="button" onClick={onLogout}>
              退出登录
            </button>
          </div>
        </header>
        <div className={styles.content}>
          <aside className={styles.sidebar}>
            <p className={styles.sidebarTitle}>导航菜单</p>
            {currentUser.menus.length ? (
              <nav className={styles.menu} aria-label="管理端菜单">
                {currentUser.menus.map((menu) => (
                  <Link key={menu.code} className={styles.menuLink} to={menu.path}>
                    {menu.name}
                  </Link>
                ))}
              </nav>
            ) : (
              <p className={styles.emptyMenu}>当前账号暂无可见菜单，请联系管理员分配权限。</p>
            )}
          </aside>
          <section className={styles.main}>{children}</section>
        </div>
      </div>
    </main>
  );
}
