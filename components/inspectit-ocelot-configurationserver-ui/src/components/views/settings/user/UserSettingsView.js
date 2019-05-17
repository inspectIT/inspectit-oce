import React from 'react'
import UserDataTable from './UserDataTable';
import UserToolbar from './UserSettingsToolbar'

class UserSettingsView extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  
  render() {
    return (
      <div className='this'>
        <style jsx>{`
        `}</style>
         <UserToolbar />
        <UserDataTable/>
      </div>
    )
  }

}

export default UserSettingsView