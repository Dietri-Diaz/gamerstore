import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'

export default function AdminLayout() {
  // Sidebar colapsable; recordamos la preferencia en localStorage.
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('gs_sidebar') === '1')

  const toggle = () => {
    setCollapsed((c) => {
      const next = !c
      localStorage.setItem('gs_sidebar', next ? '1' : '0')
      return next
    })
  }

  return (
    <div className={'admin' + (collapsed ? ' collapsed' : '')}>
      <Sidebar onToggle={toggle} collapsed={collapsed} />
      <div className="admin-main">
        <Topbar />
        <div className="admin-content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
