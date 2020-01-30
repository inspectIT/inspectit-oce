import React from 'react';
import { Tree } from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors, configurationUtils } from '../../../redux/ducks/configuration';
import { linkPrefix } from '../../../lib/configuration';

import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {

    state = {
        menu: []
    };

    /**
     * Fetch the files initially.
     */
    componentDidMount = () => {
        const { loading, defaultConfig } = this.props;
        if (!loading) {
            this.props.fetchFiles();
        }

        if (Object.entries(defaultConfig).length === 0) {
            this.props.fetchDefaultConfig();
        }

    }

    /**
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        const { selection, selectedDefaultConfigFile, rawFiles } = this.props;
        const newSelection = event.value;
        if (newSelection) {
            if (newSelection !== selection && newSelection !== selectedDefaultConfigFile) {
                this.props.selectFile(newSelection);
            }
        } else {
            if (selection || selectedDefaultConfigFile) {
                this.props.selectFile(null);
            }
        }
    }

    /**
     * Handle Contextmenu selection.
     * Switch between a contextmenu for filenodes and a general menu.
     */
    onContextMenuSelectionChange = (event) => {
        const newSelection = event.value;

        if (newSelection && newSelection.startsWith(DEFAULT_CONFIG_TREE_KEY)) {
            // Show no contextmenu when clicked on a ocelot default configuration node.
            event.originalEvent.stopPropagation();
            return;
        }
        this.onSelectionChange(event);

        this.setState({ menu: this.getMenu(!!newSelection) });
        this.cm.show(event.originalEvent || event);
    }

    render() {
        return (
            <div className='this' onContextMenu={this.onContextMenuSelectionChange}>
                <style jsx>{`
                    .this {
                        overflow: auto;
                        flex-grow: 1;
                    }
                    .this :global(.cm-tree-icon) {
                        width: 1.3rem;
                        height: 1.3rem;
                    }
                    .this :global(.cm-tree-label) {
                        color: #aaa;
                    }
                    .this :global(.ocelot-tree-head-orange) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_orange.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
                    .this :global(.ocelot-tree-head-white) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_white.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
				`}</style>
                <ContextMenu model={this.state.menu} ref={el => this.cm = el} />
                <Tree
                    className={this.props.className}
                    filter={true}
                    filterBy="label"
                    value={this.props.defaultTree.concat(this.props.files)}
                    selectionMode="single"
                    selectionKeys={this.props.selection || this.props.selectedDefaultConfigFile}
                    onSelectionChange={this.onSelectionChange}
                    onContextMenuSelectionChange={this.onContextMenuSelectionChange} />
                />
            </div>

        );
    }

    getMenu = (isForFile) => {
        const { showCreateDirectoryDialog, showCreateFileDialog, showMoveDialog, showDeleteFileDialog } = this.props;
        return [
            {
                label: 'Add Folder',
                icon: 'pi pi-folder',
                command: showCreateDirectoryDialog
            },
            {
                label: 'Add File',
                icon: 'pi pi-file',
                command: showCreateFileDialog
            },
            {
                label: 'Rename',
                icon: 'pi pi-pencil',
                disabled: !isForFile,
                command: showMoveDialog
            },
            {
                label: 'Delete',
                icon: 'pi pi-trash',
                disabled: !isForFile,
                command: showDeleteFileDialog
            }
        ];
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection, files, defaultConfig, selectedDefaultConfigFile } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading: pendingRequests > 0,
        selection,
        defaultConfig: defaultConfig,
        defaultTree: configurationSelectors.getDefaultConfigTree(state),
        selectedDefaultConfigFile
    }
}

const mapDispatchToProps = {
    fetchDefaultConfig: configurationActions.fetchDefaultConfig,
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile,
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);