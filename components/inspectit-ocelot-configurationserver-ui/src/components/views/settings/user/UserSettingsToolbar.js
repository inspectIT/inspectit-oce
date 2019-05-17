import react from 'react'
import { connect } from 'react-redux'
import {settingsActions, settingsSelectors} from '../../../../redux/ducks/settings'
import  {Toolbar} from 'primereact/toolbar'
import SearchField from './ToolbarSearchField'
import {Button} from 'primereact/button'

class UserToolbar extends React.Component {
  constructor(props){
    super(props)
  }

  render(){
    return(
      <div className='this'>
        <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
      }

      .this :global(.p-toolbar-group-left) :global(.p-button) {
          margin-right: 0.25rem;
      }
        `}</style>
        <Toolbar>
          <div className='p-toolbar-group-left'>
            <SearchField />
          </div>
          <div className='p-toolbar-group-right'>
            <Button icon='pi pi-refresh' onClick={(e) => {this.props.getUsers()}}></Button>
          </div>
        </Toolbar>
      </div>
    )
  }
}

const mapDispatchToProps = {
  getUsers: settingsActions.searchUser
}

export default connect(null, mapDispatchToProps)(UserToolbar)