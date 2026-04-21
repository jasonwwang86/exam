import { Typography } from 'antd';
import { AdminPage, AdminPageHeader, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import styles from './DashboardPage.module.css';

const overviewMetrics = [
  { label: '考试人数', value: '514', detail: '年：514 / 总：514' },
  { label: '及格人数', value: '514', detail: '年：514 / 总：514' },
  { label: '不及格人数', value: '0', detail: '年：0 / 总：0' },
  { label: '及格率', value: '100%', detail: '年：100% / 总：100%' },
];

const quickActions = ['考生管理', '准考证', '考试计划', '考试记录', '试卷管理', '题目管理', '员工管理'];

const capabilityCards = [
  {
    title: '登录与鉴权',
    text: '已支持管理员登录、会话恢复、退出登录以及未登录路由保护。',
  },
  {
    title: '角色与权限',
    text: '基础菜单权限和接口权限已打通，可继续承接后续模块的授权控制。',
  },
  {
    title: '考生管理',
    text: '统一主页面已预留业务模块入口，考生管理模块可在该框架下接入查询、维护与批量处理流程。',
  },
];

const monthlyTrend = [12, 18, 22, 24, 28, 32, 40, 44, 46, 52, 58, 100];
const yearlyTrend = [34, 46, 54, 60, 68, 72, 80, 84, 88, 92, 96, 100];

export function DashboardPage() {
  return (
    <AdminPage>
      <AdminPageHeader title="管理首页" description="查看考试业务概览、常用功能入口和近期趋势，作为统一工作台的默认落点。" />

      <div className={styles.topGrid}>
        <AdminPageSection title="个人信息" description="当前登录账号的基础摘要">
          <div className={styles.profilePanel}>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>姓名</span>
              <span className={styles.profileValue}>admin</span>
            </div>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>部门</span>
              <span className={styles.profileValue}>总经办</span>
            </div>
            <div className={styles.profileRow}>
              <span className={styles.profileLabel}>岗位</span>
              <span className={styles.profileValue}>技术支持</span>
            </div>
          </div>
        </AdminPageSection>

        <AdminPageSection title="本月数据" description="当前考试业务核心概览">
          <div className={styles.metricsGrid}>
            {overviewMetrics.map((metric) => (
              <article key={metric.label} className={styles.metricCard}>
                <Typography.Text className={styles.metricLabel}>{metric.label}</Typography.Text>
                <Typography.Title level={2} className={styles.metricValue}>
                  {metric.value}
                </Typography.Title>
                <Typography.Text className={styles.metricDetail}>{metric.detail}</Typography.Text>
              </article>
            ))}
          </div>
        </AdminPageSection>
      </div>

      <AdminPageSection title="常用功能" description="高频入口统一放在工作台，便于快速进入考试业务流程">
        <div className={styles.quickActionGrid}>
          {quickActions.map((item) => (
            <div key={item} className={styles.quickActionCard}>
              <span className={styles.quickActionIcon} aria-hidden="true" />
              <Typography.Text className={styles.quickActionLabel}>{item}</Typography.Text>
            </div>
          ))}
        </div>
      </AdminPageSection>

      <div className={styles.chartGrid}>
        <AdminPageSection title="考试趋势图（月）" description="查看月度考试人数与通过趋势">
          <div className={styles.chartPanel}>
            <div className={styles.chartBars}>
              {monthlyTrend.map((value, index) => (
                <div key={`month-${index + 1}`} className={styles.barGroup}>
                  <span className={styles.barTrack}>
                    <span className={styles.barFill} style={{ height: `${value}%` }} />
                  </span>
                  <span className={styles.barLabel}>{index + 1}月</span>
                </div>
              ))}
            </div>
          </div>
        </AdminPageSection>

        <AdminPageSection title="考试趋势图（年）" description="查看年度通过率与人数变化">
          <div className={styles.chartPanel}>
            <div className={styles.chartBars}>
              {yearlyTrend.map((value, index) => (
                <div key={`year-${index + 1}`} className={styles.barGroup}>
                  <span className={styles.barTrack}>
                    <span className={styles.barFillAlt} style={{ height: `${value}%` }} />
                  </span>
                  <span className={styles.barLabel}>{index + 1}月</span>
                </div>
              ))}
            </div>
          </div>
        </AdminPageSection>
      </div>

      <AdminPageSection title="系统概览" description="当前管理端已具备的通用能力">
        <div className={styles.capabilityGrid}>
          {capabilityCards.map((item) => (
            <article key={item.title} className={styles.capabilityCard}>
              <Typography.Title level={5} className={styles.cardTitle}>
                {item.title}
              </Typography.Title>
              <Typography.Paragraph className={styles.cardText}>{item.text}</Typography.Paragraph>
            </article>
          ))}
        </div>
      </AdminPageSection>
    </AdminPage>
  );
}
