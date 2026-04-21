import type { ThemeConfig } from 'antd';

export const adminTheme: ThemeConfig = {
  token: {
    colorPrimary: '#2f8cff',
    colorInfo: '#2f8cff',
    colorSuccess: '#2fa36b',
    colorWarning: '#d98b2b',
    colorError: '#dc2626',
    colorText: '#1f2d3d',
    colorTextSecondary: '#6f7f92',
    colorTextDescription: '#93a1b2',
    colorBgBase: '#f3f6fa',
    colorBgLayout: '#f3f6fa',
    colorBgContainer: '#ffffff',
    colorBorder: '#dfe7f1',
    colorBorderSecondary: '#e7edf4',
    borderRadius: 12,
    borderRadiusLG: 16,
    borderRadiusSM: 8,
    boxShadowSecondary: '0 16px 36px rgba(31, 45, 61, 0.06)',
    fontFamily: '"Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  },
  components: {
    Layout: {
      headerBg: 'transparent',
      siderBg: '#f7fafd',
      bodyBg: 'transparent',
      triggerBg: '#f7fafd',
    },
    Card: {
      headerBg: 'transparent',
      boxShadowTertiary: '0 10px 24px rgba(31, 45, 61, 0.04)',
    },
    Menu: {
      itemBg: 'transparent',
      itemColor: '#526173',
      itemHoverColor: '#2f8cff',
      itemHoverBg: 'rgba(47, 140, 255, 0.08)',
      itemSelectedBg: '#e8f2ff',
      itemSelectedColor: '#2f8cff',
      itemBorderRadius: 10,
      itemHeight: 42,
    },
    Button: {
      controlHeight: 40,
      borderRadius: 8,
      fontWeight: 600,
    },
    Input: {
      controlHeight: 40,
      borderRadius: 8,
    },
    Select: {
      controlHeight: 40,
      borderRadius: 8,
    },
    Table: {
      headerBg: '#f7fafd',
      headerColor: '#5a6777',
      rowHoverBg: '#f7fbff',
      borderColor: '#e7edf4',
    },
  },
};
