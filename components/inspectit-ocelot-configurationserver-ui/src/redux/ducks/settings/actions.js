import * as types from './types'
import {axiosPlain} from '../../../lib/axios-api'
import axiosBearer from '../../../lib/axios-api'
import { notificationActions } from '../notification'

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
        dispatch({ type: types.CHANGE_PASSWORD_SUCCESS }) 
        dispatch(notificationActions.showSuccessMessage('Request failed', 'Your password has been changed'))
      })
      .catch(e => { 
        const {response} = e
        if(response.status === 401) {
          dispatch(notificationActions.showErrorMessage('Request failed', 'the given password was wrong'))
        }
        dispatch({ type: types.CHANGE_PASSWORD_FAILURE }) 
      })
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
        dispatch({ type: types.SEARCH_USER_FAILURE })
      })
  }
}

// change incoming user array into required usage for UserDataTable Component
const prepareUsers = (users) => {
  let res = []
  res.push({ head: true })
  res.push({ newUser: true })
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
        dispatch({ type: types.DELETE_USER_SUCCESS })
        dispatch(searchUser())
        dispatch(notificationActions.showSuccessMessage('Request success', `User with ID: ${id} has been deleted`))
      })
      .catch(e => { dispatch({ type: types.DELETE_USER_FAILURE}) 
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
        const message = `User ${userObj.username} has been added`
        dispatch({ type: types.ADD_USER_SUCCESS })
        dispatch(searchUser())
        dispatch(notificationActions.showSuccessMessage('Request success', message))
      })
      .catch(e => { 
        const {response} = e
        if(response.status === 400) {
          dispatch(notificationActions.showWarningMessage('Request failed', response.data.message))
        }
        dispatch({ type: types.ADD_USER_FAILURE })
      })
  }
}