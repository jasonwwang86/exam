import type { PropsWithChildren, ReactNode } from 'react';
import { Card, Space, Typography } from 'antd';
import styles from './AdminPage.module.css';

type AdminPageHeaderProps = {
  title: string;
  description: string;
  extra?: ReactNode;
};

type AdminPageSectionProps = PropsWithChildren<{
  title: string;
  description?: string;
  extra?: ReactNode;
}>;

export function AdminPage({ children }: PropsWithChildren) {
  return <div className={styles.page}>{children}</div>;
}

export function AdminPageHeader({ title, description, extra }: AdminPageHeaderProps) {
  return (
    <Card variant="borderless" className={styles.hero}>
      <div className={styles.headerRow}>
        <Space direction="vertical" size={4}>
          <Typography.Text className={styles.kicker}>Admin Console</Typography.Text>
          <Typography.Title level={2} className={styles.title}>
            {title}
          </Typography.Title>
          <Typography.Paragraph className={styles.description}>{description}</Typography.Paragraph>
        </Space>
        {extra ? <div className={styles.headerExtra}>{extra}</div> : null}
      </div>
    </Card>
  );
}

export function AdminPageSection({ title, description, extra, children }: AdminPageSectionProps) {
  return (
    <Card
      variant="borderless"
      className={styles.section}
      title={
        <Space direction="vertical" size={2}>
          <Typography.Title level={4} className={styles.sectionTitle}>
            {title}
          </Typography.Title>
          {description ? <Typography.Text className={styles.sectionDescription}>{description}</Typography.Text> : null}
        </Space>
      }
      extra={extra}
    >
      {children}
    </Card>
  );
}
