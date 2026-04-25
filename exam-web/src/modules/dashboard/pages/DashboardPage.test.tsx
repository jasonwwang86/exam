import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { CurrentUser } from '../../auth/types';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockFetchDashboardSummary } = vi.hoisted(() => ({
  mockFetchDashboardSummary: vi.fn(),
}));

vi.mock('../services/dashboardApi', () => ({
  fetchDashboardSummary: mockFetchDashboardSummary,
}));

const currentUser: CurrentUser = {
  userId: 1,
  username: 'admin',
  displayName: '系统管理员',
  roles: ['SUPER_ADMIN'],
  permissions: ['dashboard:view', 'dashboard:read'],
  menus: [
    { code: 'dashboard:view', name: '管理首页', path: '/dashboard' },
    { code: 'examinee:view', name: '考生管理', path: '/examinees' },
  ],
};

describe('DashboardPage', () => {
  beforeEach(() => {
    mockFetchDashboardSummary.mockReset();
  });

  it('renders current user info, live monthly summary, and authorized menu entries', async () => {
    mockFetchDashboardSummary.mockResolvedValue({
      monthlyNewExamineeCount: 3,
      monthlyNewQuestionCount: 3,
      monthlyNewPaperCount: 2,
      monthlyActiveExamPlanCount: 0,
    });

    const { DashboardPage } = await import('./DashboardPage');

    render(
      <MemoryRouter>
        <DashboardPage token="token-123" currentUser={currentUser} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('系统管理员')).toBeInTheDocument();
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('SUPER_ADMIN')).toBeInTheDocument();
    expect(screen.getByText('考生管理')).toBeInTheDocument();
    expect(screen.getByText('本月新增考生')).toBeInTheDocument();
    expect(screen.getByText('本月新增题目')).toBeInTheDocument();
    expect(screen.getByText('本月新增试卷')).toBeInTheDocument();
    expect(screen.getByText('本月开考计划')).toBeInTheDocument();
    expect(screen.getAllByText('3')).toHaveLength(2);
    expect(screen.getAllByText('2')).toHaveLength(2);
  });

  it('keeps profile and quick actions visible when summary loading fails', async () => {
    mockFetchDashboardSummary.mockRejectedValue(new Error('network error'));

    const { DashboardPage } = await import('./DashboardPage');

    render(
      <MemoryRouter>
        <DashboardPage token="token-123" currentUser={currentUser} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('系统管理员')).toBeInTheDocument();
    expect(screen.getByText('考生管理')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent('本月数据加载失败，请稍后重试');
  });
});
