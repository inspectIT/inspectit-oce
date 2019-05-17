import * as types from './types'
import {axiosPlain} from '../../../lib/axios-api'
import axiosBearer from '../../../lib/axios-api'

export const changePassword = (username, oldPassword, newPassword) => {
  return dispatch => {
    dispatch({ type: types.CHANGE_PASSWORD_STARTED })

    axiosPlain
      .put('/account/password', {
          password : newPassword
        },{
        auth: {
          username: username,
          password: oldPassword
        }
      })
      .then(res => { 
        const message = 'Your password has been changed'
        dispatch({ type: types.CHANGE_PASSWORD_SUCCESS, payload: {success: message} }) })
      .catch(e => { 
        const {response} = e
        let message = ''
        if(response.status === 401) {
          message = 'The given password was wrong'
        } else {
          message = e.message
        }
        dispatch({ type: types.CHANGE_PASSWORD_FAILURE, payload: { error: message } }) })
  }
}

export const searchUser = (param) => {
  return dispatch => {
    dispatch({ type: types.SEARCH_USER_STARTED })

    //define query path
    let parsed = parseInt(param)
    let path = ''
    if(param && isNaN(parsed)){ path=`?username=${param}` } 
    else if(param){ path=`/${param}` }

    axiosBearer
      .get(`/users${path}`)
      .then(res => {
        if(Array.isArray(res.data)){
          var data = res.data
        } else {
          var data = [res]
        }
        let users = prepareUsers(data)
        dispatch({ type: types.SEARCH_USER_SUCCESS, payload: { users: users } })
       })
      .catch(e => {
        dispatch({ type: types.SEARCH_USER_FAILURE, payload: { error: e.message } })
      })
  }
}

// change incoming user array into required usage for UserDataTable Component
const prepareUsers = (users) => {
  let res = []
  res.push({
    head: true,
  })
  res.push({
    newUser: true,
  })
  users.forEach(element => {
    res.push(
      {
        id: element.id,
        username: element.username,
        role: 'admin'
      }
    )
  });
  return res
}

export const changeUserFilter = (string = '') => ({
  type: types.CHANGE_USER_FILTER,
  payload: { string }
});

export const deleteUser = (id) =>{
  return dispatch => {
    dispatch({type: types.DELETE_USER_STARTED})

    axiosBearer
      .delete(`/users/${id}`)
      .then(res => { 
        const message = 'User has been deleted'
        dispatch({ type: types.DELETE_USER_SUCCESS, payload: { success: message } })
        dispatch(searchUser())
      })
      .catch(e => { console.log(e); dispatch({ type: types.DELETE_USER_FAILURE, payload: { error: e.message } }) 
      })
  }
}

export const addUser = (userObj) => {
  return dispatch => {
    dispatch({ type: types.ADD_USER_STARTED })

    axiosBearer
      .post('/users', {
        username: userObj.username,
        password: userObj.password
      })
      .then(res => {
        const message = 'User has been added'
        dispatch({ type: types.ADD_USER_SUCCESS, payload: { success: message } })
        dispatch(searchUser())
      })
      .catch(e => { 
        const {response} = e
        let message = ''
        if(response.status === 400) {
          message = response.data.message
        } else {
          message = e.message
        }
        dispatch({ type: types.ADD_USER_FAILURE, payload: { error: message } }) })
  }
}

export const deleteStatusMessages = () => ({
  type: types.DELETE_STATUS_MESSAGE
});