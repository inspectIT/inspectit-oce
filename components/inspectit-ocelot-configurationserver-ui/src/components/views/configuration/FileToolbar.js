import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'

import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used in the configuration view's file tree.
 */
class FileToolbar extends React.Component {

    fetchFiles = () => {
        this.props.fetchFiles();
    }

    render() {
        const { loading } = this.props;
        const spinClass = loading ? " pi-spin" : "";

        const tooltipOptions = {
            showDelay: 500,
            position: "top"
        }
        return (
            <div className="this">
                <style jsx>{`
                .this :global(.p-toolbar) {
                    background: 0;
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
                    <div className="p-toolbar-group-left">
                        <Button disabled={loading} tooltip="New File" icon="pi pi-file" tooltipOptions={tooltipOptions} />
                        <Button disabled={loading} tooltip="New Directory" icon="pi pi-folder-open" tooltipOptions={tooltipOptions} />
                    </div>
                    <div className="p-toolbar-group-right">
                        <Button disabled={loading} onClick={this.fetchFiles} tooltip="Reload" icon={"pi pi-refresh" + spinClass} tooltipOptions={tooltipOptions} />
                    </div>
                </Toolbar>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const { loading } = state.configuration;
    return {
        loading
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles
}

export default connect(mapStateToProps, mapDispatchToProps)(FileToolbar);