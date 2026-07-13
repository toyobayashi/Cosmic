import { useNavigate } from 'react-router-dom'
import { Button } from 'antd'
import { UserAddOutlined } from '@ant-design/icons'

export default function Home() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col items-center justify-center">
      <div className="text-center">
        <h1 className="text-5xl font-bold text-gray-800 mb-4">Yozora</h1>
        <p className="text-lg text-gray-500 mb-8">Welcome to the Yozora</p>
        <Button
          type="primary"
          size="large"
          icon={<UserAddOutlined />}
          onClick={() => navigate('/register')}
        >
          Register Now
        </Button>
      </div>
    </div>
  )
}
