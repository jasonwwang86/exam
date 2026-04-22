import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

describe('admin pages', () => {
  it('renders standalone login page component', async () => {
    const { LoginPage } = await import('../../modules/auth/pages/LoginPage');

    render(
      <LoginPage
        form={{ username: '', password: '' }}
        submitting={false}
        errorMessage=""
        onUsernameChange={vi.fn()}
        onPasswordChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(screen.getByText('系统登录入口')).toBeInTheDocument();
    expect(screen.getByText('输入管理员账号与密码后进入统一工作区，当前登录流程、鉴权逻辑与权限体系保持不变。')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '登录' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: '用户名' })).toHaveAttribute('autocomplete', 'username');
    expect(screen.getByLabelText('密码')).toHaveAttribute('autocomplete', 'current-password');
    expect(screen.queryByText('业务导航')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '退出登录' })).not.toBeInTheDocument();
  });

  it('renders standalone dashboard page component', async () => {
    const { DashboardPage } = await import('../../modules/dashboard/pages/DashboardPage');

    render(<DashboardPage />);

    expect(screen.getByRole('heading', { name: '管理首页' })).toBeInTheDocument();
    expect(screen.queryByText('当前模块已经具备登录、鉴权、权限路由和基础日志链路能力，后续业务模块可以在这套基础之上继续扩展。')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '系统概览' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '登录与鉴权' })).toBeInTheDocument();
  });

  it('renders standalone no-permission page component', async () => {
    const { NoPermissionPage } = await import('../../modules/system/pages/NoPermissionPage');

    render(<NoPermissionPage />);

    expect(screen.getByRole('heading', { name: '暂无权限' })).toBeInTheDocument();
    expect(
      screen.getByText('当前账号没有访问该页面的权限。你可以返回可见菜单，或联系管理员补充对应角色权限。'),
    ).toBeInTheDocument();
  });
});
