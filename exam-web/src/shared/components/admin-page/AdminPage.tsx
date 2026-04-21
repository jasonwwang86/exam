import type { PropsWithChildren, ReactNode } from 'react';
import { Typography } from 'antd';
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
    <section className={styles.header}>
      <div className={styles.headerRow}>
        <div className={styles.headerContent}>
          <Typography.Text className={styles.kicker}>Work Area</Typography.Text>
          <Typography.Title level={2} className={styles.title}>
            {title}
          </Typography.Title>
          <Typography.Paragraph className={styles.description}>{description}</Typography.Paragraph>
        </div>
        {extra ? <div className={styles.headerExtra}>{extra}</div> : null}
      </div>
    </section>
  );
}

export function AdminPageSection({ title, description, extra, children }: AdminPageSectionProps) {
  return (
    <section className={styles.section}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionTitleGroup}>
          <Typography.Title level={4} className={styles.sectionTitle}>
            {title}
          </Typography.Title>
          {description ? <Typography.Text className={styles.sectionDescription}>{description}</Typography.Text> : null}
        </div>
        {extra ? <div className={styles.sectionExtra}>{extra}</div> : null}
      </div>
      <div className={styles.sectionBody}>{children}</div>
    </section>
  );
}
