import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Players from './pages/Players'
import Characters from './pages/Characters'
import NpcShop from './pages/NpcShop'
import MonsterDrop from './pages/MonsterDrop'
import GlobalDrop from './pages/GlobalDrop'
import Inventory from './pages/Inventory'
import Config from './pages/Config'
import Commands from './pages/Commands'
import AppLayout from './components/Layout'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  const { token } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={token ? <Navigate to="/" replace /> : <Login />} />
      <Route
        element={
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/players" element={<Players />} />
        <Route path="/characters" element={<Characters />} />
        <Route path="/npcs-shop" element={<NpcShop />} />
        <Route path="/monster-drop" element={<MonsterDrop />} />
        <Route path="/global-drop" element={<GlobalDrop />} />
        <Route path="/inventory" element={<Inventory />} />
        <Route path="/config" element={<Config />} />
        <Route path="/commands" element={<Commands />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
