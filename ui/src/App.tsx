import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Home from './pages/Home'
import Register from './pages/Register'
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
import LogsViewer from './pages/LogsViewer'
import MapQuery from './pages/MapQuery'
import AppLayout from './components/Layout'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  return token ? <>{children}</> : <Navigate to="/admin/login" replace />
}

export default function App() {
  const { token } = useAuth()

  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/register" element={<Register />} />
      <Route path="/admin/login" element={token ? <Navigate to="/admin" replace /> : <Login />} />
      <Route
        path="/admin"
        element={
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="accounts" element={<Accounts />} />
        <Route path="players" element={<Players />} />
        <Route path="characters" element={<Characters />} />
        <Route path="npcs-shop" element={<NpcShop />} />
        <Route path="monster-drop" element={<MonsterDrop />} />
        <Route path="global-drop" element={<GlobalDrop />} />
        <Route path="inventory" element={<Inventory />} />
        <Route path="config" element={<Config />} />
        <Route path="commands" element={<Commands />} />
        <Route path="logs" element={<LogsViewer />} />
        <Route path="map-query" element={<MapQuery />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
