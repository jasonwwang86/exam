import { Card, Col, Row, Space, Statistic, Typography } from 'antd';
import { AdminPage, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import styles from './DashboardPage.module.css';

export function DashboardPage() {
  return (
    <AdminPage>
      <AdminPageSection title="管理首页">
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Card variant="borderless" className={styles.metricCard}>
              <Statistic title="已接入模块" value={2} suffix="个" />
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card variant="borderless" className={styles.metricCard}>
              <Statistic title="统一权限链路" value="已打通" />
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card variant="borderless" className={styles.metricCard}>
              <Statistic title="当前风格状态" value="标准化中" />
            </Card>
          </Col>
        </Row>
      </AdminPageSection>

      <AdminPageSection title="系统概览">
        <div className={styles.grid}>
          <Card variant="borderless" className={styles.card}>
            <Space direction="vertical" size={8}>
              <Typography.Title level={5} className={styles.cardTitle}>
                登录与鉴权
              </Typography.Title>
              <Typography.Paragraph className={styles.cardText}>
                已支持管理员登录、会话恢复、退出登录以及未登录路由保护。
              </Typography.Paragraph>
            </Space>
          </Card>
          <Card variant="borderless" className={styles.card}>
            <Space direction="vertical" size={8}>
              <Typography.Title level={5} className={styles.cardTitle}>
                角色与权限
              </Typography.Title>
              <Typography.Paragraph className={styles.cardText}>
                基础菜单权限和接口权限已打通，可继续承接后续模块的授权控制。
              </Typography.Paragraph>
            </Space>
          </Card>
          <Card variant="borderless" className={styles.card}>
            <Space direction="vertical" size={8}>
              <Typography.Title level={5} className={styles.cardTitle}>
                考生管理
              </Typography.Title>
              <Typography.Paragraph className={styles.cardText}>
                统一主页面已预留业务模块入口，考生管理模块可在该框架下接入查询、维护与批量处理流程。
              </Typography.Paragraph>
            </Space>
          </Card>
        </div>
      </AdminPageSection>
    </AdminPage>
  );
}
