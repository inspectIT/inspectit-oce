import React from 'react'
import SettingsElement from '../SettingsElement'
import {Password} from 'primereact/password'
import { Button } from 'primereact/button'
import { connect } from 'react-redux'
import {settingsActions} from '../../../../redux/ducks/settings'
import { notificationActions } from '../../../../redux/ducks/notification'
import data from '../../../../data/settings-configuration.json'

class PasswordChange extends React.Component{
  constructor(props){
    super(props)
    this.state = {
      minPwLength: data.minPwLength,
      textRowOne: '',
      textRowTwo: '',
      textRowThree: ''
    }
  }

  changePassword = () => {
    const {oldPassword, newPassword, newPasswordSecond, minPwLength} = this.state
    const {loading, username} = this.props

    if(loading){
      return
    }
    if(!oldPassword || !newPassword || !newPasswordSecond) {
      const textRowOne = oldPassword ? '' : 'Please enter your old password'
      const textRowTwo = newPassword ? '' : 'Please enter a new password'
      const textRowThree = newPasswordSecond ? '' : 'Please enter your new password again'
      this.setState({textRowOne, textRowTwo, textRowThree})
      return
    }
    if(newPassword.length < minPwLength) {
      const textRowOne = ''
      const textRowTwo = `Your new password must have at least ${minPwLength} characters`
      const textRowThree = ''
      this.setState({textRowOne, textRowTwo, textRowThree})
      return
    }
    if(newPassword === oldPassword){
      const textRowOne = ''
      const textRowTwo = 'Your new password must differ from your old password'
      const textRowThree = ''
      this.setState({textRowOne, textRowTwo, textRowThree})
      return
    }
    if(newPasswordSecond != newPassword) {
      const textRowOne = ''
      const textRowTwo = ''
      const textRowThree = 'Your confirmation password does not match your new password'
      this.setState({textRowOne, textRowTwo, textRowThree})
      return
    }

    const textRowOne = ''
    const textRowTwo = ''
    const textRowThree = ''
  
    this.props.changePassword(username, oldPassword, newPassword)
    this.setState({textRowOne, textRowTwo, textRowThree, oldPassword: '', newPassword: '', newPasswordSecond: ''})
  }

  render() {
    const {textRowOne, textRowTwo, textRowThree, minPwLength} = this.state

    return(
      <div>
        <style jsx>{`
          .p-dir-col{
            // width: 50%;
            margin: auto;
          }
          .p-col{
            padding-top: 0;
            padding-botom: 0;
          }
          .errorMessage{
            font-size: 0.8rem;
            color: red;
            padding-left: 6rem;
          }
        `}</style>
        <SettingsElement title='Change Password' btnLabel='Change' btnOnClick={this.changePassword} line>
          <div className='content'>
            <div className="p-grid p-dir-col">
              <div className="p-col">
                <div className='p-grid p-align-center'>
                  <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>Current password</div>
                  <div className='p-col-6'>
                    <Password feedback={false} value={this.state.oldPassword} onChange={(e) => this.setState({oldPassword: e.target.value})} />
                  </div>
                  <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                    {textRowOne !== '' ? textRowOne : ''}
                  </div>
                </div>
              </div>
              <div className="p-col">
                <div className='p-grid p-align-center'>
                  <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>New password</div>
                  <div className='p-col-6'>
                    <Password feedback={false} tooltip={`Your password needs to have at least ${minPwLength} characters.`} value={this.state.newPassword} onChange={(e) => this.setState({newPassword: e.target.value})} />
                  </div>
                  <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                    {textRowTwo !== '' ? textRowTwo : ''}
                  </div>
                </div>
              </div>
              <div className="p-col">
                <div className='p-grid p-align-center'>
                  <div className='p-col-6 p-md-4 p-md-offset-2 p-lg-3 p-lg-offset-3 p-xl-2 p-xl-offset-4'>Confirm password</div>
                  <div className='p-col-6'>
                    <Password feedback={false} value={this.state.newPasswordSecond} onChange={(e) => this.setState({newPasswordSecond: e.target.value})} />
                  </div>
                  <div className='p-col-12 p-md-10 p-md-offset-2 p-lg-8 p-lg-offset-4 errorMessage'>
                    {textRowThree !== '' ? textRowThree : ''}
                  </div>
                  <div className='p-col p-offset-9 p-lg-offset-8 p-xl-offset-7'><Button label='Change' onClick={this.changePassword}/></div>
                </div>
              </div>
            </div>
          </div>
        </SettingsElement>

      </div>
        
    )
  }
}

function mapStateToProps(state) {
  const { loading, error, success} = state.settings
  const {username} = state.authentication
  return {
    loading,
    username
  }
}

const mapDispatchToProps = {
  changePassword: settingsActions.changePassword,
}

export default connect(mapStateToProps, mapDispatchToProps)(PasswordChange)