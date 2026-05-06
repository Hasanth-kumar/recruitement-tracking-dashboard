import React, { useState } from 'react';
import { Layout, Button, Dropdown, Avatar, Space } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  FileTextOutlined,
  MessageOutlined,
  BarChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Role } from '../../constants/roles';
import '../../App.css';

const { Sider, Content, Header } = Layout;

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, role, hasRole } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = () => {
    ['rts_token', 'rts_role', 'rts_user', 'rts_basic_principal'].forEach(k => {
      localStorage.removeItem(k);
      sessionStorage.removeItem(k);
    });
    window.location.href = '/login';
  };

  const isActive = (path: string) => location.pathname === path;

  const userMenuItems = [
    {
      key: 'profile',
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    ...(hasRole(Role.ADMIN) ? [
      {
        key: 'users',
        label: 'Manage Users',
        onClick: () => navigate('/admin/users'),
      },
    ] : []),
    {
      type: 'divider' as const,
      key: 'divider',
    },
    {
      key: 'logout',
      label: 'Logout',
      icon: <LogoutOutlined />,
      onClick: handleLogout,
      danger: true,
    },
  ];

  const initials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : 'U';

  return (
    <Layout className="app-layout">
      {/* Sidebar */}
      <Sider
        className="app-layout-sider"
        collapsible
        trigger={null}
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={240}
        theme="light"
      >
        <div className="app-layout-logo">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="app-layout-toggle app-layout-toggle--sider"
            aria-label="Toggle sidebar"
          />
          {!collapsed && <div className="app-layout-logo-mark">RTS</div>}
        </div>

        <div className="app-layout-menu">
          <div
            className={`app-layout-menu-item ${isActive('/dashboard') ? 'active' : ''}`}
            onClick={() => navigate('/dashboard')}
          >
            <DashboardOutlined className="app-layout-menu-icon" />
            {!collapsed && <span>Dashboard</span>}
          </div>

          {hasRole(Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER) && (
            <div
              className={`app-layout-menu-item ${isActive('/candidates') ? 'active' : ''}`}
              onClick={() => navigate('/candidates')}
            >
              <UserOutlined className="app-layout-menu-icon" />
              {!collapsed && <span>Candidates</span>}
            </div>
          )}

          {hasRole(Role.ADMIN, Role.HR_MANAGER, Role.INTERVIEWER) && (
            <div
              className={`app-layout-menu-item ${isActive('/interviews') ? 'active' : ''}`}
              onClick={() => navigate('/interviews')}
            >
              <FileTextOutlined className="app-layout-menu-icon" />
              {!collapsed && <span>Interviews</span>}
            </div>
          )}

          {hasRole(Role.ADMIN, Role.HR_MANAGER) && (
            <div
              className={`app-layout-menu-item ${isActive('/feedback') ? 'active' : ''}`}
              onClick={() => navigate('/feedback')}
            >
              <MessageOutlined className="app-layout-menu-icon" />
              {!collapsed && <span>Feedback</span>}
            </div>
          )}

          {hasRole(Role.ADMIN, Role.HR_MANAGER) && (
            <div
              className={`app-layout-menu-item ${isActive('/reports') ? 'active' : ''}`}
              onClick={() => navigate('/reports')}
            >
              <BarChartOutlined className="app-layout-menu-icon" />
              {!collapsed && <span>Reports</span>}
            </div>
          )}
        </div>
      </Sider>

      {/* Main Layout */}
      <Layout className="app-layout-main">
        {/* Header */}
        <Header className="app-layout-header">
          <div className="app-layout-header-spacer" />

          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space className="app-layout-user-menu" style={{ cursor: 'pointer' }}>
              <Avatar className="app-layout-avatar">{initials}</Avatar>
              {!collapsed && (
                <div className="app-layout-user-info">
                  <div className="app-layout-user-name">{user?.username ?? 'User'}</div>
                  <div className="app-layout-user-role">{role ?? 'Unknown'}</div>
                </div>
              )}
            </Space>
          </Dropdown>
        </Header>

        {/* Content */}
        <Content className="app-layout-content">
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;
