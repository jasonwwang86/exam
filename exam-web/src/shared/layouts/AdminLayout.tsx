import { Avatar, Button, Empty, Layout, Menu, Space, Typography } from 'antd';
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
  const selectedMenuKey =
    currentUser.menus.find((menu) => location.pathname === menu.path || location.pathname.startsWith(`${menu.path}/`))?.path ?? '';

  return (
    <Layout className={styles.shell}>
      <Layout.Sider breakpoint="lg" collapsedWidth="0" width={248} className={styles.sider}>
        <div className={styles.brand}>
          <div className={styles.brandMark}>E</div>
          <div>
            <Typography.Text className={styles.brandEyebrow}>Exam Admin</Typography.Text>
            <Typography.Title level={4} className={styles.brandTitle}>
              管理工作台
            </Typography.Title>
          </div>
        </div>
        <Typography.Text className={styles.sidebarLabel}>业务导航</Typography.Text>
        {currentUser.menus.length ? (
          <Menu
            mode="inline"
            theme="dark"
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
      </Layout.Sider>

      <Layout className={styles.workspace}>
        <Layout.Header className={styles.header}>
          <div>
            <Typography.Text className={styles.headerKicker}>统一主页面</Typography.Text>
            <Typography.Title level={3} className={styles.headerTitle}>
              企业管理台
            </Typography.Title>
          </div>
          <Space size={16} className={styles.headerActions}>
            <div className={styles.userCard}>
              <Avatar size={40} className={styles.avatar}>
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
          </Space>
        </Layout.Header>
        <Layout.Content className={styles.content}>
          <div className={styles.contentInner}>{children}</div>
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
