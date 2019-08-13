import * as types from "./types";
import { axiosPlain } from "../../../lib/axios-api";
import axiosBearer from "../../../lib/axios-api";
import { configurationActions } from "../configuration";
import { RENEW_TOKEN_TIME_BUFFER } from '../../../data/constants'
import { RENEW_TOKEN_RETRY_INTERVAL } from '../../../data/constants'
import { getTokenExpirationDate } from "./selectors";
/**
 * Fetches an access token for the given credentials.
 *
 * @param {string} username
 * @param {string} password
 */
export const fetchToken = (username, password) => {
  return dispatch => {
    dispatch({ type: types.FETCH_TOKEN_STARTED });

    axiosPlain
      .get("/account/token", {
        auth: {
          username: username,
          password: password
        }
      })
      .then(res => {
        const token = res.data;
        dispatch({ type: types.FETCH_TOKEN_SUCCESS, payload: {token, username} });
        dispatch(delayedRenewTokenDispatch());
      })
      .catch(err => {
        let message;
        const { response } = err;
        if (response && response.status == 401) {
          message = "The given credentials are not valid.";
        } else {
          message = err.message;
        }
        dispatch({ type: types.FETCH_TOKEN_FAILURE, payload: {error: message}});
      });
  };
};

/**
 * Renews the access token with the existing token.
 *
 */
export const renewToken = () => {
  return dispatch => {
    dispatch({ type: types.RENEW_TOKEN_STARTED });

    axiosBearer
      .get("/account/token")
      .then(res => {
        const token = res.data;
        dispatch({ type: types.RENEW_TOKEN_SUCCESS , payload: {token}});
        dispatch(delayedRenewTokenDispatch());
      })
      .catch(err => {
        dispatch({ type: types.RENEW_TOKEN_FAILURE, payload: {error: err.message}});
        setTimeout(() => {
          dispatch(renewToken());
        }, RENEW_TOKEN_RETRY_INTERVAL);
        
      });
  };
};

/**
 * Creates the renewToken action with a delay.
 * The interval is computed based on the expiration date including a defined buffer.
 */
export const delayedRenewTokenDispatch = () => {
  return (dispatch, getState) => {
    dispatch({ type: types.DELAYED_RENEW_TOKEN_ACTION });
    const state = getState();
    if (state.authentication.token != null) {
      const expirationDate = getTokenExpirationDate(state) * 1000;
      const timeBuffer = RENEW_TOKEN_TIME_BUFFER;
      const interval = expirationDate - Date.now() - timeBuffer;
      setTimeout(() => {
        dispatch(renewToken());
      }, interval);
    }
  };
};

/**
 * Logout of the current user.
 */
export const logout = () => {
  return dispatch => {
    dispatch({ type: types.LOGOUT });
    dispatch(configurationActions.resetState());
  };
};
