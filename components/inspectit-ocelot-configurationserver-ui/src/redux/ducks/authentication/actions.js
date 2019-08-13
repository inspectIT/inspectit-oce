import * as types from "./types";
import {axiosPlain} from '../../../lib/axios-api';
import axiosBearer from '../../../lib/axios-api';
import {configurationActions} from '../configuration';
import {START_TIMER} from 'redux-timer';
import {STOP_TIMER} from 'redux-timer';
import {getTokenExpirationDate} from "./selectors";
/**
 * Fetches an access token for the given credentials.
 * 
 * @param {string} username 
 * @param {string} password 
 */
export const fetchToken = (username, password) => {
    return dispatch => {
        dispatch(fetchTokenStarted());

        axiosPlain
            .get("/account/token", {
                auth: {
                    username: username,
                    password: password
                }
            })
            .then(res => {
                const token = res.data;
                dispatch(fetchTokenSuccess(token, username));
                dispatch(configureTokenRenewTimer());
            })
            .catch(err => {
                let message;
                const { response } = err;
                if (response && response.status == 401) {
                    message = "The given credentials are not valid.";
                } else {
                    message = err.message;
                }
                dispatch(fetchTokenFailure(message));
            });
    };
};

/**
 * Is dispatched when the fetching of the access token has been started.
 */
export const fetchTokenStarted = () => ({
    type: types.FETCH_TOKEN_STARTED
});

/**
 * Is dispatched if the fetching of the access token was not successful.
 * 
 * @param {*} error 
 */
export const fetchTokenFailure = (error) => ({
    type: types.FETCH_TOKEN_FAILURE,
    payload: {
        error
    }
});

/**
 * Is dispatched when the fetching of the access token was successful.
 * 
 * @param {string} token 
 */
export const fetchTokenSuccess = (token, username) => ({
    type: types.FETCH_TOKEN_SUCCESS,
    payload: {
        token,
        username
    }
});

/**
 * Renews the access token with the existing token.
 * 
 */
export const renewToken = () => {
    return dispatch => {
        dispatch(renewTokenStarted());

        axiosBearer
            .get("/account/token")
            .then(res => {
                const token = res.data;
                dispatch(renewTokenSuccess(token));
            })
            .catch(err => {
                let message;
                const { response } = err;
                if (response && response.status == 401) {
                    message = "The given token was not valid.";
                } else {
                    message = err.message;
                }
                dispatch(renewTokenFailure(message));
            });
    };
};

/**
 * Is dispatched when the renewing of the access token has been started.
 */
export const renewTokenStarted = () => ({
    type: types.RENEW_TOKEN_STARTED
});

/**
 * Is dispatched if the renewing of the access token was not successful.
 * 
 * @param {*} error 
 */
export const renewTokenFailure = (error) => ({
    type: types.RENEW_TOKEN_FAILURE,
    payload: {
        error
    }
});

/**
 * Is dispatched when the renewing of the access token was successful.
 * 
 * @param {string} token 
 */
export const renewTokenSuccess = (token) => ({
    type: types.RENEW_TOKEN_SUCCESS,
    payload: {
        token
    }
});

/**
 * Starts a timer, which continuously renews the authentication token. 
 * @param {*} interval the interval of the token renew timer
 */
export const startTokenRenewTimer = (interval) => async dispatch => { 
    return dispatch({
      type: START_TIMER,
      payload: {
        name: 'tokenRenewTimer',
        action: async () => {
         dispatch(renewToken());
        },
        interval: interval,
        runImmediately: false
      }
    });
  };


export const stopTokenRenewTimer = () => ({
  type: STOP_TIMER,
  payload: {
    name: 'tokenRenewTimer'
  }
});

/**
 * Configures and starts token renew timer. The interval is computed based on the expiration date including a buffer of 5 minutes.
 */
export const configureTokenRenewTimer = () => {
    return (dispatch, getState) => {
        const state = getState();
        if (state.authentication.token != null) {
            const expirationDate = getTokenExpirationDate(state)*1000;
            const timeBuffer = 300000;
            const interval = expirationDate - Date.now()- timeBuffer;
            dispatch(startTokenRenewTimer(interval));
        };
    };
};

/**
 * Logout of the current user.
 */
export const logout = () => {
    return dispatch => {
        dispatch(stopTokenRenewTimer());
        dispatch({type: types.LOGOUT});
        dispatch(configurationActions.resetState());
    };
};
