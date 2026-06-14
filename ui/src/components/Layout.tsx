import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, theme  } from 'antd'
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  GiftOutlined,
  ShopOutlined,
  BugOutlined,
  GlobalOutlined,
  FileTextOutlined,
  SettingOutlined,
  CodeOutlined,
  ReadOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'

const { Header, Sider, Content } = Layout

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { token: _, username, logout } = useAuth()
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  const menuItems = [
    { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/accounts', icon: <UserOutlined />, label: 'Accounts' },
    { key: '/players', icon: <GiftOutlined />, label: 'Players' },
    { key: '/characters', icon: <TeamOutlined />, label: 'Characters' },
    { key: '/npcs-shop', icon: <ShopOutlined />, label: 'NPC Shop' },
    { key: '/monster-drop', icon: <BugOutlined />, label: 'Monster Drop' },
    { key: '/global-drop', icon: <GlobalOutlined />, label: 'Global Drop' },
    { key: '/inventory', icon: <FileTextOutlined />, label: 'Inventory' },
    { key: '/config', icon: <SettingOutlined />, label: 'Config' },
    { key: '/commands', icon: <CodeOutlined />, label: 'Commands' },
    { key: '/logs', icon: <ReadOutlined />, label: 'Server Logs' },
    { key: '/map-query', icon: <EnvironmentOutlined />, label: 'Map Query' },
  ]

  const currentKey = location.pathname === '/' ? '/' : '/' + location.pathname.split('/')[1]

  return (
    <Layout className="h-screen">
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div className="h-16 flex items-center justify-center font-bold text-lg" style={{ color: '#1677ff' }}>
          {collapsed ? 'CM' : 'Cosmic Admin'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[currentKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          theme="dark"
        />
      </Sider>
      <Layout>
        <Header className="flex items-center justify-between" style={{ padding: '0 16px', background: colorBgContainer }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="text-lg"
          />
          <div className="flex items-center gap-4">
            <span>{username}</span>
            <Button
              type="text"
              icon={<LogoutOutlined />}
              onClick={() => {
                logout()
                navigate('/login')
              }}
              danger
            >
              Logout
            </Button>
          </div>
        </Header>
        <Content className="m-6 p-6 overflow-auto" style={{ background: '#fff', borderRadius: 8, minHeight: 0 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
