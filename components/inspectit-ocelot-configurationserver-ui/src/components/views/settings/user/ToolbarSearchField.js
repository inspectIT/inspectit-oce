import React from 'react'
import {InputText} from 'primereact/inputtext'
import { connect } from 'react-redux'
import {settingsActions, settingsSelectors} from '../../../../redux/ducks/settings'

class SearchField extends React.Component {
  constructor(props){
    super(props)
    this.state = { value: ''}
  }

  render() {
    return(
      <div className='this'>
        <style jsx>{`
          .this{
            display: flex;
            align-items: center;
          }
          .searchbox{
            margin: 0.3rem;
          }
          h4{
            margin: 0;
            margin-right: 1rem;
            font-size: 1rem;
            font-weight: normal;
          }
        `}</style>
        <h4>Users</h4>
        <div className='searchbox'>
          <span className="p-float-label">
            <InputText id="in" value={this.state.value} onChange={(e) => { this.props.changeUserFilter(e.target.value); this.setState({value: e.target.value}) } } />
            <label htmlFor="in">Search</label>
          </span>
        </div>
      </div>
    )
  }
}

const mapDispatchToProps = {
  getUsers: settingsActions.searchUser,
  changeUserFilter: settingsActions.changeUserFilter
}

export default connect(null, mapDispatchToProps)(SearchField)