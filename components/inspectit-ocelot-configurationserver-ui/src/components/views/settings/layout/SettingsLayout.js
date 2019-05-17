import React from 'react'
import SettingsMenu from './SettingsMenu'

const SettingsLayout = (props) => {
  return (
    <div>
      <style jsx>{`
        .content {
          // margin for side navigation
          // margin-left: 6rem;
          // margin for tab menu
          margin-top: 2.5rem;
          overflow: auto auto;
        }
      `}</style>
      <SettingsMenu />
      <div className='content'>
        {props.children}
      </div>
    </div>
  )
}

export default SettingsLayout