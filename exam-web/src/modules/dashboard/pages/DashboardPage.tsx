import { Alert, Skeleton, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { CurrentUser } from '../../auth/types';
import { fetchDashboardSummary } from '../services/dashboardApi';
import type { DashboardSummary } from '../types';
import { AdminPage, AdminPageHeader, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import styles from './DashboardPage.module.css';

type DashboardPageProps = {
  token: string;
  currentUser: CurrentUser;
};

const metricDefinitions: Array<{
  key: keyof DashboardSummary;
  label: string;
  detail: string;
}> = [
  { key: 'monthlyNewExamineeCount', label: '本月新增考生', detail: '按考生创建时间统计' },
  { key: 'monthlyNewQuestionCount', label: '本月新增题目', detail: '按题目录入时间统计' },
  { key: 'monthlyNewPaperCount', label: '本月新增试卷', detail: '按试卷创建时间统计' },
  { key: 'monthlyActiveExamPlanCount', label: '本月开考计划', detail: '按考试开始时间统计' },
];

export function DashboardPage({ token, currentUser }: DashboardPageProps) {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let active = true;

    async function loadSummary() {
      setLoading(true);
      setErrorMessage('');

      try {
        const response = await fetchDashboardSummary(token);
        if (!active) {
          return;
        }
        setSummary(response);
      } catch (error) {
        if (!active) {
          return;
        }
        setSummary(null);
        setErrorMessage('本月数据加载失败，请稍后重试');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadSummary();

    return () => {
      active = false;
    };
  }, [token]);

  const rolesText = useMemo(() => {
    return currentUser.roles.length ? currentUser.roles.join('、') : '未分配角色';
  }, [currentUser.roles]);

  return (
    <AdminPage>
      <AdminPageHeader title="管理首页" description="查看当前登录账号信息、本月关键数据和可直接进入的常用功能。" />

      <div className={styles.topGrid}>
        <AdminPageSection title="个人信息" description="当前登录账号的基础摘要">
          <div className={styles.profilePanel}>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>姓名</span>
              <span className={styles.profileValue}>{currentUser.displayName}</span>
            </div>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>账号</span>
              <span className={styles.profileValue}>{currentUser.username}</span>
            </div>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>角色</span>
              <span className={styles.profileValue}>{rolesText}</span>
            </div>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>模块数</span>
              <span className={styles.profileValue}>{currentUser.menus.length}</span>
            </div>
          </div>
        </AdminPageSection>

        <AdminPageSection title="本月数据" description="基于当前系统已接入模块的真实月度统计">
          {loading ? (
            <div className={styles.loadingBlock}>
              <Skeleton active paragraph={{ rows: 4 }} />
            </div>
          ) : errorMessage ? (
            <Alert showIcon type="error" role="alert" message={errorMessage} />
          ) : summary ? (
            <div className={styles.metricsGrid}>
              {metricDefinitions.map((metric) => (
                <article key={metric.key} className={styles.metricCard}>
                  <Typography.Text className={styles.metricLabel}>{metric.label}</Typography.Text>
                  <Typography.Title level={2} className={styles.metricValue}>
                    {summary[metric.key]}
                  </Typography.Title>
                  <Typography.Text className={styles.metricDetail}>{metric.detail}</Typography.Text>
                </article>
              ))}
            </div>
          ) : null}
        </AdminPageSection>
      </div>

      <AdminPageSection title="常用功能" description="当前账号已经开通且可直接进入的管理模块">
        <div className={styles.quickActionGrid}>
          {currentUser.menus.map((menu) => (
            <Link key={menu.path} className={styles.quickActionCard} to={menu.path}>
              <span className={styles.quickActionIcon} aria-hidden="true" />
              <Typography.Text className={styles.quickActionLabel}>{menu.name}</Typography.Text>
              <Typography.Text className={styles.quickActionHint}>{menu.path}</Typography.Text>
            </Link>
          ))}
        </div>
      </AdminPageSection>
    </AdminPage>
  );
}
