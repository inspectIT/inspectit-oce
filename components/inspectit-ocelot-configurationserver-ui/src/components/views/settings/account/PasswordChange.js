import React from 'react'
import SettingsElement from '../SettingsElement'
import {Password} from 'primereact/password'
import { connect } from 'react-redux'
import {settingsActions, settingsSelectors} from '../../../../redux/ducks/settings'

import { Button } from 'primereact/button'

import {Growl} from 'primereact/growl'
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
    this.pendingRequest = false
  }

  changePassword = () => {
    const {oldPassword, newPassword, newPasswordSecond, minPwLength} = this.state
    const {loading, username} = this.props
    if(loading){
      return
    }
      
    // checking for all fields if anything has been entered at all
    if(!oldPassword || !newPassword || !newPasswordSecond) {
      const textRowOne = oldPassword ? '' : 'Please enter your old password'
      const textRowTwo = newPassword ? '' : 'Please enter a new password'
      const textRowThree = newPasswordSecond ? '' : 'Please enter your new password again'
      this.setState({textRowOne, textRowTwo, textRowThree})
      return
    }
    // checking if new password has the minimum length
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
    this.setState({textRowOne, textRowTwo, textRowThree})
  
    this.pendingRequest = true
    this.props.changePassword(username, oldPassword, newPassword)
  }

  passwordDidChange = () => {
    // this.pendingRequest = false
    // this.growl.show({severity: 'success', summary: 'Ok', detail: 'Your password has been changed'})
    this.setState({oldPassword: '', newPassword: '', newPasswordSecond: ''})
  }

  passwordDidNotChange = () => {
    // this.pendingRequest = false
    // this.growl.show({severity: 'error', summary: 'Error', detail: `Your password could not be changed: ${this.props.error}`})
  }

  requestEnded(){
    const {error, success} = this.props
    if(error) { 
      this.growl.show([{severity: 'error', summary: 'Changing Password failed', detail: `${error}`}]) 
    } else if (success) {
      this.setState({oldPassword: '', newPassword: '', newPasswordSecond: ''})
      this.growl.show([{severity: 'success', summary: 'Password Changed', detail: `${success}`}]) 
    }
    this.props.deleteStatusMessages()
  }

  render() {
    // console.log(this.state)
    const {textRowOne, textRowTwo, textRowThree, minPwLength} = this.state
    const {loading, error, success} = this.props
    if(error || success) {
      this.requestEnded()
    }
    // if(!loading && this.pendingRequest && !error) {this.passwordDidChange()}
    // if(!loading && this.pendingRequest && error) {this.passwordDidNotChange()}

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

        <Growl ref={(el) => this.growl = el} />
      </div>
        
    )
  }
}

function mapStateToProps(state) {
  const { loading, error, success} = state.settings
  const {username} = state.authentication
  return {
    loading,
    error,
    success,
    username
  }
}

const mapDispatchToProps = {
  exampleIncrement: settingsActions.exampleIncrement,
  exampleDecrement: settingsActions.exampleDecrement,
  changePassword: settingsActions.changePassword,
  deleteStatusMessages: settingsActions.deleteStatusMessages
}

export default connect(mapStateToProps, mapDispatchToProps)(PasswordChange)