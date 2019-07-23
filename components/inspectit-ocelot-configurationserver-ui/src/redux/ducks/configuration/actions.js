import * as types from "./types";
import * as selectors from "./selectors";
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';

/**
 * Fetches all existing configuration files and directories.
 */
export const fetchFiles = () => {
    return dispatch => {
        dispatch(fetchFilesStarted());

        axios
            .get("/directories/")
            .then(res => {
                const files = res.data;
                dispatch(fetchFilesSuccess(files));
            })
            .catch(() => {
                dispatch(fetchFilesFailure());
            });
    };
};


/**
 * Is dispatched when the fetching of the configuration files has been started.
 */
export const fetchFilesStarted = () => ({
    type: types.FETCH_FILES_STARTED
});

/**
 * Is dispatched if the fetching of the configuration files was not successful.
 * 
 */
export const fetchFilesFailure = () => ({
    type: types.FETCH_FILES_FAILURE
});

/**
 * Is dispatched when the fetching of the configuration files was successful.
 * 
 * @param {*} files - the fetched files
 */
export const fetchFilesSuccess = (files) => ({
    type: types.FETCH_FILES_SUCCESS,
    payload: {
        files
    }
});

/**
 * Sets the selection to the given file.
 * 
 * @param {string} selection - absolute path of the selected file (e.g. /configs/prod/interfaces.yml)
 */
export const selectFile = (selection) => ({
    type: types.SELECT_FILE,
    payload: {
        selection
    }
});

/**
 * Resets the configuration state.
 */
export const resetState = () => ({
    type: types.RESET
});

/**
 * Attempts to delete the currently selected file or folder.
 * In case of success, fetchFiles() is automatically triggered.
 */
export const deleteSelection = (fetchFilesOnSuccess) => {
    return (dispatch, getState) => {
        const state = getState();
        const { selection } = state.configuration;
        const isDirectory = selectors.isSelectionDirectory(state);

        let filePath = selection.startsWith("/") ? selection.substring(1) : selection;

        dispatch({type : types.DELETE_SELECTION_STARTED});

        axios
            .delete( (isDirectory ? "/directories/" : "/files/") + filePath)
            .then(res => {
                dispatch({type : types.DELETE_SELECTION_SUCCESS});
                if(fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({type : types.DELETE_SELECTION_FAILURE});
                dispatch(notificationActions.showErrorMessage("Could not delete " + selection, "Server responden with " + error.response.status));
            });
    };
};


/**
 * Attempts to write the given contents to the given file.
 * Triggers fetchFiles() if requested on success.
 */
export const writeFile = (file,content,fetchFilesOnSuccess) => {
    return (dispatch) => {

        let filePath = file.startsWith("/") ? file.substring(1) : file;

        dispatch({type : types.WRITE_FILE_STARTED});

        axios
            .put("/files/" + filePath, {
                content
            })
            .then(res => {
                dispatch({type : types.WRITE_FILE_SUCCESS});
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({type : types.WRITE_FILE_FAILURE});
                dispatch(notificationActions.showErrorMessage("Could not write file", "Server responden with " + error.response.status));
            });
    };
};


/**
 * Attempts to create the given directory.
 * Triggers fetchFiles() if requested on success.
 */
export const createDirectory = (path,fetchFilesOnSuccess) => {
    return (dispatch) => {

        let dirPath = path.startsWith("/") ? path.substring(1) : path;

        dispatch({type : types.CREATE_DIRECTORY_STARTED});

        axios
            .put("/directories/" + dirPath)
            .then(res => {
                dispatch({type : types.CREATE_DIRECTORY_SUCCESS});
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({type : types.CREATE_DIRECTORY_FAILURE});
                dispatch(notificationActions.showErrorMessage("Could not create directory", "Server responden with " + error.response.status));
            });
    };
};