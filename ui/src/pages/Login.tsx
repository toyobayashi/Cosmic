import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'
import api from '../api/client'

export default function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login } = useAuth()

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const res = await api.post('/auth/login', { data: values })
      if (res.data.code === 200) {
        login(res.data.data.token, values.username)
        message.success('Login successful')
        navigate('/admin')
      } else {
        message.error(res.data.message || 'Login failed')
      }
    } catch {
      message.error('Login failed, please check credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex items-center justify-center h-screen bg-gray-50">
      <Card className="w-96 shadow-2xl" title="Admin Login">
        <div className="text-center mb-4">
          <Button type="link" onClick={() => navigate('/register')}>
            No account? Register here
          </Button>
        </div>
        <Form name="login" onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: 'Please enter username' }]}>
            <Input prefix={<UserOutlined />} placeholder="Username" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Please enter password' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              Login
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
