import type { ThemeConfig } from 'antd';

export const adminTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1677ff',
    colorInfo: '#1677ff',
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorText: '#172033',
    colorTextSecondary: '#5b6578',
    colorTextDescription: '#7b8597',
    colorBgBase: '#f4f7fb',
    colorBgLayout: '#f4f7fb',
    colorBgContainer: '#ffffff',
    colorBorderSecondary: '#e8edf5',
    borderRadius: 16,
    borderRadiusLG: 20,
    borderRadiusSM: 12,
    boxShadowSecondary: '0 18px 48px rgba(15, 23, 42, 0.08)',
    fontFamily: '"DIN Alternate", "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  },
  components: {
    Layout: {
      headerBg: 'rgba(255, 255, 255, 0.92)',
      siderBg: '#0f172a',
      bodyBg: 'transparent',
      triggerBg: '#0f172a',
    },
    Card: {
      headerBg: 'transparent',
      boxShadowTertiary: '0 16px 40px rgba(15, 23, 42, 0.06)',
    },
    Menu: {
      darkItemBg: '#0f172a',
      darkSubMenuItemBg: '#0f172a',
      darkItemSelectedBg: 'rgba(22, 119, 255, 0.22)',
      darkItemSelectedColor: '#ffffff',
      darkItemHoverColor: '#ffffff',
      itemBorderRadius: 14,
      itemHeight: 44,
    },
    Button: {
      controlHeight: 40,
      borderRadius: 12,
      fontWeight: 600,
    },
    Input: {
      controlHeight: 40,
    },
    Select: {
      controlHeight: 40,
    },
    Table: {
      headerBg: '#f7f9fc',
      headerColor: '#455166',
      rowHoverBg: '#f7fbff',
      borderColor: '#e8edf5',
    },
  },
};
