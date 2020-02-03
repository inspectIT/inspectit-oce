import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors, configurationUtils } from '../../../../redux/ducks/configuration'

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';


/**
 * Dialog for deleting the currently selected file or folder.
 */
class DeleteDialog extends React.Component {

    state = {};

    deleteButton = React.createRef();

    render() {
        const { selectionName, type } = this.state;

        return (
            <Dialog
                header={"Delete " + type}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={(
                    <div>
                        <Button label="Delete" ref={this.deleteButton} className="p-button-danger" onClick={this.deleteSelectedFile} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                Are you sure you want to delete <b>"{selectionName}"</b> ? This cannot be undone!
            </Dialog>
        )
    }

    deleteSelectedFile = () => {
        this.props.deleteSelection(true);
        this.props.onHide();
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.deleteButton.current.element.focus();

            /** Pick selection between redux state selection and incoming property selection. */
            const { selection, stateSelection } = this.props;

            const selectedFile = stateSelection || selection;
            const selectionName = selectedFile ? selectedFile.split("/").slice(-1)[0] : "";
            const fileObj = configurationUtils.getFile(this.props.files, selectedFile);
            const type = configurationUtils.isDirectory(fileObj) ? "Directory" : "File";

            this.setState({ selectionName, type });
        }
    }

}

function mapStateToProps(state) {
    const { selection, files } = state.configuration;
    return {
        selection,
        files
    }
}

const mapDispatchToProps = {
    deleteSelection: configurationActions.deleteSelection
}

export default connect(mapStateToProps, mapDispatchToProps)(DeleteDialog);