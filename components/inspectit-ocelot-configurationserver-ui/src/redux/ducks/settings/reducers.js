import * as types from "./types";
import { createReducer } from "../../utils";
import {settings as initialState} from '../initial-states'

const settingsReducer = createReducer(initialState)({
  [types.DELETE_STATUS_MESSAGE] : (state, action) => {
    return {
      ...state,
      success: null,
      error: null,
    }
  },
  [types.CHANGE_PASSWORD_STARTED]: (state, action) => {
    return {
      ...state,
      loading: true
    }
  },
  [types.CHANGE_PASSWORD_FAILURE]: (state, action) => {
    const {error} = action.payload
    return {
      ...state,
      loading: false,
      error: error,
      success: null
    }
  },
  [types.CHANGE_PASSWORD_SUCCESS]: (state, action) => {
    const {success} = action.payload
    return {
      ...state,
      loading: false,
      error: null,
      success: success
    }
  },
  [types.SEARCH_USER_STARTED]: (state, action) => {
    return {
      ...state
    }
  },
  [types.SEARCH_USER_FAILURE]: (state, action) => {
    const { error } = action.payload
    return {
      ...state,
      users: [],
      error: error,
      success: null
    }
  },
  [types.SEARCH_USER_SUCCESS]: (state, action) => {
    const {users} = action.payload 
    return {
      ...state,
      users: users,
      error: null
    }
  },
  [types.CHANGE_USER_FILTER]: (state, action) => {
    const {string} = action.payload
    return {
      ...state,
      userFilter: string
    }
  },
  [types.DELETE_USER_STARTED]: (state, action) => {
    return {
      ...state
    }
  },
  [types.DELETE_USER_FAILURE]: (state, action) => {
    const { error } = action.payload
    return {
      ...state,
      error: error,
      success: null
    }
  },
  [types.DELETE_USER_SUCCESS]: (state, action) => {
    const {success} = action.payload
    return {
      ...state,
      error: null,
      success: success
    }
  },
  [types.ADD_USER_STARTED]: (state, action) => {
    return {
      ...state
    }
  },
  [types.ADD_USER_FAILURE]: (state, action) => {
    const { error } = action.payload
    return {
      ...state,
      error: error,
      success: null
    }
  },
  [types.ADD_USER_SUCCESS]: (state, action) => {
    const {success} = action.payload
    return {
      ...state,
      error: null,
      success: success
    }
  },
})

export default settingsReducer