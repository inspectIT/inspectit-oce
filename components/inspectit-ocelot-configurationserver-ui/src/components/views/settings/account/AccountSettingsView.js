import React from 'react'
import SettingsElement from '../SettingsElement'
import {InputText} from 'primereact/inputtext'
import {Password} from 'primereact/password'
import PasswordChange from './PasswordChange';

class AccountSettingsView extends React.Component {
  constructor(props) {
    super(props)
    this.state={
      oldPassword: '',
      newPassword: '',
      newPasswordSecond: ''
    }
  }

  render() {
    return (
      <div className='this'>
        <style jsx>{`
        `}</style>
        <PasswordChange />
      </div>
    )
  }
}

export default AccountSettingsView