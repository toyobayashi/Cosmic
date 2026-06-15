import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, DatePicker, Select, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import api from '../api/client'

export default function Register() {
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const onFinish = async (values: {
    username: string
    password: string
    confirmPassword: string
    birthday: Dayjs
    gender: number
  }) => {
    if (values.password !== values.confirmPassword) {
      message.error('Passwords do not match')
      return
    }

    setLoading(true)
    try {
      const res = await api.post('/account/v1', {
        data: {
          name: values.username,
          password: values.password,
          birthday: values.birthday?.format('YYYY-MM-DD'),
          gender: values.gender,
        },
      })
      if (res.data.code === 200) {
        message.success('Registration successful')
        form.resetFields()
      } else {
        message.error(res.data.message || 'Registration failed')
      }
    } catch {
      message.error('Registration failed, please try again')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Card className="w-96 shadow-2xl" title="Create Account">
        <Form form={form} name="register" onFinish={onFinish} size="large" layout="vertical">
          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Please enter username' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="Username" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[
              { required: true, message: 'Please enter password' },
              { min: 6, message: 'Password must be at least 6 characters' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Passwords do not match'))
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Confirm Password" />
          </Form.Item>
          <Form.Item
            name="birthday"
            rules={[{ required: true, message: 'Please select birthday' }]}
          >
            <DatePicker
              placeholder="Birthday"
              className="w-full"
            />
          </Form.Item>
          <Form.Item
            name="gender"
            rules={[{ required: true, message: 'Please select gender' }]}
          >
            <Select placeholder="Gender">
              <Select.Option value={0}>Male</Select.Option>
              <Select.Option value={1}>Female</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              Register
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
