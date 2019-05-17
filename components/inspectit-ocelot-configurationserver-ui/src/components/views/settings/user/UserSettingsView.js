import React from 'react'
import SettingsElement from '../SettingsElement'
import SearchField from './ToolbarSearchField';
import UserDataTable from './UserDataTable';
import UserToolbar from './UserSettingsToolbar'
import { connect } from 'react-redux'
import {settingsActions, settingsSelectors} from '../../../../redux/ducks/settings'
import {Growl} from 'primereact/growl'

class UserSettingsView extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  showMessage(){
    const {error, success} = this.props
    if(error) { 
      this.growl.show([{severity: 'error', summary: 'Request Failed', detail: `${error}`}]) 
    } else if (success) {
      this.growl.show([{severity: 'success', summary: 'Request Success', detail: `${success}`}]) 
    }
    this.props.deleteStatusMessages()
  }
  
  render() {
    const {error, success} = this.props
    if(error || success) {
      this.showMessage()
    }

    return (
      <div className='this'>
        <style jsx>{`
        `}</style>
         <UserToolbar />
        <UserDataTable/>
        <Growl ref={(el) => this.growl = el} />
      </div>
    )
  }

}

function mapStateToProps(state) {
  const {error, success} = state.settings
  return {
    error,
    success,
  }
}

const mapDispatchToProps = {
  deleteStatusMessages: settingsActions.deleteStatusMessages
}

export default connect(mapStateToProps, mapDispatchToProps)(UserSettingsView)