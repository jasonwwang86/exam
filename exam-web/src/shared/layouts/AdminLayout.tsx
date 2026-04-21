import { Avatar, Button, Empty, Layout, Menu, Typography } from 'antd';
import type { PropsWithChildren } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import type { CurrentUser } from '../../modules/auth/types';
import styles from './AdminLayout.module.css';

type AdminLayoutProps = PropsWithChildren<{
  currentUser: CurrentUser;
  onLogout: () => void;
}>;

export function AdminLayout({ currentUser, onLogout, children }: AdminLayoutProps) {
  const location = useLocation();
  const activeMenu = currentUser.menus.find((menu) => location.pathname === menu.path || location.pathname.startsWith(`${menu.path}/`)) ?? null;
  const selectedMenuKey = activeMenu?.path ?? '';
  const workspaceTabs = activeMenu ? [activeMenu.name] : ['工作台'];

  return (
    <Layout className={styles.shell}>
      <Layout.Sider breakpoint="lg" collapsedWidth="0" width={236} className={styles.sider}>
        <div className={styles.siderInner}>
          <div className={styles.brand}>
            <div className={styles.brandMark}>E</div>
            <div>
              <Typography.Text className={styles.brandEyebrow}>Exam Platform</Typography.Text>
              <Typography.Title level={4} className={styles.brandTitle}>
                企业管理台
              </Typography.Title>
            </div>
          </div>

          <div className={styles.navBlock}>
            <Typography.Text className={styles.sidebarLabel}>功能导航</Typography.Text>
            {currentUser.menus.length ? (
              <Menu
                mode="inline"
                selectedKeys={selectedMenuKey ? [selectedMenuKey] : []}
                items={currentUser.menus.map((menu) => ({
                  key: menu.path,
                  label: (
                    <NavLink className={styles.menuLink} to={menu.path}>
                      {menu.name}
                    </NavLink>
                  ),
                }))}
              />
            ) : (
              <div className={styles.emptyMenu}>
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={<span className={styles.emptyMenuText}>当前账号暂无可见菜单，请联系管理员分配权限。</span>}
                />
              </div>
            )}
          </div>
        </div>
      </Layout.Sider>

      <Layout className={styles.workspace}>
        <Layout.Header className={styles.header}>
          <div className={styles.headerBar}>
            <div>
              <Typography.Text className={styles.headerKicker}>考试业务管理系统</Typography.Text>
              <Typography.Title level={3} className={styles.headerTitle}>
                企业管理台
              </Typography.Title>
            </div>
            <div className={styles.headerActions}>
              <div className={styles.userCard}>
                <Avatar size={38} className={styles.avatar}>
                  {currentUser.displayName.slice(0, 1).toUpperCase()}
                </Avatar>
                <div>
                  <Typography.Text className={styles.userName}>{currentUser.displayName}</Typography.Text>
                  <Typography.Text className={styles.userRole}>{currentUser.username}</Typography.Text>
                </div>
              </div>
              <Button type="primary" onClick={onLogout}>
                退出登录
              </Button>
            </div>
          </div>

          <div className={styles.workspaceTabs} aria-label="当前工作区">
            {workspaceTabs.map((tab, index) => (
              <span key={`${tab}-${index}`} className={index === workspaceTabs.length - 1 ? styles.workspaceTabActive : styles.workspaceTab}>
                {tab}
              </span>
            ))}
          </div>
        </Layout.Header>
        <Layout.Content className={styles.content}>
          <div className={styles.contentInner}>{children}</div>
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
