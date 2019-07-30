import * as types from "./types";
import * as selectors from "./selectors";
import axios from '../../../lib/axios-api';
import { configurationUtils } from '../configuration';
import { notificationActions } from '../notification';

/**
 * Fetches all existing configuration files and directories.
 */
export const fetchFiles = () => {
    return dispatch => {
        dispatch({ type: types.FETCH_FILES_STARTED });

        axios
            .get("/directories/")
            .then(res => {
                const files = res.data;
                dispatch({ type: types.FETCH_FILES_SUCCESS, payload: { files } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_FILES_FAILURE });
            });
    };
};

export const fetchFile = (file) => {
    return dispatch => {
        dispatch({ type: types.FETCH_FILE_STARTED });

        axios
            .get("/files" + file)
            .then(res => {
                const fileContent = res.data.content;
                dispatch({ type: types.FETCH_FILE_SUCCESS, payload: { fileContent } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_FILE_FAILURE });
            });
    };
};

/**
 * Sets the selection to the given file.
 * 
 * @param {string} selection - absolute path of the selected file (e.g. /configs/prod/interfaces.yml)
 */
export const selectFile = (selection) => {
    return (dispatch, getState) => {
        dispatch({
            type: types.SELECT_FILE,
            payload: {
                selection
            }
        });

        const file = configurationUtils.getFile(getState().configuration.files, selection);
        const isDirectory = configurationUtils.isDirectory(file);
        if (!isDirectory) {
            dispatch(fetchFile(selection));
        }
    };
};

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

        dispatch({ type: types.DELETE_SELECTION_STARTED });

        axios
            .delete((isDirectory ? "/directories/" : "/files/") + filePath)
            .then(res => {
                dispatch({ type: types.DELETE_SELECTION_SUCCESS });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({ type: types.DELETE_SELECTION_FAILURE });
            });
    };
};


/**
 * Attempts to write the given contents to the given file.
 * Triggers fetchFiles() if requested on success.
 */
export const writeFile = (file, content, fetchFilesOnSuccess, fetchFileContent = false) => {
    return (dispatch) => {

        let filePath = file.startsWith("/") ? file.substring(1) : file;

        dispatch({ type: types.WRITE_FILE_STARTED });

        axios
            .put("/files/" + filePath, {
                content
            })
            .then(res => {
                dispatch({ type: types.WRITE_FILE_SUCCESS });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
                if (fetchFileContent) {
                    dispatch(fetchFile(file));
                }
            })
            .catch((error) => {
                dispatch({ type: types.WRITE_FILE_FAILURE });
            });
    };
};


/**
 * Attempts to create the given directory.
 * Triggers fetchFiles() if requested on success.
 */
export const createDirectory = (path, fetchFilesOnSuccess) => {
    return (dispatch) => {

        let dirPath = path.startsWith("/") ? path.substring(1) : path;

        dispatch({ type: types.CREATE_DIRECTORY_STARTED });

        axios
            .put("/directories/" + dirPath)
            .then(res => {
                dispatch({ type: types.CREATE_DIRECTORY_SUCCESS });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({ type: types.CREATE_DIRECTORY_FAILURE });
            });
    };
};